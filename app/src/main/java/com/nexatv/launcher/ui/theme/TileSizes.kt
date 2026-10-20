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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexatv.launcher.data.TileSize

/**
 * The drawn dimensions of a home tile (ADR-0009). [width] is both the grid cell's adaptive minimum —
 * so smaller tiles pack more columns onto a 1920-wide TV — and the fixed favorites-row tile width;
 * [height] is the tile height in both places. The single place S/M/L becomes concrete dp, so "how big
 * is Large" is one edit and a future custom-size control can map through the same token.
 */
@Immutable
internal data class TileDimensions(val width: Dp, val height: Dp)

/** Resolve a semantic [TileSize] to its drawn dimensions. The S/M/L → dp map lives only here. */
internal fun TileSize.dimensions(): TileDimensions = when (this) {
    TileSize.SMALL -> TileDimensions(width = 80.dp, height = 100.dp)
    TileSize.MEDIUM -> TileDimensions(width = 96.dp, height = 120.dp)
    TileSize.LARGE -> TileDimensions(width = 128.dp, height = 156.dp)
}
