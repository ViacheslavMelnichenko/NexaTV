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

import android.view.KeyEvent as AndroidKeyEvent
import android.view.ViewConfiguration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nexatv.launcher.ui.theme.NexaTheme

/** The "OK"/confirm keys on a TV remote or keyboard (no dedicated Menu key exists on the remote). */
private val OK_KEYCODES = setOf(
    AndroidKeyEvent.KEYCODE_DPAD_CENTER,
    AndroidKeyEvent.KEYCODE_ENTER,
    AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
)

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
    moving: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val colors = NexaTheme.colors
    val shape = RoundedCornerShape(NexaTheme.radius.md)

    // Focus cue (M5): the focused tile lifts slightly and casts a tinted glow, so the eye lands on it
    // from across the room. Animated so the move reads as motion, not a jump; resting tiles stay flat.
    val scale by animateFloatAsState(targetValue = if (focused) 1.05f else 1f, label = "tileScale")
    // The glow spreads ~this far past the tile; the rows that host tiles must leave at least this much
    // padding inside their (clipping) scroll bounds, or the halo is sheared off at the container edge.
    val glow by animateDpAsState(targetValue = if (focused) 16.dp else 0.dp, label = "tileGlow")

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation = glow, shape = shape, ambientColor = colors.focusRing, spotColor = colors.focusRing)
            .clip(shape)
            .background(if (focused || moving) colors.surfaceVariant else colors.surface)
            .border(
                // While "picked up" for reordering a tile keeps an accent ring even without focus,
                // so the item being moved is always visible as the row shuffles around it.
                border = BorderStroke(
                    width = if (focused || moving) 3.dp else 1.dp,
                    color = when {
                        moving -> colors.accent
                        focused -> colors.focusRing
                        else -> colors.surfaceVariant
                    },
                ),
                shape = shape,
            )
            // Short OK launches; long OK (no "Menu" key on the TV remote) opens the context menu.
            .okClickable(interaction = interaction, onClick = onClick, onLongClick = onLongClick)
            .padding(NexaTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(NexaTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The icon takes all the height the label leaves, so a square-ish tile stays balanced whether the
        // name wraps to one line or two — rather than a fixed square that forces the tile taller.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                )
            } else {
                Monogram(label = label)
            }
        }
        Text(
            text = label,
            // A tile label is a *secondary* cue — the icon identifies the app — so it is kept light and
            // quiet (regular weight, muted colour, small). Soft word-wrap over two lines lets a full name
            // like "Play Store" sit whole instead of being cut with an ellipsis on the first line.
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Normal,
            ),
            color = colors.onSurfaceMuted,
            softWrap = true,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        )
    }
}

/**
 * OK-key handling for a tile: a short press is [onClick], a long press is [onLongClick], decided from
 * the key's own event-time because foundation's clickable can't detect a D-pad long-press on TV. Also
 * makes the node focusable so it can receive those key events. No ripple — the focus ring is the cue.
 */
@Composable
private fun Modifier.okClickable(
    interaction: MutableInteractionSource,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
): Modifier {
    val longPressMs = remember { ViewConfiguration.getLongPressTimeout().toLong() }
    var okDownAt by remember { mutableLongStateOf(0L) }
    return onKeyEvent { keyEvent ->
        val native = keyEvent.nativeKeyEvent
        when {
            native.keyCode !in OK_KEYCODES -> false
            native.action == AndroidKeyEvent.ACTION_DOWN -> {
                if (okDownAt == 0L) okDownAt = native.eventTime
                true
            }
            native.action == AndroidKeyEvent.ACTION_UP -> {
                val held = native.eventTime - okDownAt
                okDownAt = 0L
                if (held >= longPressMs) onLongClick() else onClick()
                true
            }
            else -> false
        }
    }.focusable(interactionSource = interaction)
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
