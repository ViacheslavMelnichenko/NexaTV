/*
 * Copyright 2026 Viacheslav Melnichenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.nexatv.launcher.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.nexatv.launcher.data.TileSize
import com.nexatv.launcher.ui.components.SectionHeader
import com.nexatv.launcher.ui.theme.NexaTheme

/**
 * The Settings surface (M5.1 / ADR-0009): a plain D-pad list. v1 holds one row — the home tile size —
 * whose options write through immediately (no Save button), so the home grid updates live via the
 * settings Flow. BACK returns to Home; that is handled by the caller, which owns the screen state.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    tileSize: TileSize,
    onSelectTileSize: (TileSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = NexaTheme.spacing.safeAreaHorizontal,
                    vertical = NexaTheme.spacing.screenMargin,
                ),
            verticalArrangement = Arrangement.spacedBy(NexaTheme.spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.displayLarge,
                color = NexaTheme.colors.onSurface,
            )
            TileSizeRow(selected = tileSize, onSelect = onSelectTileSize)
        }
    }
}

/** The "Tile size" setting: a label above three selectable S/M/L options that write on focus-select. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TileSizeRow(selected: TileSize, onSelect: (TileSize) -> Unit) {
    val firstOption = remember { FocusRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(NexaTheme.spacing.sm)) {
        SectionHeader(title = stringResource(R.string.settings_tile_size))
        Row(horizontalArrangement = Arrangement.spacedBy(NexaTheme.spacing.md)) {
            TileSize.entries.forEachIndexed { index, size ->
                TileSizeOption(
                    label = stringResource(size.labelRes()),
                    selected = size == selected,
                    onClick = { onSelect(size) },
                    modifier = if (index == 0) Modifier.focusRequester(firstOption) else Modifier,
                )
            }
        }
    }
    // Land focus on the first option so the remote acts immediately on entering Settings (FR-006).
    LaunchedEffect(Unit) { runCatching { firstOption.requestFocus() } }
}

/**
 * One selectable option chip. The focus ring is the navigation cue; a persistent accent border marks
 * the currently-selected value even without focus, so the active choice is always visible. No ripple.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TileSizeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val colors = NexaTheme.colors
    val shape = RoundedCornerShape(NexaTheme.radius.md)

    Text(
        text = label,
        style = MaterialTheme.typography.titleLarge,
        color = if (selected) colors.onAccent else colors.onSurface,
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.accent else colors.surfaceVariant)
            .border(
                border = BorderStroke(
                    width = if (focused) 3.dp else 1.dp,
                    color = when {
                        focused -> colors.focusRing
                        selected -> colors.accent
                        else -> colors.surfaceVariant
                    },
                ),
                shape = shape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = NexaTheme.spacing.lg, vertical = NexaTheme.spacing.md),
    )
}

/** The display label for a tile-size preset. */
private fun TileSize.labelRes(): Int = when (this) {
    TileSize.SMALL -> R.string.settings_tile_size_small
    TileSize.MEDIUM -> R.string.settings_tile_size_medium
    TileSize.LARGE -> R.string.settings_tile_size_large
}
