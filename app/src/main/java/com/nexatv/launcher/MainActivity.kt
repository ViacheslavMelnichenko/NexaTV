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
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexatv.launcher.data.InstalledAppsRepository
import com.nexatv.launcher.ui.home.HomeScreen
import com.nexatv.launcher.ui.home.HomeViewModel
import com.nexatv.launcher.ui.theme.NexaTheme

/**
 * Home entry point. M2 (Minimal Launcher): discovers installed apps via [HomeViewModel]
 * and renders a D-pad-navigable grid that launches on OK. HOME registration (M3), favorites
 * and live updates (M4) build on this.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexaTheme {
                val context = LocalContext.current.applicationContext
                val viewModel: HomeViewModel = viewModel {
                    HomeViewModel(InstalledAppsRepository(context))
                }
                val state by viewModel.state.collectAsStateWithLifecycle()
                HomeScreen(state = state, onLaunch = viewModel::launch)
            }
        }
    }
}
