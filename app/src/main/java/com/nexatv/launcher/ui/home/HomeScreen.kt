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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed as itemsIndexedRow
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
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

/** The Home surface: the pre-Home prompt (FR-015) or, once NexaTV is Home, the D-pad app grid. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onLaunch: (AppEntry) -> Boolean,
    onRequestHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        when (state) {
            HomeUiState.Loading -> CenteredMessage(stringResource(R.string.home_loading))
            HomeUiState.NeedsHomeRole -> PreHomePrompt(onRequestHome = onRequestHome)
            HomeUiState.Empty -> EmptyState()
            is HomeUiState.Apps -> AppsContent(
                favorites = state.favorites,
                apps = state.apps,
                onLaunch = onLaunch,
            )
        }
    }
}

/**
 * The Home content: an optional "Favorites" row (ADR-0008) above the full app grid. Favorites are
 * pinned apps the owner reaches first; the grid below lists every launchable app. A short OK press
 * on any tile launches it (the long-press context menu arrives in a follow-up change).
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun AppsContent(
    favorites: List<AppEntry>,
    apps: List<AppEntry>,
    onLaunch: (AppEntry) -> Boolean,
) {
    var launchError by remember { mutableStateOf<String?>(null) }
    val favoritesFocus = remember { FocusRequester() }
    val gridFocus = remember { FocusRequester() }
    val failedTemplate = stringResource(R.string.home_launch_failed)

    fun tryLaunch(app: AppEntry) {
        if (!onLaunch(app)) launchError = failedTemplate.format(app.label)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (favorites.isNotEmpty()) {
                FavoritesRow(favorites, favoritesFocus, onLaunch = ::tryLaunch)
            }
            AppGridSection(apps, favoritesPresent = favorites.isNotEmpty(), gridFocus, onLaunch = ::tryLaunch)
        }
        launchError?.let { message -> TransientMessage(message) }
    }

    // FR-006/FR-011: land focus on the favorites row when present, otherwise the first grid tile,
    // so the remote works immediately. Re-runs if the favorites row appears or disappears.
    LaunchedEffect(favorites.isEmpty()) {
        val target = if (favorites.isEmpty()) gridFocus else favoritesFocus
        runCatching { target.requestFocus() }
    }
    // FR-014: launch errors are shown in-UI (focus-preserving) and auto-dismiss.
    LaunchedEffect(launchError) {
        if (launchError != null) {
            delay(ERROR_VISIBLE_MS)
            launchError = null
        }
    }
}

/** The horizontal "Favorites" row (ADR-0008); the first tile holds the row's entry focus. */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun FavoritesRow(
    favorites: List<AppEntry>,
    firstFocus: FocusRequester,
    onLaunch: (AppEntry) -> Unit,
) {
    SectionHeader(
        title = stringResource(R.string.home_favorites_title),
        modifier = Modifier.padding(
            start = NexaTheme.spacing.safeAreaHorizontal,
            end = NexaTheme.spacing.safeAreaHorizontal,
            top = NexaTheme.spacing.screenMargin,
        ),
    )
    LazyRow(
        contentPadding = PaddingValues(
            horizontal = NexaTheme.spacing.safeAreaHorizontal,
            vertical = NexaTheme.spacing.md,
        ),
        horizontalArrangement = Arrangement.spacedBy(NexaTheme.spacing.md),
        modifier = Modifier.fillMaxWidth(),
    ) {
        itemsIndexedRow(
            items = favorites,
            key = { _, app -> "fav:" + app.component.flattenToShortString() },
        ) { index, app ->
            AppTile(
                label = app.label,
                icon = app.icon,
                modifier = Modifier
                    .width(TILE_MIN_WIDTH)
                    .height(TILE_HEIGHT)
                    .then(if (index == 0) Modifier.focusRequester(firstFocus) else Modifier),
                onClick = { onLaunch(app) },
            )
        }
    }
}

/** The full app grid (FR-002). Fills the remaining height; its first tile holds the grid's focus. */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun ColumnScope.AppGridSection(
    apps: List<AppEntry>,
    favoritesPresent: Boolean,
    gridFocus: FocusRequester,
    onLaunch: (AppEntry) -> Unit,
) {
    SectionHeader(
        title = stringResource(R.string.home_apps_title),
        modifier = Modifier.padding(
            start = NexaTheme.spacing.safeAreaHorizontal,
            end = NexaTheme.spacing.safeAreaHorizontal,
            top = if (favoritesPresent) NexaTheme.spacing.sm else NexaTheme.spacing.screenMargin,
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
        // Restore (or, on first entry, seed) focus to the first tile so the very first
        // D-pad press moves instead of being absorbed re-entering the grid (FR-006).
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
                    .height(TILE_HEIGHT)
                    .then(if (index == 0) Modifier.focusRequester(gridFocus) else Modifier),
                onClick = { onLaunch(app) },
            )
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

/**
 * FR-015 / ADR-0006 pre-Home prompt: shown until NexaTV holds `ROLE_HOME`. A clear explanation plus a
 * single focusable action that asks the system (reversibly) to make NexaTV the default Home.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PreHomePrompt(onRequestHome: () -> Unit) {
    val action = remember { FocusRequester() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = NexaTheme.spacing.safeAreaHorizontal, vertical = NexaTheme.spacing.screenMargin),
        verticalArrangement = Arrangement.spacedBy(NexaTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_prompt_title),
            style = MaterialTheme.typography.headlineMedium,
            color = NexaTheme.colors.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.home_prompt_body),
            style = MaterialTheme.typography.bodyLarge,
            color = NexaTheme.colors.onSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRequestHome,
            modifier = Modifier.focusRequester(action),
        ) {
            Text(stringResource(R.string.home_prompt_action))
        }
    }
    LaunchedEffect(Unit) { runCatching { action.requestFocus() } }
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
