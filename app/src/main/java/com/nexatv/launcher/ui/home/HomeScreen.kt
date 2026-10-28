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
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed as itemsIndexedRow
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
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.nexatv.launcher.R
import com.nexatv.launcher.data.BackgroundSettings
import com.nexatv.launcher.data.BackgroundMode
import com.nexatv.launcher.data.AppEntry
import com.nexatv.launcher.data.lazyItemKey
import com.nexatv.launcher.data.toFavoriteKey
import com.nexatv.launcher.ui.components.AppContextMenu
import com.nexatv.launcher.ui.components.AppTile
import com.nexatv.launcher.ui.theme.NexaTheme
import com.nexatv.launcher.ui.theme.backdropBrush
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
    background: BackgroundSettings,
    environment: HomeEnvironment,
    actions: HomeActions,
    onRequestHome: () -> Unit,
    onOpenAllApps: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NexaTheme.colors
    // The gradient wash allocates a Brush + color list; build it once per palette, not per composition.
    val backdrop = remember(colors) { colors.backdropBrush() }
    Box(modifier.fillMaxSize().background(backdrop)) {
        val landscape = background.mode != BackgroundMode.THEME
        if (landscape && state is HomeUiState.Apps) {
            AdaptiveBackground(background, resolveScene(background, environment.now, environment.weather))
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            colors = SurfaceDefaults.colors(containerColor = Color.Transparent),
        ) {
            when (state) {
                HomeUiState.Loading -> CenteredMessage(stringResource(R.string.home_loading))
                HomeUiState.NeedsHomeRole -> PreHomePrompt(onRequestHome = onRequestHome)
                HomeUiState.Empty -> EmptyState()
                is HomeUiState.Apps -> AppsContent(
                    favorites = state.favorites,
                    tile = state.tileSize.dimensions(),
                    actions = actions,
                    environment = environment,
                    landscape = landscape,
                    onOpenAllApps = onOpenAllApps,
                    onOpenSettings = onOpenSettings,
                )
            }
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
 * The Home content: the cinematic top bar over the favorites row, which is the whole of Home now. Home
 * shows only the owner's pinned apps (ADR-0008), centred on the screen; the full catalogue moved to its
 * own All-apps page reached from the top bar. Short OK launches; long OK opens the context menu
 * (pin/unpin, move, app info). When nothing is pinned yet, a hint points at the All-apps page.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun AppsContent(
    favorites: List<AppEntry>,
    tile: TileDimensions,
    actions: HomeActions,
    environment: HomeEnvironment,
    landscape: Boolean,
    onOpenAllApps: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var launchError by remember { mutableStateOf<String?>(null) }
    var menuTarget by remember { mutableStateOf<AppEntry?>(null) }
    var reorder by remember { mutableStateOf<ReorderSession?>(null) }
    val favoritesFocus = remember { FocusRequester() }
    val failedTemplate = stringResource(R.string.home_launch_failed)
    val favoriteKeys = remember(favorites) { favorites.map { it.toFavoriteKey() }.toSet() }

    fun tryLaunch(app: AppEntry) {
        if (!actions.onLaunch(app)) launchError = failedTemplate.format(app.label)
    }

    val favoritesPresent = favorites.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeTopBar(onOpenAllApps = onOpenAllApps, onOpenSettings = onOpenSettings)
            // The centre holds the two equal widgets (calendar + weather); it takes the free vertical
            // space between the top bar and the bottom favorites band (ADR-0010).
            HomeWidgets(
                environment = environment,
                landscape = landscape,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = NexaTheme.spacing.safeAreaHorizontal),
            )
            // The favorites row anchors to the bottom-centre; the LazyRow centres the tiles itself.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = NexaTheme.spacing.screenMargin),
                contentAlignment = Alignment.BottomCenter,
            ) {
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
                } else {
                    EmptyFavoritesHint()
                }
            }
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

    // FR-006/FR-011: land focus on the favorites row when present, so the remote works immediately.
    // With no favorites there is nothing to focus here; the top-bar buttons take the first press.
    LaunchedEffect(favoritesPresent) {
        if (favoritesPresent) runCatching { favoritesFocus.requestFocus() }
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
 * The cinematic Home top bar (M5): a live clock on the left, and on the right the All-apps button
 * followed by the Settings gear. It is a thin, content-justified row — the clock earns its vertical space
 * — so it reads as a status bar rather than the empty gear strip it replaces. Both buttons stay reachable
 * by a D-pad ▲ from the favorites row below.
 */
@Composable
private fun HomeTopBar(onOpenAllApps: () -> Unit, onOpenSettings: () -> Unit) {
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(NexaTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopBarIconButton(
                iconRes = R.drawable.ic_apps,
                contentDescription = stringResource(R.string.home_open_all_apps),
                onClick = onOpenAllApps,
            )
            TopBarIconButton(
                iconRes = R.drawable.ic_settings,
                contentDescription = stringResource(R.string.home_open_settings),
                onClick = onOpenSettings,
            )
        }
    }
}

/**
 * A live clock, formatted through [DateFormat] so the 12/24-hour choice, locale and timezone all follow
 * the system. It ticks off `ACTION_TIME_TICK` (one broadcast a minute, on the minute) rather than a
 * polling loop, so it costs nothing between updates; the receiver is scoped to the composition.
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

    Text(
        text = timeText,
        style = MaterialTheme.typography.displayLarge,
        color = NexaTheme.colors.onSurface,
    )
}

/**
 * A focusable round top-bar icon button; the focus ring is the cue (no ripple). Shared by the All-apps
 * and Settings entries so both read identically — only the glyph and action differ.
 */
@Composable
private fun TopBarIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
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
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(if (focused) colors.onAccent else colors.onSurface),
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * The horizontal favorites row (ADR-0008) — the whole of Home now, so the tiles are centred. Normally the
 * first tile holds the row's entry focus; in a [reorder] session the row previews the moved order, keeps
 * focus on the moving tile, and traps the D-pad (◄ ► move, OK commit, BACK cancel) so the shuffle can't
 * accidentally leave the row.
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

    LazyRow(
        contentPadding = PaddingValues(
            horizontal = NexaTheme.spacing.safeAreaHorizontal,
            // The row clips to its own bounds, so this vertical room is what keeps a focused tile's
            // glow (see AppTile) from being sheared off at the top or bottom edge.
            vertical = NexaTheme.spacing.md,
        ),
        // Centre the tiles horizontally: Home holds only favorites now, so a short row reads as a
        // deliberate, balanced cluster rather than hugging the left safe-area margin.
        horizontalArrangement = Arrangement.spacedBy(NexaTheme.spacing.md, Alignment.CenterHorizontally),
        modifier = Modifier
            .fillMaxWidth()
            // Returning to Home lands focus back on the last-focused favorite (parity with the All-apps
            // grid), falling back to the first tile — not an unpredictable bounce to favorite #1.
            .focusRestorer { firstFocus }
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
            key = { _, app -> "fav:" + app.lazyItemKey() },
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

/** Shown on Home when nothing is pinned yet: a gentle pointer to the All-apps page to pin from. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EmptyFavoritesHint() {
    Text(
        text = stringResource(R.string.home_favorites_empty),
        style = MaterialTheme.typography.bodyLarge,
        color = NexaTheme.colors.onSurfaceMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = NexaTheme.spacing.safeAreaHorizontal),
    )
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
