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

import com.nexatv.launcher.data.ThemeChoice

/**
 * Resolve a domain [ThemeChoice] to its concrete palette. The theme→palette map lives *only* here, so
 * the data layer stays color-free (ADR-0009) and adding a theme is one branch plus one [NexaColorScheme]
 * val. The `when` is exhaustive with no `else`, so a new [ThemeChoice] forces a compile error here until
 * its palette is wired — the type-safe mapping the design calls for. Each branch returns a pre-built
 * singleton, so this allocates nothing on the hot path (performance rule 7).
 */
internal fun ThemeChoice.colorScheme(): NexaColorScheme = when (this) {
    ThemeChoice.PASTEL_DARK -> PastelDarkColors
    ThemeChoice.OLED_BLACK -> OledBlackColors
    ThemeChoice.AMBER_EMBER -> AmberEmberColors
    ThemeChoice.MAGENTA_NOIR -> MagentaNoirColors
}
