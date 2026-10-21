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

import androidx.compose.ui.graphics.Color

/**
 * NexaTV design tokens — color (NFR-016: calm pastel palette, dark surface for a TV room).
 * These are the single source of truth for color; screens must not hardcode hex values.
 */
internal object NexaColors {
    // --- Default: Pastel Dark (NFR-016) ---
    val Surface = Color(0xFF0F1419)
    val SurfaceVariant = Color(0xFF1B2430)
    // Cinematic backdrop endpoints: a softly lifted, blue-tinted top fading to a deep near-black
    // bottom, so the room's dark "letterbox" frames the content without a flat single tone.
    val BackdropTop = Color(0xFF17202C)
    val BackdropBottom = Color(0xFF080B0F)
    val OnSurface = Color(0xFFE6EAF0)
    val OnSurfaceMuted = Color(0xFF9AA6B2)
    val Accent = Color(0xFF8EC5FF)
    val Focus = Color(0xFFBFA2FF)
    val Success = Color(0xFFA6E3B5)
    val Error = Color(0xFFF2A9A9)

    // --- OLED True-Black: pure-black surface + deepest backdrop (shared by Magenta Noir). ---
    val OledSurface = Color(0xFF000000)
    val OledSurfaceVariant = Color(0xFF11161D)
    val OledOnSurface = Color(0xFFECEFF4)
    val OledBackdropTop = Color(0xFF0A0E14)
    val OledBackdropBottom = Color(0xFF000000)

    // --- Amber Ember: warm accent on the default dark base; error cooled + focus pushed to coral
    //     so the three warm hues stay distinguishable at 10 ft. ---
    val AmberAccent = Color(0xFFFFC68E)
    val AmberFocus = Color(0xFFFF9E8E)
    val AmberError = Color(0xFFF09393)

    // --- Magenta Noir: muted, noble rose-magenta accent (owner-approved 2026-10-21, desaturated from
    //     the #FF5EB4 neon pass). The focus ring reuses the pastel-blue Accent so the nav cue never
    //     collides with the magenta accent on the selected tile. ---
    val MagentaAccent = Color(0xFFE692C0)
}
