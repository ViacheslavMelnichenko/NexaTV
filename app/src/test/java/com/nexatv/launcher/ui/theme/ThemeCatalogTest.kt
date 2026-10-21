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
import com.nexatv.launcher.data.ThemeChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * JVM guards for the theme catalog (no device). Every [ThemeChoice] must resolve to a palette, the
 * default must be Pastel Dark, and each palette must clear NFR-018 contrast (body ≥ 4.5:1; accent and
 * focus ring ≥ 3:1 on their own surface) so a future palette can't silently regress legibility.
 */
class ThemeCatalogTest {

    @Test
    fun `every theme choice resolves to a palette and the default is Pastel Dark`() {
        for (choice in ThemeChoice.entries) {
            // Resolution is exhaustive and non-null by construction; call it to lock that in.
            choice.colorScheme()
        }
        assertEquals(PastelDarkColors, ThemeChoice.Default.colorScheme())
        assertEquals(PastelDarkColors, ThemeChoice.PASTEL_DARK.colorScheme())
    }

    @Test
    fun `every palette clears the NFR-018 contrast floors`() {
        for (choice in ThemeChoice.entries) {
            val p = choice.colorScheme()
            assertTrue(
                "onSurface/surface for $choice",
                contrastRatio(p.onSurface, p.surface) >= BODY_MIN,
            )
            assertTrue(
                "accent/surface for $choice",
                contrastRatio(p.accent, p.surface) >= LARGE_MIN,
            )
            assertTrue(
                "focusRing/surface for $choice",
                contrastRatio(p.focusRing, p.surface) >= LARGE_MIN,
            )
        }
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val la = a.relativeLuminance()
        val lb = b.relativeLuminance()
        return (max(la, lb) + CONTRAST_OFFSET) / (min(la, lb) + CONTRAST_OFFSET)
    }

    // WCAG 2.x relative luminance from sRGB components (pure math; no Android framework).
    private fun Color.relativeLuminance(): Double =
        R_COEFF * linearize(red) + G_COEFF * linearize(green) + B_COEFF * linearize(blue)

    private fun linearize(channel: Float): Double {
        val c = channel.toDouble()
        return if (c <= SRGB_KNEE) c / SRGB_LINEAR_DIV else ((c + SRGB_OFFSET) / SRGB_SCALE).pow(SRGB_GAMMA)
    }

    private companion object {
        const val BODY_MIN = 4.5
        const val LARGE_MIN = 3.0
        const val CONTRAST_OFFSET = 0.05
        const val R_COEFF = 0.2126
        const val G_COEFF = 0.7152
        const val B_COEFF = 0.0722
        const val SRGB_KNEE = 0.03928
        const val SRGB_LINEAR_DIV = 12.92
        const val SRGB_OFFSET = 0.055
        const val SRGB_SCALE = 1.055
        const val SRGB_GAMMA = 2.4
    }
}
