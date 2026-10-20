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
import com.nexatv.launcher.data.HomeRoleRepository
import com.nexatv.launcher.data.InstalledAppsRepository
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
    data class Apps(val apps: List<AppEntry>) : HomeUiState
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
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
        // Keep the grid live: re-evaluate whenever an app is installed/removed/changed (FR-002).
        // collectLatest cancels an in-flight reload if another change arrives, so a burst of callbacks
        // (one app update fires several) coalesces into a single, up-to-date load.
        viewModelScope.launch {
            repository.appChanges().collectLatest { reload() }
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
        val apps = repository.loadApps()
        _state.value = if (apps.isEmpty()) HomeUiState.Empty else HomeUiState.Apps(apps)
    }

    fun launch(entry: AppEntry): Boolean = repository.launch(entry)
}
