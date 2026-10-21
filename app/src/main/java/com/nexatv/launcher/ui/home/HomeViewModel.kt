/*
 * Copyright 2026 Viacheslav Melnichenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.nexatv.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexatv.launcher.data.AppEntry
import com.nexatv.launcher.data.FavoriteKey
import com.nexatv.launcher.data.FavoritesRepository
import com.nexatv.launcher.data.HomeRoleRepository
import com.nexatv.launcher.data.InstalledAppsRepository
import com.nexatv.launcher.data.SettingsRepository
import com.nexatv.launcher.data.ThemeChoice
import com.nexatv.launcher.data.TileSize
import com.nexatv.launcher.data.resolveFavorites
import com.nexatv.launcher.data.toFavoriteKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** UI state for the Home grid — a first frame appears before discovery finishes (FR-009/FR-014). */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    /**
     * Not Home yet (neither the default Home nor the HOME-override): show the pre-Home prompt instead
     * of a (potentially partial) grid (FR-015 / ADR-0006 / ADR-0007).
     */
    data object NeedsHomeRole : HomeUiState
    data object Empty : HomeUiState

    /**
     * NexaTV is Home and apps were discovered. [apps] is the full grid ("Усі застосунки"); [favorites]
     * is the user's pinned apps in pin/reorder order (ADR-0008), rendered as the "Обране" row above the
     * grid. [favorites] is empty until the user pins something. [tileSize] is the user's chosen tile
     * size (ADR-0009); the UI maps it to concrete dimensions. [theme] is the user's chosen color theme
     * (Theming EPIC); the Settings row reflects it (the palette itself is applied at the activity root).
     */
    data class Apps(
        val favorites: List<AppEntry>,
        val apps: List<AppEntry>,
        val tileSize: TileSize,
        val theme: ThemeChoice,
    ) : HomeUiState
}

/**
 * Owns Home-screen state: whether NexaTV is the default Home (ADR-0006) and, once it is, the
 * discovered app list (ADR-0004). Discovery runs off the UI thread in the repository (NFR-003); this
 * drives the [HomeUiState] and forwards launch requests. Repositories are injected so the state
 * machine is unit-testable without a device.
 */
class HomeViewModel(
    private val repository: InstalledAppsRepository,
    private val homeRole: HomeRoleRepository,
    private val favoritesRepository: FavoritesRepository,
    private val settingsRepository: SettingsRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** Last discovered apps, cached so a favorites change re-emits without re-querying/re-rasterizing. */
    private var cachedApps: List<AppEntry> = emptyList()

    /** True once the catalogue has been loaded at least once, so [refresh] never re-queries needlessly. */
    private var hasLoaded: Boolean = false

    /** The in-flight catalogue load, cancelled before a new one so overlapping triggers coalesce. */
    private var reloadJob: Job? = null

    /** Latest persisted favorites, kept so [emitApps] can resolve them against [cachedApps]. */
    private var favoriteKeys: List<FavoriteKey> = emptyList()

    /** Latest persisted tile size (ADR-0009); defaults to MEDIUM until the store's first emission. */
    private var tileSize: TileSize = TileSize.MEDIUM

    /** Latest persisted color theme (Theming EPIC); defaults until the store's first emission. */
    private var theme: ThemeChoice = ThemeChoice.Default

    init {
        refresh()
        // Keep the grid live: re-evaluate whenever an app is installed/removed/changed (FR-002).
        // collectLatest cancels an in-flight reload if another change arrives, so a burst of callbacks
        // (one app update fires several) coalesces into a single, up-to-date load.
        viewModelScope.launch {
            repository.appChanges().collectLatest { reload() }
        }
        // Keep favorites live: a pin/unpin/reorder updates the "Обране" row without reloading the grid.
        viewModelScope.launch {
            favoritesRepository.favorites.collect { keys ->
                favoriteKeys = keys
                if (_state.value is HomeUiState.Apps) emitApps()
            }
        }
        // Keep tile size live: changing it in Settings re-renders the grid/favorites without a reload.
        viewModelScope.launch {
            settingsRepository.tileSize.collect { size ->
                tileSize = size
                if (_state.value is HomeUiState.Apps) emitApps()
            }
        }
        // Keep the theme live: changing it in Settings re-emits so the UI reflects the selection.
        viewModelScope.launch {
            settingsRepository.theme.collect { choice ->
                theme = choice
                if (_state.value is HomeUiState.Apps) emitApps()
            }
        }
    }

    /**
     * Re-evaluate the HOME role on a role return or every ON_RESUME. This must stay cheap: it does NOT
     * re-query/re-rasterize the catalogue once it is loaded — the live [InstalledAppsRepository.appChanges]
     * stream already owns catalogue reloads on install/removal (FR-002). Only the first time we become
     * Home (nothing loaded yet) does this trigger a full load; afterwards it just re-checks the role and
     * re-projects the cached apps (or surfaces the prompt if NexaTV stopped being Home).
     */
    fun refresh() {
        if (!hasLoaded) {
            reload()
            return
        }
        viewModelScope.launch {
            if (isEffectivelyHome()) emitApps() else _state.value = HomeUiState.NeedsHomeRole
        }
    }

    /**
     * Load the catalogue from scratch: the first become-Home, or an install/removal via [appChanges].
     * Until NexaTV is effectively Home — the default Home *or* the enabled HOME-override (ADR-0007) — we
     * surface the prompt instead of a (potentially partial) grid. Cancels any in-flight load first so a
     * burst of triggers (init racing the first resume, or several change callbacks) rasterizes icons once.
     */
    private fun reload() {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            if (!isEffectivelyHome()) {
                _state.value = HomeUiState.NeedsHomeRole
                return@launch
            }
            cachedApps = repository.loadApps()
            hasLoaded = true
            emitApps()
        }
    }

    /** Resolve the HOME role (two binder round-trips) off the UI thread so the first frame isn't blocked. */
    private suspend fun isEffectivelyHome(): Boolean =
        withContext(defaultDispatcher) { homeRole.isEffectivelyHome() }

    /** Project the cached apps + current favorites into the UI state (favorites resolved in order). */
    private fun emitApps() {
        _state.value = if (cachedApps.isEmpty()) {
            HomeUiState.Empty
        } else {
            HomeUiState.Apps(
                favorites = resolveFavorites(cachedApps, favoriteKeys),
                apps = cachedApps,
                tileSize = tileSize,
                theme = theme,
            )
        }
    }

    fun launch(entry: AppEntry): Boolean = repository.launch(entry)

    /** Open the system "App info" screen for [entry] (context-menu "Інформація"). */
    fun openAppInfo(entry: AppEntry): Boolean = repository.openAppInfo(entry)

    /** Pin [entry] if it is not a favorite, otherwise unpin it; persisted via DataStore (ADR-0008). */
    fun toggleFavorite(entry: AppEntry) {
        viewModelScope.launch { favoritesRepository.toggle(entry.toFavoriteKey()) }
    }

    /** Move the favorite at [from] to [to] within the "Обране" row; persisted via DataStore. */
    fun moveFavorite(from: Int, to: Int) {
        viewModelScope.launch { favoritesRepository.reorder(from, to) }
    }

    /** Persist the home tile size (ADR-0009); the grid re-renders live via the settings Flow. */
    fun setTileSize(size: TileSize) {
        viewModelScope.launch { settingsRepository.setTileSize(size) }
    }

    /** Persist the color theme (Theming EPIC); the palette applies live via the theme Flow. */
    fun setTheme(choice: ThemeChoice) {
        viewModelScope.launch { settingsRepository.setTheme(choice) }
    }
}
