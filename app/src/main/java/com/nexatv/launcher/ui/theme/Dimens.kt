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

import androidx.compose.ui.unit.dp

/**
 * NexaTV design tokens — spacing and radii on an 8dp grid.
 * TV-safe screen padding (overscan margin) lives here too.
 */
internal object NexaSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp

    /** Safe inset from top/bottom screen edges to clear TV overscan (FR-012, ~5% @1080p). */
    val screenMargin = 48.dp

    /** Safe inset from left/right screen edges — wider, per FR-012 (~64 dp @1080p). */
    val safeAreaHorizontal = 64.dp
}

internal object NexaRadius {
    val sm = 8.dp
    val md = 16.dp
}
