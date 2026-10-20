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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed as itemsIndexedRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nexatv.launcher.R
import com.nexatv.launcher.data.AppEntry
import com.nexatv.launcher.data.toFavoriteKey
import com.nexatv.launcher.ui.components.AppTile
import com.nexatv.launcher.ui.components.SectionHeader
import com.nexatv.launcher.ui.theme.NexaTheme
import com.nexatv.launcher.ui.theme.TileDimensions
import com.nexatv.launcher.ui.theme.dimensions
import java.util.Date
import kotlinx.coroutines.delay

private const val ERROR_VISIBLE_MS = 3000L

/** The Home surface: the pre-Home prompt (FR-015) or, once NexaTV is Home, the D-pad app grid. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    actions: HomeActions,
    onRequestHome: () -> Unit,
    onOpenSettings: () -> Unit,
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
                tile = state.tileSize.dimensions(),
                actions = actions,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

/** The per-app actions the Home surface invokes: launch (OK) plus the long-press menu entries. */
@Immutable
internal class HomeActions(
    val onLaunch: (AppEntry) -> Boolean,
    val onToggleFavorite: (AppEntry) -> Unit,
    val onMoveFavorite: (Int, Int) -> Unit,
    val onAppInfo: (AppEntry) -> Unit,
)

/** A live "move favorite" session: the moving tile started at [origin] and now sits at slot [current]. */
private class ReorderSession(val origin: Int, val current: Int)

/** Pure list move used to preview a reorder without persisting until the owner commits. */
private fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

/**
 * The Home content: an optional "Favorites" row (ADR-0008) above the full app grid. Favorites are
 * pinned apps the owner reaches first; the grid below lists every launchable app. Short OK launches;
 * long OK opens the context menu (pin/unpin, move, app info).
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun AppsContent(
    favorites: List<AppEntry>,
    apps: List<AppEntry>,
    tile: TileDimensions,
    actions: HomeActions,
    onOpenSettings: () -> Unit,
) {
    var launchError by remember { mutableStateOf<String?>(null) }
    var menuTarget by remember { mutableStateOf<AppEntry?>(null) }
    var reorder by remember { mutableStateOf<ReorderSession?>(null) }
    val favoritesFocus = remember { FocusRequester() }
    val gridFocus = remember { FocusRequester() }
    val failedTemplate = stringResource(R.string.home_launch_failed)
    val favoriteKeys = remember(favorites) { favorites.map { it.toFavoriteKey() }.toSet() }

    fun tryLaunch(app: AppEntry) {
        if (!actions.onLaunch(app)) launchError = failedTemplate.format(app.label)
    }

    val favoritesPresent = favorites.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeTopBar(onOpenSettings = onOpenSettings)
            if (favoritesPresent) {
                FavoritesRow(
                    favorites = favorites,
                    tile = tile,
                    firstFocus = favoritesFocus,
                    reorder = reorder,
                    onLaunch = ::tryLaunch,
                    onLongPress = { menuTarget = it },
                    onReorderMove = { delta ->
                        reorder?.let { s ->
                            val next = (s.current + delta).coerceIn(0, favorites.lastIndex)
                            if (next != s.current) reorder = ReorderSession(s.origin, next)
                        }
                    },
                    onReorderCommit = {
                        reorder?.let { actions.onMoveFavorite(it.origin, it.current) }
                        reorder = null
                    },
                    onReorderCancel = { reorder = null },
                )
            }
            AppGridSection(
                apps,
                tile = tile,
                gridFocus = gridFocus,
                onLaunch = ::tryLaunch,
                onLongPress = { menuTarget = it },
            )
        }
        if (reorder != null) ReorderHint()
        launchError?.let { message -> TransientMessage(message) }
    }

    menuTarget?.let { target ->
        val key = target.toFavoriteKey()
        AppContextMenu(
            app = target,
            isFavorite = key in favoriteKeys,
            onToggleFavorite = { actions.onToggleFavorite(target); menuTarget = null },
            onMove = {
                val index = favorites.indexOfFirst { it.toFavoriteKey() == key }
                if (index >= 0) reorder = ReorderSession(index, index)
                menuTarget = null
            },
            onAppInfo = { actions.onAppInfo(target); menuTarget = null },
            onDismiss = { menuTarget = null },
        )
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

/**
 * The cinematic Home top bar (M5): a live clock + date on the left, the Settings gear on the right. It is
 * a thin, content-justified row — the clock earns its vertical space — so it reads as a status bar rather
 * than the empty gear strip it replaces. The gear stays reachable by a D-pad ▲ from the rows below. Later
 * M5 steps add the status cluster (Search / Wi-Fi) beside the gear.
 */
@Composable
private fun HomeTopBar(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = NexaTheme.spacing.safeAreaHorizontal,
                end = NexaTheme.spacing.safeAreaHorizontal,
                top = NexaTheme.spacing.md,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Clock()
        SettingsEntry(onClick = onOpenSettings)
    }
}

/**
 * A live clock + long date, formatted through [DateFormat] so the 12/24-hour choice, locale and timezone
 * all follow the system. It ticks off `ACTION_TIME_TICK` (one broadcast a minute, on the minute) rather
 * than a polling loop, so it costs nothing between updates; the receiver is scoped to the composition.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Clock() {
    val context = LocalContext.current
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                now = System.currentTimeMillis()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        // Protected system broadcasts; NOT_EXPORTED keeps other apps from ever reaching this receiver.
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    val moment = remember(now) { Date(now) }
    val timeText = remember(now, context) { DateFormat.getTimeFormat(context).format(moment) }
    val dateText = remember(now, context) { DateFormat.getLongDateFormat(context).format(moment) }

    Column {
        Text(
            text = timeText,
            style = MaterialTheme.typography.displayLarge,
            color = NexaTheme.colors.onSurface,
        )
        Text(
            text = dateText,
            style = MaterialTheme.typography.bodyMedium,
            color = NexaTheme.colors.onSurfaceMuted,
        )
    }
}

/** A focusable round Settings icon; the focus ring is the cue (no ripple). Opens the Settings screen. */
@Composable
private fun SettingsEntry(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val colors = NexaTheme.colors
    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .background(if (focused) colors.accent else colors.surfaceVariant)
            .border(
                border = BorderStroke(
                    width = if (focused) 3.dp else 1.dp,
                    color = if (focused) colors.focusRing else colors.surfaceVariant,
                ),
                shape = shape,
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_settings),
            contentDescription = stringResource(R.string.home_open_settings),
            colorFilter = ColorFilter.tint(if (focused) colors.onAccent else colors.onSurface),
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * The horizontal "Favorites" row (ADR-0008). Normally the first tile holds the row's entry focus; in a
 * [reorder] session the row previews the moved order, keeps focus on the moving tile, and traps the
 * D-pad (◄ ► move, OK commit, BACK cancel) so the shuffle can't accidentally leave the row.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun FavoritesRow(
    favorites: List<AppEntry>,
    tile: TileDimensions,
    firstFocus: FocusRequester,
    reorder: ReorderSession?,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onReorderMove: (Int) -> Unit,
    onReorderCommit: () -> Unit,
    onReorderCancel: () -> Unit,
) {
    val display = if (reorder != null) favorites.moved(reorder.origin, reorder.current) else favorites
    val movingFocus = remember { FocusRequester() }

    SectionHeader(
        title = stringResource(R.string.home_favorites_title),
        modifier = Modifier.padding(
            start = NexaTheme.spacing.safeAreaHorizontal,
            end = NexaTheme.spacing.safeAreaHorizontal,
            // The top bar supplies the top margin, so this header only needs a small gap.
            top = NexaTheme.spacing.sm,
        ),
    )
    LazyRow(
        contentPadding = PaddingValues(
            horizontal = NexaTheme.spacing.safeAreaHorizontal,
            vertical = NexaTheme.spacing.sm,
        ),
        horizontalArrangement = Arrangement.spacedBy(NexaTheme.spacing.md),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (reorder != null) {
                    Modifier.onPreviewKeyEvent { reorderKey(it, onReorderMove, onReorderCommit, onReorderCancel) }
                } else {
                    Modifier
                },
            ),
    ) {
        itemsIndexedRow(
            items = display,
            key = { _, app -> "fav:" + app.component.flattenToShortString() },
        ) { index, app ->
            val isMoving = reorder != null && index == reorder.current
            AppTile(
                label = app.label,
                icon = app.icon,
                moving = isMoving,
                modifier = Modifier
                    .width(tile.width)
                    .height(tile.height)
                    .then(
                        when {
                            isMoving -> Modifier.focusRequester(movingFocus)
                            reorder == null && index == 0 -> Modifier.focusRequester(firstFocus)
                            else -> Modifier
                        },
                    ),
                onClick = { onLaunch(app) },
                onLongClick = { onLongPress(app) },
            )
        }
    }

    // Keep focus on the moving tile as it hops between slots, so ◄ ► keeps acting on the same item.
    LaunchedEffect(reorder?.current) {
        if (reorder != null) runCatching { movingFocus.requestFocus() }
    }
}

/** D-pad handling while reordering: ◄ ► move the tile, OK commits, BACK cancels; up/down are trapped. */
@OptIn(ExperimentalComposeUiApi::class)
private fun reorderKey(
    event: KeyEvent,
    onMove: (Int) -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
): Boolean {
    // Trap all arrow/OK/BACK presses (both edges) so the reorder shuffle can't leave the row; act once,
    // on key-down. Keys we don't handle fall through to normal focus navigation.
    val handled = event.key in REORDER_KEYS
    if (handled && event.type == KeyEventType.KeyDown) {
        when (event.key) {
            Key.DirectionLeft -> onMove(-1)
            Key.DirectionRight -> onMove(1)
            Key.DirectionCenter, Key.Enter -> onCommit()
            Key.Back -> onCancel()
        }
    }
    return handled
}

private val REORDER_KEYS = setOf(
    Key.DirectionLeft, Key.DirectionRight, Key.DirectionCenter, Key.Enter, Key.Back,
    Key.DirectionUp, Key.DirectionDown,
)

/** A bottom banner explaining the reorder controls while a move is in progress. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BoxScope.ReorderHint() {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(NexaTheme.spacing.screenMargin)
            .clip(RoundedCornerShape(NexaTheme.radius.md))
            .background(NexaTheme.colors.accent)
            .padding(horizontal = NexaTheme.spacing.lg, vertical = NexaTheme.spacing.md),
    ) {
        Text(
            text = stringResource(R.string.home_reorder_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = NexaTheme.colors.onAccent,
        )
    }
}

/** The full app grid (FR-002). Fills the remaining height; its first tile holds the grid's focus. */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun ColumnScope.AppGridSection(
    apps: List<AppEntry>,
    tile: TileDimensions,
    gridFocus: FocusRequester,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    SectionHeader(
        title = stringResource(R.string.home_apps_title),
        modifier = Modifier.padding(
            start = NexaTheme.spacing.safeAreaHorizontal,
            end = NexaTheme.spacing.safeAreaHorizontal,
            top = NexaTheme.spacing.sm,
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
                    .height(tile.height)
                    .then(if (index == 0) Modifier.focusRequester(gridFocus) else Modifier),
                onClick = { onLaunch(app) },
                onLongClick = { onLongPress(app) },
            )
        }
    }
}

/**
 * The long-press OK context menu (ADR-0008). No "Menu" key exists on the TV remote, so a long OK on a
 * tile opens this: pin/unpin, move (favorites only), open App info, or cancel. Short OK always launches,
 * so this is the only destructive-free way to act on an item without opening it.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppContextMenu(
    app: AppEntry,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onMove: () -> Unit,
    onAppInfo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val firstAction = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(NexaTheme.radius.md),
            modifier = Modifier.widthIn(min = 280.dp),
        ) {
            Column(
                modifier = Modifier.padding(NexaTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(NexaTheme.spacing.sm),
            ) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleLarge,
                    color = NexaTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = NexaTheme.spacing.sm),
                )
                val pinLabel = if (isFavorite) R.string.home_menu_unpin else R.string.home_menu_pin
                MenuAction(stringResource(pinLabel), onToggleFavorite, Modifier.focusRequester(firstAction))
                // "Move" reorders within the Favorites row, so it only applies to a pinned app.
                if (isFavorite) MenuAction(stringResource(R.string.home_menu_move), onMove)
                MenuAction(stringResource(R.string.home_menu_app_info), onAppInfo)
                MenuAction(stringResource(R.string.home_menu_cancel), onDismiss)
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { firstAction.requestFocus() } }
}

/** A full-width, D-pad-focusable row in the context menu. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MenuAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(text)
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
