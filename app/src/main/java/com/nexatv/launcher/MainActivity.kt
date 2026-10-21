/*
 * Copyright 2026 Viacheslav Melnichenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.nexatv.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.LifecycleEventObserver
import com.nexatv.launcher.data.HomeRoleRepository
import com.nexatv.launcher.data.InstalledAppsRepository
import com.nexatv.launcher.data.favoritesRepository
import com.nexatv.launcher.data.settingsRepository
import com.nexatv.launcher.data.ThemeChoice
import com.nexatv.launcher.data.TileSize
import com.nexatv.launcher.ui.allapps.AllAppsScreen
import com.nexatv.launcher.ui.home.HomeActions
import com.nexatv.launcher.ui.home.HomeScreen
import com.nexatv.launcher.ui.home.HomeUiState
import com.nexatv.launcher.ui.home.HomeViewModel
import com.nexatv.launcher.ui.settings.SettingsScreen
import com.nexatv.launcher.ui.theme.NexaTheme
import com.nexatv.launcher.ui.theme.colorScheme
import com.nexatv.launcher.ui.theme.dimensions

/**
 * Home entry point. Discovers installed apps via [HomeViewModel] and renders a D-pad-navigable
 * grid that launches on OK (M2). Until NexaTV holds `ROLE_HOME` it shows the pre-Home prompt and
 * drives the system role request (M3 / ADR-0006); state is re-evaluated on return from that flow
 * and on every `ON_RESUME`, so becoming — or ceasing to be — Home is reflected without a restart.
 */
/** The three state-based routes the launcher switches between (ADR-0009). */
private enum class HomeDestination { HOME, ALL_APPS, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current.applicationContext
            val settings = remember { settingsRepository(context) }
            // Resolve the theme one level above LauncherApp. The first frame draws the synchronous
            // in-memory default (Pastel Dark) — the theme read stays off the cold-start critical path
            // (performance rules 2-4). A stored default produces no swap; a stored non-default recolors
            // in a single frame (colors only, no relayout) when DataStore emits.
            val theme by settings.theme.collectAsStateWithLifecycle(initialValue = ThemeChoice.Default)
            NexaTheme(colors = theme.colorScheme()) {
                LauncherApp()
            }
        }
    }
}

/**
 * The launcher's root composable: wires the [HomeViewModel], keeps the HOME-role state fresh (ADR-0006),
 * and switches between the three state-based routes (Home / All apps / Settings; ADR-0009).
 */
@Composable
private fun LauncherApp() {
    val context = LocalContext.current.applicationContext
    val roleRepository = remember { HomeRoleRepository(context) }
    val viewModel: HomeViewModel = viewModel {
        HomeViewModel(
            InstalledAppsRepository(context),
            roleRepository,
            favoritesRepository(context),
            settingsRepository(context),
        )
    }

    // Re-check the HOME role when the system role-request flow returns (ADR-0006).
    val roleRequest = rememberLauncherForActivityResult(StartActivityForResult()) {
        viewModel.refresh()
    }

    // Also re-check on every resume: the default Home can change from outside the app
    // (Settings, another launcher), and we must never show a stale prompt or grid.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    // Signal "fully drawn" once real content replaces the initial Loading state, so the Baseline
    // Profile/Macrobenchmark timeToFullDisplay reflects the grid being usable, not just the first
    // empty frame (NFR-001). No-op in normal use; read by the benchmark harness (:benchmark).
    ReportDrawnWhen { state !is HomeUiState.Loading }

    val actions = remember(viewModel) {
        HomeActions(
            onLaunch = viewModel::launch,
            onToggleFavorite = viewModel::toggleFavorite,
            onMoveFavorite = viewModel::moveFavorite,
            onAppInfo = { viewModel.openAppInfo(it) },
        )
    }

    // State-based navigation (ADR-0009): three routes, still too few to justify a nav library.
    // BACK on any sub-page returns to Home.
    var destination by remember { mutableStateOf(HomeDestination.HOME) }
    val appsState = state as? HomeUiState.Apps
    when (destination) {
        HomeDestination.HOME -> HomeScreen(
            state = state,
            actions = actions,
            onRequestHome = { roleRequest.launch(roleRepository.requestHomeIntent()) },
            onOpenAllApps = { destination = HomeDestination.ALL_APPS },
            onOpenSettings = { destination = HomeDestination.SETTINGS },
        )
        HomeDestination.ALL_APPS -> {
            BackHandler { destination = HomeDestination.HOME }
            AllAppsScreen(
                apps = appsState?.apps.orEmpty(),
                favorites = appsState?.favorites.orEmpty(),
                tile = (appsState?.tileSize ?: TileSize.MEDIUM).dimensions(),
                onLaunch = viewModel::launch,
                onToggleFavorite = viewModel::toggleFavorite,
                onAppInfo = { viewModel.openAppInfo(it) },
            )
        }
        HomeDestination.SETTINGS -> {
            BackHandler { destination = HomeDestination.HOME }
            SettingsScreen(
                tileSize = appsState?.tileSize ?: TileSize.MEDIUM,
                onSelectTileSize = viewModel::setTileSize,
                theme = appsState?.theme ?: ThemeChoice.Default,
                onSelectTheme = viewModel::setTheme,
            )
        }
    }
}
