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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexatv.launcher.data.AppEntry
import com.nexatv.launcher.data.InstalledAppsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the Home grid — a first frame appears before discovery finishes (FR-009/FR-014). */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data class Apps(val apps: List<AppEntry>) : HomeUiState
}

/**
 * Owns app discovery for the Home screen. Discovery runs off the UI thread in the repository
 * (NFR-003); this just drives the [HomeUiState] and forwards launch requests.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InstalledAppsRepository(application)

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val apps = repository.loadApps()
            _state.value = if (apps.isEmpty()) HomeUiState.Empty else HomeUiState.Apps(apps)
        }
    }

    fun launch(entry: AppEntry): Boolean = repository.launch(entry)
}
