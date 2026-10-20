/*
 * Copyright 2026 Viacheslav Melnichenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.nexatv.launcher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nexatv.launcher.ui.theme.NexaTheme

/**
 * A focusable app/content tile. The D-pad focus treatment — a [NexaTheme.colors.focusRing]
 * border plus a lifted surface — is driven entirely by tokens, so every theme restyles it
 * for free. `indication = null`: the ring *is* the focus affordance (no Material ripple on TV).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun AppTile(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val colors = NexaTheme.colors
    val shape = RoundedCornerShape(NexaTheme.radius.md)

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (focused) colors.surfaceVariant else colors.surface)
            .border(
                border = BorderStroke(
                    width = if (focused) 3.dp else 1.dp,
                    color = if (focused) colors.focusRing else colors.surfaceVariant,
                ),
                shape = shape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(NexaTheme.spacing.md),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview
@Composable
private fun AppTilePreview() {
    NexaTheme {
        AppTile(
            label = "Settings",
            modifier = Modifier.size(width = 220.dp, height = 132.dp),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview
@Composable
private fun AppTileWidePreview() {
    NexaTheme {
        AppTile(
            label = "Media Player",
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
        )
    }
}
