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
import com.nexatv.launcher.data.TileSize
import com.nexatv.launcher.data.resolveFavorites
import com.nexatv.launcher.data.toFavoriteKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
     * size (ADR-0009); the UI maps it to concrete dimensions.
     */
    data class Apps(
        val favorites: List<AppEntry>,
        val apps: List<AppEntry>,
        val tileSize: TileSize,
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
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** Last discovered apps, cached so a favorites change re-emits without re-querying/re-rasterizing. */
    private var cachedApps: List<AppEntry> = emptyList()

    /** Latest persisted favorites, kept so [emitApps] can resolve them against [cachedApps]. */
    private var favoriteKeys: List<FavoriteKey> = emptyList()

    /** Latest persisted tile size (ADR-0009); defaults to MEDIUM until the store's first emission. */
    private var tileSize: TileSize = TileSize.MEDIUM

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
    }

    /** Fire-and-forget re-evaluation for callers outside a coroutine (role return, ON_RESUME). */
    fun refresh() {
        viewModelScope.launch { reload() }
    }

    /**
     * Re-evaluate from scratch: until NexaTV is effectively Home — the default Home *or* the enabled
     * HOME-override (ADR-0007) — we surface the prompt instead of a (potentially partial) grid.
     */
    private suspend fun reload() {
        if (!homeRole.isEffectivelyHome()) {
            _state.value = HomeUiState.NeedsHomeRole
            return
        }
        cachedApps = repository.loadApps()
        emitApps()
    }

    /** Project the cached apps + current favorites into the UI state (favorites resolved in order). */
    private fun emitApps() {
        _state.value = if (cachedApps.isEmpty()) {
            HomeUiState.Empty
        } else {
            HomeUiState.Apps(
                favorites = resolveFavorites(cachedApps, favoriteKeys),
                apps = cachedApps,
                tileSize = tileSize,
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
}
