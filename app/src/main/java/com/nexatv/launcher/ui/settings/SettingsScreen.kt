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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nexatv.launcher.R
import com.nexatv.launcher.data.ThemeChoice
import com.nexatv.launcher.data.TileSize
import com.nexatv.launcher.ui.theme.NexaTheme

/**
 * The Settings surface (ADR-0009): a short D-pad list of setting rows. Each row is a contained card —
 * its name on the left, its selectable options on the right — so the screen reads like a real TV
 * settings menu rather than loose blocks. Options write through immediately (no Save button): the home
 * grid and theme update live via the settings Flow. A faint build version sits in the bottom-right
 * corner. BACK returns to Home; that is handled by the caller, which owns the screen state.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    tileSize: TileSize,
    onSelectTileSize: (TileSize) -> Unit,
    theme: ThemeChoice,
    onSelectTheme: (ThemeChoice) -> Unit,
    versionName: String,
    onOpenBackground: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            // The screen sits on the deep base surface so the surfaceVariant setting cards lift off it;
            // tv-material's default Surface paints surfaceVariant, which would hide the cards.
            .background(NexaTheme.colors.surface)
            .padding(
                horizontal = NexaTheme.spacing.safeAreaHorizontal,
                vertical = NexaTheme.spacing.screenMargin,
            ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NexaTheme.spacing.md)) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.displayLarge,
                color = NexaTheme.colors.onSurface,
                modifier = Modifier.padding(bottom = NexaTheme.spacing.sm),
            )
            TileSizeRow(selected = tileSize, onSelect = onSelectTileSize)
            ThemeRow(selected = theme, onSelect = onSelectTheme)
            SettingRow(label = stringResource(R.string.background_title)) {
                SettingOptionChip(stringResource(R.string.background_customize), false, onOpenBackground)
            }
        }

        // Faint build version, low-emphasis, pinned to the bottom-right corner.
        Text(
            text = "NexaTV  v$versionName",
            style = MaterialTheme.typography.bodyMedium,
            color = NexaTheme.colors.onSurfaceMuted,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

/** The "Tile size" setting row: S/M/L options that write on select; initial focus lands here. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TileSizeRow(selected: TileSize, onSelect: (TileSize) -> Unit) {
    val firstOption = remember { FocusRequester() }
    SettingRow(label = stringResource(R.string.settings_tile_size)) {
        TileSize.entries.forEachIndexed { index, size ->
            SettingOptionChip(
                label = stringResource(size.labelRes()),
                selected = size == selected,
                onClick = { onSelect(size) },
                modifier = if (index == 0) Modifier.focusRequester(firstOption) else Modifier,
            )
        }
    }
    // Land focus on the first option so the remote acts immediately on entering Settings (FR-006).
    LaunchedEffect(Unit) { runCatching { firstOption.requestFocus() } }
}

/** The "Theme" setting row: the color-theme options that write on select. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ThemeRow(selected: ThemeChoice, onSelect: (ThemeChoice) -> Unit) {
    SettingRow(label = stringResource(R.string.settings_theme)) {
        ThemeChoice.entries.forEach { choice ->
            SettingOptionChip(
                label = stringResource(choice.labelRes()),
                selected = choice == selected,
                onClick = { onSelect(choice) },
            )
        }
    }
}

/**
 * One setting as a card: the [label] on the left, its [options] grouped on the right. The card's
 * surfaceVariant fill separates the row from the base surface so each setting reads as its own block.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SettingRow(
    label: String,
    options: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NexaTheme.radius.md))
            .background(NexaTheme.colors.surfaceVariant)
            .padding(horizontal = NexaTheme.spacing.lg, vertical = NexaTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = NexaTheme.colors.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(NexaTheme.spacing.sm)) { options() }
    }
}

/**
 * One selectable option chip, shared by every settings row. Smaller than a page control: 18sp label
 * with tight padding. The focus ring is the navigation cue; a persistent accent fill marks the active
 * value even without focus, and an unselected chip keeps a muted outline so it stays visible on the
 * card. No ripple.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SettingOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val colors = NexaTheme.colors
    val shape = RoundedCornerShape(NexaTheme.radius.sm)

    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) colors.onAccent else colors.onSurface,
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.accent else Color.Transparent)
            .border(
                border = BorderStroke(
                    width = if (focused) 3.dp else 1.dp,
                    color = when {
                        focused -> colors.focusRing
                        selected -> colors.accent
                        else -> colors.onSurfaceMuted
                    },
                ),
                shape = shape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = NexaTheme.spacing.md, vertical = NexaTheme.spacing.sm),
    )
}

/** The display label for a tile-size preset. */
private fun TileSize.labelRes(): Int = when (this) {
    TileSize.SMALL -> R.string.settings_tile_size_small
    TileSize.MEDIUM -> R.string.settings_tile_size_medium
    TileSize.LARGE -> R.string.settings_tile_size_large
}

/** The display label for a color theme. */
private fun ThemeChoice.labelRes(): Int = when (this) {
    ThemeChoice.PASTEL_DARK -> R.string.settings_theme_pastel_dark
    ThemeChoice.OLED_BLACK -> R.string.settings_theme_oled_black
    ThemeChoice.AMBER_EMBER -> R.string.settings_theme_amber_ember
    ThemeChoice.MAGENTA_NOIR -> R.string.settings_theme_magenta_noir
}
