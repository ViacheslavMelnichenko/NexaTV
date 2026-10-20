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
    val Surface = Color(0xFF0F1419)
    val SurfaceVariant = Color(0xFF1B2430)
    val OnSurface = Color(0xFFE6EAF0)
    val OnSurfaceMuted = Color(0xFF9AA6B2)
    val Accent = Color(0xFF8EC5FF)
    val Focus = Color(0xFFBFA2FF)
    val Success = Color(0xFFA6E3B5)
    val Error = Color(0xFFF2A9A9)
}
