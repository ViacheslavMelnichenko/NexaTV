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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/**
 * NexaTV Compose-for-TV theme. A [colors] instance selects the active palette — swapping it
 * (future theme-switch step) re-composes every consumer. Screens read tokens through the
 * [NexaTheme] accessor object, never from raw palette values.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun NexaTheme(
    colors: NexaColorScheme = PastelDarkColors,
    content: @Composable () -> Unit,
) {
    // Feed tv-material components sensible defaults derived from our semantic roles.
    val material = darkColorScheme(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        secondary = colors.focusRing,
        background = colors.surface,
        onBackground = colors.onSurface,
        surface = colors.surfaceVariant,
        onSurface = colors.onSurface,
        error = colors.error,
    )
    CompositionLocalProvider(LocalNexaColors provides colors) {
        MaterialTheme(
            colorScheme = material,
            typography = NexaTypography,
            content = content,
        )
    }
}

/** Token accessor, mirroring the `MaterialTheme` pattern: `NexaTheme.colors`, `NexaTheme.spacing`. */
internal object NexaTheme {
    val colors: NexaColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalNexaColors.current

    val spacing: NexaSpacing get() = NexaSpacing
    val radius: NexaRadius get() = NexaRadius
}
