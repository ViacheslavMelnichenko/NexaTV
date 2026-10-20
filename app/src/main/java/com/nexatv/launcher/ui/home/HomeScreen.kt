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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nexatv.launcher.R
import com.nexatv.launcher.data.AppEntry
import com.nexatv.launcher.ui.components.AppTile
import com.nexatv.launcher.ui.components.SectionHeader
import com.nexatv.launcher.ui.theme.NexaTheme
import kotlinx.coroutines.delay

/** Width floor for a grid cell; the grid fits as many columns as the screen allows. */
private val TILE_MIN_WIDTH = 180.dp
private val TILE_HEIGHT = 220.dp
private const val ERROR_VISIBLE_MS = 3000L

/** The M2 Home: render discovered apps in a D-pad-navigable grid and launch on OK. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onLaunch: (AppEntry) -> Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        when (state) {
            HomeUiState.Loading -> CenteredMessage(stringResource(R.string.home_loading))
            HomeUiState.Empty -> EmptyState()
            is HomeUiState.Apps -> AppGrid(apps = state.apps, onLaunch = onLaunch)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppGrid(
    apps: List<AppEntry>,
    onLaunch: (AppEntry) -> Boolean,
) {
    var launchError by remember { mutableStateOf<String?>(null) }
    val firstItem = remember { FocusRequester() }
    val failedTemplate = stringResource(R.string.home_launch_failed)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SectionHeader(
                title = stringResource(R.string.home_apps_title),
                modifier = Modifier.padding(
                    start = NexaTheme.spacing.safeAreaHorizontal,
                    end = NexaTheme.spacing.safeAreaHorizontal,
                    top = NexaTheme.spacing.screenMargin,
                ),
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = TILE_MIN_WIDTH),
                contentPadding = PaddingValues(
                    start = NexaTheme.spacing.safeAreaHorizontal,
                    end = NexaTheme.spacing.safeAreaHorizontal,
                    top = NexaTheme.spacing.md,
                    bottom = NexaTheme.spacing.screenMargin,
                ),
                horizontalArrangement = Arrangement.spacedBy(NexaTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(NexaTheme.spacing.md),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(
                    items = apps,
                    key = { _, app -> app.component.flattenToShortString() },
                ) { index, app ->
                    AppTile(
                        label = app.label,
                        icon = app.icon,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TILE_HEIGHT)
                            .then(if (index == 0) Modifier.focusRequester(firstItem) else Modifier),
                        onClick = {
                            if (!onLaunch(app)) launchError = failedTemplate.format(app.label)
                        },
                    )
                }
            }
        }
        launchError?.let { message -> TransientMessage(message) }
    }

    // FR-006/FR-011: land focus on the first tile so the remote works immediately.
    LaunchedEffect(Unit) { runCatching { firstItem.requestFocus() } }
    // FR-014: launch errors are shown in-UI (focus-preserving) and auto-dismiss.
    LaunchedEffect(launchError) {
        if (launchError != null) {
            delay(ERROR_VISIBLE_MS)
            launchError = null
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BoxScope.TransientMessage(text: String) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(NexaTheme.spacing.screenMargin)
            .clip(RoundedCornerShape(NexaTheme.radius.md))
            .background(NexaTheme.colors.error)
            .padding(horizontal = NexaTheme.spacing.lg, vertical = NexaTheme.spacing.md),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = NexaTheme.colors.surface,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CenteredMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = NexaTheme.colors.onSurfaceMuted,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(NexaTheme.spacing.screenMargin),
        verticalArrangement = Arrangement.spacedBy(NexaTheme.spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            color = NexaTheme.colors.onSurface,
        )
        Text(
            text = stringResource(R.string.home_empty_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = NexaTheme.colors.onSurfaceMuted,
        )
    }
}
