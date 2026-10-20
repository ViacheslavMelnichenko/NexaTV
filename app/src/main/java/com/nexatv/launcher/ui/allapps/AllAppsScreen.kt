/*
 * Copyright 2026 Viacheslav Melnichenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.nexatv.launcher.ui.allapps

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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.nexatv.launcher.R
import com.nexatv.launcher.data.AppEntry
import com.nexatv.launcher.data.toFavoriteKey
import com.nexatv.launcher.ui.components.AppContextMenu
import com.nexatv.launcher.ui.components.AppTile
import com.nexatv.launcher.ui.theme.NexaTheme
import com.nexatv.launcher.ui.theme.TileDimensions
import com.nexatv.launcher.ui.theme.backdropBrush
import kotlinx.coroutines.delay

private const val ERROR_VISIBLE_MS = 3000L

/**
 * The full app catalogue (FR-002), now its own page reached from the Home top bar. Home shows only
 * favorites; every launchable app lives here. Short OK launches; long OK opens the context menu to
 * pin/unpin a favorite or open App info. BACK returns to Home — the caller owns that screen state.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
internal fun AllAppsScreen(
    apps: List<AppEntry>,
    favorites: List<AppEntry>,
    tile: TileDimensions,
    onLaunch: (AppEntry) -> Boolean,
    onToggleFavorite: (AppEntry) -> Unit,
    onAppInfo: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var launchError by remember { mutableStateOf<String?>(null) }
    var menuTarget by remember { mutableStateOf<AppEntry?>(null) }
    val gridFocus = remember { FocusRequester() }
    val failedTemplate = stringResource(R.string.home_launch_failed)
    val favoriteKeys = remember(favorites) { favorites.map { it.toFavoriteKey() }.toSet() }
    val colors = NexaTheme.colors
    // The gradient wash allocates a Brush + color list; build it once per palette, not per composition.
    val backdrop = remember(colors) { colors.backdropBrush() }

    fun tryLaunch(app: AppEntry) {
        if (!onLaunch(app)) launchError = failedTemplate.format(app.label)
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(backdrop),
        colors = SurfaceDefaults.colors(containerColor = Color.Transparent),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(R.string.home_apps_title),
                    style = MaterialTheme.typography.displayLarge,
                    color = NexaTheme.colors.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = NexaTheme.spacing.safeAreaHorizontal,
                            end = NexaTheme.spacing.safeAreaHorizontal,
                            top = NexaTheme.spacing.screenMargin,
                            bottom = NexaTheme.spacing.sm,
                        ),
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = tile.width),
                    contentPadding = PaddingValues(
                        start = NexaTheme.spacing.safeAreaHorizontal,
                        end = NexaTheme.spacing.safeAreaHorizontal,
                        top = NexaTheme.spacing.md,
                        bottom = NexaTheme.spacing.screenMargin,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(NexaTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(NexaTheme.spacing.md),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .focusRestorer { gridFocus },
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
                                .height(tile.height)
                                .then(if (index == 0) Modifier.focusRequester(gridFocus) else Modifier),
                            onClick = { tryLaunch(app) },
                            onLongClick = { menuTarget = app },
                        )
                    }
                }
            }
            launchError?.let { message -> TransientMessage(message) }
        }
    }

    menuTarget?.let { target ->
        AppContextMenu(
            app = target,
            isFavorite = target.toFavoriteKey() in favoriteKeys,
            onToggleFavorite = { onToggleFavorite(target); menuTarget = null },
            onAppInfo = { onAppInfo(target); menuTarget = null },
            onDismiss = { menuTarget = null },
        )
    }

    // Land focus on the first tile so the remote acts immediately on entering the page (FR-006).
    LaunchedEffect(Unit) { runCatching { gridFocus.requestFocus() } }
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
