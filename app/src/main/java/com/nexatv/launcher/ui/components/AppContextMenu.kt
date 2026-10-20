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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
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
import com.nexatv.launcher.ui.theme.NexaTheme

/**
 * The long-press OK context menu (ADR-0008). No "Menu" key exists on the TV remote, so a long OK on a
 * tile opens this: pin/unpin, optionally move, open App info, or cancel. Short OK always launches, so
 * this is the only non-destructive way to act on an item without opening it.
 *
 * [onMove] is supplied only where reordering makes sense (the Home favorites row); when it is null the
 * "Move" row is hidden, so the same menu serves the All-apps page where there is nothing to reorder.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun AppContextMenu(
    app: AppEntry,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAppInfo: () -> Unit,
    onDismiss: () -> Unit,
    onMove: (() -> Unit)? = null,
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
                // "Move" reorders within the Favorites row, so it only applies to a pinned app and only
                // where a reorder handler was given.
                if (onMove != null && isFavorite) MenuAction(stringResource(R.string.home_menu_move), onMove)
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
