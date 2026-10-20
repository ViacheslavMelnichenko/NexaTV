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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.LifecycleEventObserver
import com.nexatv.launcher.data.HomeRoleRepository
import com.nexatv.launcher.data.InstalledAppsRepository
import com.nexatv.launcher.ui.home.HomeScreen
import com.nexatv.launcher.ui.home.HomeViewModel
import com.nexatv.launcher.ui.theme.NexaTheme

/**
 * Home entry point. Discovers installed apps via [HomeViewModel] and renders a D-pad-navigable
 * grid that launches on OK (M2). Until NexaTV holds `ROLE_HOME` it shows the pre-Home prompt and
 * drives the system role request (M3 / ADR-0006); state is re-evaluated on return from that flow
 * and on every `ON_RESUME`, so becoming — or ceasing to be — Home is reflected without a restart.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexaTheme {
                val context = LocalContext.current.applicationContext
                val roleRepository = remember { HomeRoleRepository(context) }
                val viewModel: HomeViewModel = viewModel {
                    HomeViewModel(InstalledAppsRepository(context), roleRepository)
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
                HomeScreen(
                    state = state,
                    onLaunch = viewModel::launch,
                    onRequestHome = { roleRequest.launch(roleRepository.requestHomeIntent()) },
                )
            }
        }
    }
}
