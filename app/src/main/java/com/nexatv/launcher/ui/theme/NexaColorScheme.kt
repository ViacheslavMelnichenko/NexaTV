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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Semantic color roles for NexaTV. A [NexaColorScheme] instance *is* a theme's palette —
 * adding a new theme later means providing another instance, nothing else changes.
 * Screens read these via [NexaTheme.colors]; they must never reference raw palette values.
 */
@Immutable
data class NexaColorScheme(
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val focusRing: Color,
    val success: Color,
    val error: Color,
    val backdropTop: Color,
    val backdropBottom: Color,
)

/** The cinematic Home backdrop: a vertical wash from the softly lifted top to the deep bottom. */
internal fun NexaColorScheme.backdropBrush(): Brush =
    Brush.verticalGradient(listOf(backdropTop, backdropBottom))

/** Default theme: calm pastel on a dark TV-room surface (NFR-016). */
internal val PastelDarkColors = NexaColorScheme(
    surface = NexaColors.Surface,
    surfaceVariant = NexaColors.SurfaceVariant,
    onSurface = NexaColors.OnSurface,
    onSurfaceMuted = NexaColors.OnSurfaceMuted,
    accent = NexaColors.Accent,
    onAccent = NexaColors.Surface,
    focusRing = NexaColors.Focus,
    success = NexaColors.Success,
    error = NexaColors.Error,
    backdropTop = NexaColors.BackdropTop,
    backdropBottom = NexaColors.BackdropBottom,
)

/** Active palette, provided by [NexaTheme]. Static: swapping it re-composes consumers. */
internal val LocalNexaColors = staticCompositionLocalOf { PastelDarkColors }
