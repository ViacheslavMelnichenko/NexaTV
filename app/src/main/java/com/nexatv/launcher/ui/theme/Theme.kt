/*
 * Copyright 2026 Viacheslav Melnichenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.nexatv.launcher.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/**
 * NexaTV Compose-for-TV theme. Wraps the design tokens (color/type) into a single
 * [MaterialTheme] that all screens consume. The launcher is dark-only by design.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun NexaTvTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = NexaColors.Accent,
        onPrimary = NexaColors.Surface,
        secondary = NexaColors.Focus,
        background = NexaColors.Surface,
        onBackground = NexaColors.OnSurface,
        surface = NexaColors.SurfaceVariant,
        onSurface = NexaColors.OnSurface,
        error = NexaColors.Error,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NexaTypography,
        content = content,
    )
}
