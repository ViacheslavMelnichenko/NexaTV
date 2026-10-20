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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nexatv.launcher.ui.theme.NexaTheme

/**
 * The canonical NexaTV tile: a square art area over a single-line label (FR-013).
 * The D-pad focus treatment — a [NexaTheme.colors.focusRing] border plus a lifted surface — is
 * driven entirely by tokens, so every theme restyles it for free. `indication = null`: the ring
 * *is* the focus affordance (no Material ripple on TV). When [icon] is null a monogram is drawn.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun AppTile(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageBitmap? = null,
    onClick: () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val colors = NexaTheme.colors
    val shape = RoundedCornerShape(NexaTheme.radius.md)

    Column(
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
        verticalArrangement = Arrangement.spacedBy(NexaTheme.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Monogram(label = label)
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        )
    }
}

/** Art-less fallback: the label's first letter on an accent chip (FR-013). */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Monogram(label: String) {
    val colors = NexaTheme.colors
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(NexaTheme.radius.sm))
            .background(colors.accent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.trim().take(1).uppercase().ifEmpty { "?" },
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onAccent,
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
            modifier = Modifier.size(width = 180.dp, height = 220.dp),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview
@Composable
private fun AppTileLongLabelPreview() {
    NexaTheme {
        AppTile(
            label = "A Very Long Media Application Name",
            modifier = Modifier.size(width = 180.dp, height = 220.dp),
        )
    }
}
