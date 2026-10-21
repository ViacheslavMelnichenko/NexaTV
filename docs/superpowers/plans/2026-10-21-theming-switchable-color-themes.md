# Switchable Color Themes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the owner pick from a catalog of four dark color themes in Settings; the choice applies live and is restored after reboot, without regressing cold-start or switch performance.

**Architecture:** The token layer is already theme-agnostic (a theme = one `NexaColorScheme`). This epic adds: a persisted `theme` field in `SettingsProto`, a domain `ThemeChoice` enum with repository read/write (mirroring `tile_size`), the four palettes + an enum→palette map in `ui/theme`, the theme applied at the activity root (`MainActivity.setContent`, above `LauncherApp`), and a Settings switcher row reusing the tile-size chip pattern. The first frame always draws the synchronous in-memory default (Pastel Dark); a stored non-default recolors in one frame when DataStore emits.

**Tech Stack:** Kotlin, Jetpack Compose-for-TV (`androidx.tv:tv-material3`), Proto DataStore, JUnit4 + mockk + kotlinx-coroutines-test. Device-free build: `./tools/gradle-8.9/bin/gradle.bat <task> --no-daemon`.

**Design source:** [`docs/superpowers/specs/2026-10-21-theming-switchable-color-themes-design.md`](../specs/2026-10-21-theming-switchable-color-themes-design.md) — follow its **8 performance rules**.

---

## File structure

| File | Responsibility | Change |
|---|---|---|
| `app/src/main/proto/launcher_state.proto` | Persisted schema | Add `ThemeId` enum + `SettingsProto.theme = 2` |
| `app/src/main/java/com/nexatv/launcher/data/SettingsRepository.kt` | Settings persistence | Add `ThemeChoice` enum, proto↔domain mapping, `theme` Flow + `setTheme` |
| `app/src/test/java/com/nexatv/launcher/data/SettingsRepositoryTest.kt` | Persistence tests | Add theme mapping/default/round-trip/isolation tests |
| `app/src/main/java/com/nexatv/launcher/ui/theme/Color.kt` | Raw hex tokens | Add per-theme hex constants |
| `app/src/main/java/com/nexatv/launcher/ui/theme/NexaColorScheme.kt` | Palettes | Add `OledBlackColors`, `AmberEmberColors`, `MagentaNoirColors` |
| `app/src/main/java/com/nexatv/launcher/ui/theme/Themes.kt` | Theme→palette map | **Create**: `ThemeChoice.colorScheme()` |
| `app/src/test/java/com/nexatv/launcher/ui/theme/ThemeCatalogTest.kt` | Mapping + contrast guard | **Create** |
| `app/src/main/java/com/nexatv/launcher/ui/home/HomeViewModel.kt` | Home state machine | Surface `theme` in `HomeUiState.Apps`; add `setTheme` |
| `app/src/test/java/com/nexatv/launcher/ui/home/HomeViewModelTest.kt` | ViewModel tests | Stub `settings.theme`; add theme tests |
| `app/src/main/java/com/nexatv/launcher/MainActivity.kt` | Activity root | Read theme, apply via `NexaTheme(colors = …)`; pass to Settings |
| `app/src/main/java/com/nexatv/launcher/ui/settings/SettingsScreen.kt` | Settings UI | Generalize the option chip; add the Theme row |
| `app/src/main/res/values/strings.xml` + `values-uk/strings.xml` | Strings | Theme label + 4 theme names (en/uk) |
| `CHANGELOG.md` | Changelog | `[Unreleased]` entry |

---

## Task 1: Proto schema — ThemeId enum + SettingsProto.theme field

**Files:**
- Modify: `app/src/main/proto/launcher_state.proto`

- [ ] **Step 1: Add the `theme` field to `SettingsProto` and the `ThemeId` enum**

In `app/src/main/proto/launcher_state.proto`, replace the `SettingsProto` message block:

```proto
// User-configurable launcher settings (ADR-0009). Grows additively: new settings are new fields
// with safe defaults, so old files keep deserializing. Tag 1 is the first setting (tile size).
message SettingsProto {
  // How large home tiles are drawn. TILE_SIZE_UNSPECIFIED (the default) means the owner never
  // chose, which the repository resolves to MEDIUM.
  TileSize tile_size = 1;

  // The chosen color theme (Theming EPIC). THEME_ID_UNSPECIFIED (the default) means the owner never
  // chose, which the repository resolves to Pastel Dark. Lives here (not root tag 3) per ADR-0009:
  // all settings belong in SettingsProto; root tag 3 stays reserved forever (ADR-0008, never reuse).
  ThemeId theme = 2;
}

// Color theme identity (Theming EPIC). Semantic ids, not raw colors — the UI maps each to a
// NexaColorScheme, so storage stays color-free. Grows additively; never renumber or reuse a value.
enum ThemeId {
  THEME_ID_UNSPECIFIED = 0;
  THEME_ID_PASTEL_DARK = 1;
  THEME_ID_OLED_BLACK = 2;
  THEME_ID_AMBER_EMBER = 3;
  THEME_ID_MAGENTA_NOIR = 4;
}
```

- [ ] **Step 2: Update the root `reserved` comment** so it no longer claims tag 3 is where the theme lives.

In the same file, replace the reserved comment inside `LauncherStateProto`:

```proto
  // Reserved for upcoming / retired tags so they stay stable across schema growth. Tag 3 was
  // originally earmarked for a root-level theme id; the theme now lives in SettingsProto.theme
  // instead (ADR-0009), so tag 3 stays reserved forever (ADR-0008: never reuse a tag).
  //   2 = category assignments (M4), 3 = retired theme-id slot.
  reserved 2, 3;
```

- [ ] **Step 3: Generate the proto classes by compiling**

Run: `./tools/gradle-8.9/bin/gradle.bat :app:compileDebugKotlin --no-daemon`
Expected: BUILD SUCCESSFUL. This regenerates `ThemeId` and the `SettingsProto.getTheme()/setTheme()` accessors under `build/generated`. (Kotlin still compiles because nothing references the new symbols yet.)

- [ ] **Step 4: Commit**

```bash
git -c user.name="ViacheslavMelnichenko" -c user.email="melnichenkoviatcheslav@gmail.com" \
  add app/src/main/proto/launcher_state.proto
git -c user.name="ViacheslavMelnichenko" -c user.email="melnichenkoviatcheslav@gmail.com" \
  commit -m "feat(proto): add ThemeId enum and SettingsProto.theme field"
```

---

## Task 2: Domain ThemeChoice + repository read/write

**Files:**
- Modify: `app/src/main/java/com/nexatv/launcher/data/SettingsRepository.kt`
- Test: `app/src/test/java/com/nexatv/launcher/data/SettingsRepositoryTest.kt`

- [ ] **Step 1: Write the failing tests**

In `SettingsRepositoryTest.kt`, add these tests inside the class (after the existing tile-size tests). Also add the import near the other proto import:

```kotlin
import com.nexatv.launcher.data.proto.ThemeId as ThemeIdProto
```

```kotlin
    @Test
    fun `theme maps across the domain-proto boundary both ways`() {
        for (choice in ThemeChoice.entries) {
            assertEquals(choice, choice.toProto().toDomain())
        }
        assertEquals(ThemeIdProto.THEME_ID_PASTEL_DARK, ThemeChoice.PASTEL_DARK.toProto())
        assertEquals(ThemeIdProto.THEME_ID_OLED_BLACK, ThemeChoice.OLED_BLACK.toProto())
        assertEquals(ThemeIdProto.THEME_ID_AMBER_EMBER, ThemeChoice.AMBER_EMBER.toProto())
        assertEquals(ThemeIdProto.THEME_ID_MAGENTA_NOIR, ThemeChoice.MAGENTA_NOIR.toProto())
    }

    @Test
    fun `unspecified and unknown themes resolve to the default`() {
        assertEquals(ThemeChoice.Default, ThemeIdProto.THEME_ID_UNSPECIFIED.toDomain())
        assertEquals(ThemeChoice.Default, ThemeIdProto.UNRECOGNIZED.toDomain())
    }

    @Test
    fun `theme defaults to Pastel Dark on an unset store`() = runTest {
        val repository = SettingsRepository(FakeLauncherStore())
        assertEquals(ThemeChoice.PASTEL_DARK, repository.theme.first())
    }

    @Test
    fun `setTheme persists through the store and the flow reflects it`() = runTest {
        val repository = SettingsRepository(FakeLauncherStore())

        repository.setTheme(ThemeChoice.AMBER_EMBER)
        assertEquals(ThemeChoice.AMBER_EMBER, repository.theme.first())

        repository.setTheme(ThemeChoice.MAGENTA_NOIR)
        assertEquals(ThemeChoice.MAGENTA_NOIR, repository.theme.first())
    }

    @Test
    fun `setTheme leaves tile size and favorites untouched`() = runTest {
        val store = FakeLauncherStore()
        val favorites = FavoritesRepository(store)
        val settings = SettingsRepository(store)
        val key = FavoriteKey("pkg/.A", userSerial = 0)

        favorites.toggle(key)
        settings.setTileSize(TileSize.LARGE)
        settings.setTheme(ThemeChoice.OLED_BLACK)

        assertEquals(listOf(key), favorites.favorites.first())
        assertEquals(TileSize.LARGE, settings.tileSize.first())
        assertEquals(ThemeChoice.OLED_BLACK, settings.theme.first())
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./tools/gradle-8.9/bin/gradle.bat :app:testDebugUnitTest --tests "com.nexatv.launcher.data.SettingsRepositoryTest" --no-daemon`
Expected: compile failure / FAIL — `ThemeChoice`, `theme`, `setTheme`, `toProto`/`toDomain` for themes are unresolved.

- [ ] **Step 3: Implement the domain enum, mapping, and repository members**

In `SettingsRepository.kt`, add the proto import alias near the top (beside the existing `TileSize as TileSizeProto`):

```kotlin
import com.nexatv.launcher.data.proto.ThemeId as ThemeIdProto
```

Add the domain enum just below the existing `enum class TileSize { SMALL, MEDIUM, LARGE }`:

```kotlin
/**
 * Color-theme choice (Theming EPIC). A semantic id, not raw colors — the ui/theme layer maps each to a
 * [NexaColorScheme][com.nexatv.launcher.ui.theme.NexaColorScheme], so storage stays color-free (ADR-0009).
 * Android-free so the proto mapping is unit-testable. [Default] is the palette used when the owner never
 * chose (and for any unknown/future stored value).
 */
enum class ThemeChoice {
    PASTEL_DARK,
    OLED_BLACK,
    AMBER_EMBER,
    MAGENTA_NOIR,
    ;

    companion object {
        val Default: ThemeChoice = PASTEL_DARK
    }
}
```

Inside `SettingsRepository`, add the Flow and writer (after the tile-size members):

```kotlin
    /** The chosen color theme; emits again on every change. Defaults to [ThemeChoice.Default]. */
    val theme: Flow<ThemeChoice> =
        dataStore.data.map { it.settings.theme.toDomain() }

    /** Persist the color theme. Writes only the `settings` sub-message, leaving favorites + tile size intact. */
    suspend fun setTheme(choice: ThemeChoice) {
        dataStore.updateData { state ->
            state.toBuilder()
                .setSettings(state.settings.toBuilder().setTheme(choice.toProto()))
                .build()
        }
    }
```

Add the mapping functions at file scope (beside the tile-size `toDomain`/`toProto`):

```kotlin
/**
 * Resolve the persisted proto enum to the domain enum. `THEME_ID_UNSPECIFIED` (owner never chose),
 * an explicit `PASTEL_DARK`, and any unknown/future value all resolve to [ThemeChoice.Default].
 */
internal fun ThemeIdProto.toDomain(): ThemeChoice = when (this) {
    ThemeIdProto.THEME_ID_OLED_BLACK -> ThemeChoice.OLED_BLACK
    ThemeIdProto.THEME_ID_AMBER_EMBER -> ThemeChoice.AMBER_EMBER
    ThemeIdProto.THEME_ID_MAGENTA_NOIR -> ThemeChoice.MAGENTA_NOIR
    else -> ThemeChoice.Default
}

/** Map the domain enum to its persisted proto value. */
internal fun ThemeChoice.toProto(): ThemeIdProto = when (this) {
    ThemeChoice.PASTEL_DARK -> ThemeIdProto.THEME_ID_PASTEL_DARK
    ThemeChoice.OLED_BLACK -> ThemeIdProto.THEME_ID_OLED_BLACK
    ThemeChoice.AMBER_EMBER -> ThemeIdProto.THEME_ID_AMBER_EMBER
    ThemeChoice.MAGENTA_NOIR -> ThemeIdProto.THEME_ID_MAGENTA_NOIR
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./tools/gradle-8.9/bin/gradle.bat :app:testDebugUnitTest --tests "com.nexatv.launcher.data.SettingsRepositoryTest" --no-daemon`
Expected: PASS (all tile-size and theme tests).

- [ ] **Step 5: Commit**

```bash
git -c user.name="ViacheslavMelnichenko" -c user.email="melnichenkoviatcheslav@gmail.com" \
  add app/src/main/java/com/nexatv/launcher/data/SettingsRepository.kt \
      app/src/test/java/com/nexatv/launcher/data/SettingsRepositoryTest.kt
git -c user.name="ViacheslavMelnichenko" -c user.email="melnichenkoviatcheslav@gmail.com" \
  commit -m "feat(settings): persist theme choice via SettingsRepository"
```

---

## Task 3: Palettes + theme→palette map

**Files:**
- Modify: `app/src/main/java/com/nexatv/launcher/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/nexatv/launcher/ui/theme/NexaColorScheme.kt`
- Create: `app/src/main/java/com/nexatv/launcher/ui/theme/Themes.kt`
- Test: `app/src/test/java/com/nexatv/launcher/ui/theme/ThemeCatalogTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/nexatv/launcher/ui/theme/ThemeCatalogTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./tools/gradle-8.9/bin/gradle.bat :app:testDebugUnitTest --tests "com.nexatv.launcher.ui.theme.ThemeCatalogTest" --no-daemon`
Expected: compile failure — `colorScheme()`, `OledBlackColors`, etc. unresolved.

- [ ] **Step 3: Add the raw hex constants**

In `Color.kt`, replace the `NexaColors` object body (keep the existing default tokens; append the per-theme sections):

```kotlin
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
```

- [ ] **Step 4: Add the three palettes**

In `NexaColorScheme.kt`, add after the existing `PastelDarkColors` val (before `LocalNexaColors`):

```kotlin
/** Maximum-contrast dark theme: pure-black surface, pastel accents, deepest backdrop. */
internal val OledBlackColors = NexaColorScheme(
    surface = NexaColors.OledSurface,
    surfaceVariant = NexaColors.OledSurfaceVariant,
    onSurface = NexaColors.OledOnSurface,
    onSurfaceMuted = NexaColors.OnSurfaceMuted,
    accent = NexaColors.Accent,
    onAccent = NexaColors.OledSurface,
    focusRing = NexaColors.Focus,
    success = NexaColors.Success,
    error = NexaColors.Error,
    backdropTop = NexaColors.OledBackdropTop,
    backdropBottom = NexaColors.OledBackdropBottom,
)

/** Warm/cozy dark theme: the default base with an amber accent; error cooled, focus pushed to coral. */
internal val AmberEmberColors = NexaColorScheme(
    surface = NexaColors.Surface,
    surfaceVariant = NexaColors.SurfaceVariant,
    onSurface = NexaColors.OnSurface,
    onSurfaceMuted = NexaColors.OnSurfaceMuted,
    accent = NexaColors.AmberAccent,
    onAccent = NexaColors.Surface,
    focusRing = NexaColors.AmberFocus,
    success = NexaColors.Success,
    error = NexaColors.AmberError,
    backdropTop = NexaColors.BackdropTop,
    backdropBottom = NexaColors.BackdropBottom,
)

/** OLED base with a muted rose-magenta accent; focus ring kept cool-blue to stay distinct from it. */
internal val MagentaNoirColors = NexaColorScheme(
    surface = NexaColors.OledSurface,
    surfaceVariant = NexaColors.OledSurfaceVariant,
    onSurface = NexaColors.OledOnSurface,
    onSurfaceMuted = NexaColors.OnSurfaceMuted,
    accent = NexaColors.MagentaAccent,
    onAccent = NexaColors.OledSurface,
    focusRing = NexaColors.Accent,
    success = NexaColors.Success,
    error = NexaColors.Error,
    backdropTop = NexaColors.OledBackdropTop,
    backdropBottom = NexaColors.OledBackdropBottom,
)
```

- [ ] **Step 5: Create the theme→palette map**

Create `app/src/main/java/com/nexatv/launcher/ui/theme/Themes.kt`:

```kotlin
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
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./tools/gradle-8.9/bin/gradle.bat :app:testDebugUnitTest --tests "com.nexatv.launcher.ui.theme.ThemeCatalogTest" --no-daemon`
Expected: PASS (resolution + contrast guards for all four themes).

> If `ThemeCatalogTest` fails to compile because `androidx.compose.ui.graphics.Color` is unavailable on the unit-test classpath, fall back to reading the raw ARGB via `Color.value.toLong()` bit-shifts instead of `.red/.green/.blue` — the luminance math is unchanged. (Color's sRGB component accessors are pure Kotlin, so this is expected to compile as written.)

- [ ] **Step 7: Commit**

```bash
git -c user.name="ViacheslavMelnichenko" -c user.email="melnichenkoviatcheslav@gmail.com" \
  add app/src/main/java/com/nexatv/launcher/ui/theme/Color.kt \
      app/src/main/java/com/nexatv/launcher/ui/theme/NexaColorScheme.kt \
      app/src/main/java/com/nexatv/launcher/ui/theme/Themes.kt \
      app/src/test/java/com/nexatv/launcher/ui/theme/ThemeCatalogTest.kt
git -c user.name="ViacheslavMelnichenko" -c user.email="melnichenkoviatcheslav@gmail.com" \
  commit -m "feat(theme): add OLED/Amber/Magenta palettes and theme-palette map"
```

---

## Task 4: Surface the theme through HomeViewModel state

**Files:**
- Modify: `app/src/main/java/com/nexatv/launcher/ui/home/HomeViewModel.kt`
- Test: `app/src/test/java/com/nexatv/launcher/ui/home/HomeViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

In `HomeViewModelTest.kt`, add the theme stub to `setUp()` (the ViewModel will collect it in `init`):

```kotlin
        every { settings.theme } returns emptyFlow()
```

Add the imports near the others:

```kotlin
import com.nexatv.launcher.data.ThemeChoice
```

Add these tests inside the class:

```kotlin
    @Test
    fun `theme defaults to the default when the store has not emitted`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        coEvery { repository.loadApps() } returns listOf(mockk<AppEntry>())

        val viewModel = HomeViewModel(repository, homeRole, favorites, settings, mainDispatcherRule.dispatcher)
        advanceUntilIdle()

        assertEquals(ThemeChoice.Default, (viewModel.state.value as HomeUiState.Apps).theme)
    }

    @Test
    fun `the persisted theme is reflected in the Apps state`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        coEvery { repository.loadApps() } returns listOf(mockk<AppEntry>())
        every { settings.theme } returns flowOf(ThemeChoice.MAGENTA_NOIR)

        val viewModel = HomeViewModel(repository, homeRole, favorites, settings, mainDispatcherRule.dispatcher)
        advanceUntilIdle()

        assertEquals(ThemeChoice.MAGENTA_NOIR, (viewModel.state.value as HomeUiState.Apps).theme)
    }

    @Test
    fun `setTheme delegates the choice to the repository`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        coEvery { repository.loadApps() } returns emptyList()

        val viewModel = HomeViewModel(repository, homeRole, favorites, settings, mainDispatcherRule.dispatcher)
        advanceUntilIdle()

        viewModel.setTheme(ThemeChoice.OLED_BLACK)
        advanceUntilIdle()

        coVerify { settings.setTheme(ThemeChoice.OLED_BLACK) }
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./tools/gradle-8.9/bin/gradle.bat :app:testDebugUnitTest --tests "com.nexatv.launcher.ui.home.HomeViewModelTest" --no-daemon`
Expected: FAIL — `HomeUiState.Apps.theme` and `viewModel.setTheme` don't exist; the `settings.theme` stub makes the existing tests pass as before.

- [ ] **Step 3: Implement the state field, collection, and writer**

In `HomeViewModel.kt`, add the import beside the other data imports:

```kotlin
import com.nexatv.launcher.data.ThemeChoice
```

Add `theme` to the `Apps` state (update the data class and its KDoc tail):

```kotlin
    data class Apps(
        val favorites: List<AppEntry>,
        val apps: List<AppEntry>,
        val tileSize: TileSize,
        val theme: ThemeChoice,
    ) : HomeUiState
```

Add the backing field beside `tileSize`:

```kotlin
    /** Latest persisted color theme (Theming EPIC); defaults until the store's first emission. */
    private var theme: ThemeChoice = ThemeChoice.Default
```

Add a collector in `init` (after the tile-size collector):

```kotlin
        // Keep the theme live: changing it in Settings re-emits so the UI reflects the selection.
        viewModelScope.launch {
            settingsRepository.theme.collect { choice ->
                theme = choice
                if (_state.value is HomeUiState.Apps) emitApps()
            }
        }
```

Pass it through `emitApps()`:

```kotlin
            HomeUiState.Apps(
                favorites = resolveFavorites(cachedApps, favoriteKeys),
                apps = cachedApps,
                tileSize = tileSize,
                theme = theme,
            )
```

Add the writer beside `setTileSize`:

```kotlin
    /** Persist the color theme (Theming EPIC); the palette applies live via the theme Flow. */
    fun setTheme(choice: ThemeChoice) {
        viewModelScope.launch { settingsRepository.setTheme(choice) }
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./tools/gradle-8.9/bin/gradle.bat :app:testDebugUnitTest --tests "com.nexatv.launcher.ui.home.HomeViewModelTest" --no-daemon`
Expected: PASS (all existing + three new theme tests).

- [ ] **Step 5: Commit**

```bash
git -c user.name="ViacheslavMelnichenko" -c user.email="melnichenkoviatcheslav@gmail.com" \
  add app/src/main/java/com/nexatv/launcher/ui/home/HomeViewModel.kt \
      app/src/test/java/com/nexatv/launcher/ui/home/HomeViewModelTest.kt
git -c user.name="ViacheslavMelnichenko" -c user.email="melnichenkoviatcheslav@gmail.com" \
  commit -m "feat(home): surface theme choice through HomeViewModel state"
```

---

## Task 5: Apply the persisted theme at the activity root

**Files:**
- Modify: `app/src/main/java/com/nexatv/launcher/MainActivity.kt`

- [ ] **Step 1: Add imports**

In `MainActivity.kt`, add beside the existing data/theme imports:

```kotlin
import com.nexatv.launcher.data.ThemeChoice
import com.nexatv.launcher.ui.theme.colorScheme
```

- [ ] **Step 2: Read the theme in `setContent` and apply it**

Replace `onCreate`:

```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current.applicationContext
            val settings = remember { settingsRepository(context) }
            // Resolve the theme one level above LauncherApp. The first frame draws the synchronous
            // in-memory default (Pastel Dark) — the theme read stays off the cold-start critical path
            // (performance rules 2-4). A stored default produces no swap; a stored non-default recolors
            // in a single frame (colors only, no relayout) when DataStore emits.
            val theme by settings.theme.collectAsStateWithLifecycle(initialValue = ThemeChoice.Default)
            NexaTheme(colors = theme.colorScheme()) {
                LauncherApp()
            }
        }
    }
```

- [ ] **Step 3: Pass the theme into the Settings route**

In `LauncherApp`, replace the `HomeDestination.SETTINGS` branch's `SettingsScreen(...)` call:

```kotlin
            SettingsScreen(
                tileSize = appsState?.tileSize ?: TileSize.MEDIUM,
                onSelectTileSize = viewModel::setTileSize,
                theme = appsState?.theme ?: ThemeChoice.Default,
                onSelectTheme = viewModel::setTheme,
            )
```

- [ ] **Step 4: Verify it compiles**

Run: `./tools/gradle-8.9/bin/gradle.bat :app:compileDebugKotlin --no-daemon`
Expected: FAIL — `SettingsScreen` does not yet accept `theme`/`onSelectTheme` (wired in Task 6). This is an expected intermediate state; proceed to Task 6, then compile.

- [ ] **Step 5: Commit** (after Task 6 compiles — see Task 6 Step 6). Staged together with Task 6 so the tree never holds a non-compiling commit.

---

## Task 6: Settings Theme switcher row

**Files:**
- Modify: `app/src/main/java/com/nexatv/launcher/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-uk/strings.xml`

- [ ] **Step 1: Add the strings (English)**

In `app/src/main/res/values/strings.xml`, inside the `<!-- Settings screen … -->` group (after `settings_tile_size_large`):

```xml
    <string name="settings_theme">Theme</string>
    <string name="settings_theme_pastel_dark">Pastel Dark</string>
    <string name="settings_theme_oled_black">OLED Black</string>
    <string name="settings_theme_amber_ember">Amber Ember</string>
    <string name="settings_theme_magenta_noir">Magenta Noir</string>
```

- [ ] **Step 2: Add the strings (Ukrainian)**

In `app/src/main/res/values-uk/strings.xml`, in the same group (after `settings_tile_size_large`):

```xml
    <string name="settings_theme">Тема</string>
    <string name="settings_theme_pastel_dark">Пастельна</string>
    <string name="settings_theme_oled_black">OLED чорна</string>
    <string name="settings_theme_amber_ember">Бурштин</string>
    <string name="settings_theme_magenta_noir">Маджента</string>
```

- [ ] **Step 3: Generalize the option chip**

In `SettingsScreen.kt`, rename the private `TileSizeOption` composable to the theme-agnostic `SettingOptionChip` (body unchanged — it already takes `label`/`selected`/`onClick`/`modifier`). Replace the function header and its KDoc:

```kotlin
/**
 * One selectable option chip, shared by every settings row. The focus ring is the navigation cue; a
 * persistent accent border marks the currently-selected value even without focus, so the active choice
 * is always visible. No ripple.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
```

Update the call inside `TileSizeRow` to use the new name:

```kotlin
                SettingOptionChip(
                    label = stringResource(size.labelRes()),
                    selected = size == selected,
                    onClick = { onSelect(size) },
                    modifier = if (index == 0) Modifier.focusRequester(firstOption) else Modifier,
                )
```

- [ ] **Step 4: Add the imports, the screen parameters, the Theme row, and its labels**

Add the import beside `com.nexatv.launcher.data.TileSize`:

```kotlin
import com.nexatv.launcher.data.ThemeChoice
```

Extend `SettingsScreen`'s signature and body. Replace the function header through the `Column` content:

```kotlin
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    tileSize: TileSize,
    onSelectTileSize: (TileSize) -> Unit,
    theme: ThemeChoice,
    onSelectTheme: (ThemeChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = NexaTheme.spacing.safeAreaHorizontal,
                    vertical = NexaTheme.spacing.screenMargin,
                ),
            verticalArrangement = Arrangement.spacedBy(NexaTheme.spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.displayLarge,
                color = NexaTheme.colors.onSurface,
            )
            TileSizeRow(selected = tileSize, onSelect = onSelectTileSize)
            ThemeRow(selected = theme, onSelect = onSelectTheme)
        }
    }
}
```

Add the `ThemeRow` composable (after `TileSizeRow`). It deliberately has **no** focus-requester, so entering Settings still lands focus on the first tile-size option (FR-006):

```kotlin
/** The "Theme" setting: a label above the selectable color-theme options that write on select. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ThemeRow(selected: ThemeChoice, onSelect: (ThemeChoice) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(NexaTheme.spacing.sm)) {
        SectionHeader(title = stringResource(R.string.settings_theme))
        Row(horizontalArrangement = Arrangement.spacedBy(NexaTheme.spacing.md)) {
            ThemeChoice.entries.forEach { choice ->
                SettingOptionChip(
                    label = stringResource(choice.labelRes()),
                    selected = choice == selected,
                    onClick = { onSelect(choice) },
                )
            }
        }
    }
}
```

Add the label mapping (beside `TileSize.labelRes()`):

```kotlin
/** The display label for a color theme. */
private fun ThemeChoice.labelRes(): Int = when (this) {
    ThemeChoice.PASTEL_DARK -> R.string.settings_theme_pastel_dark
    ThemeChoice.OLED_BLACK -> R.string.settings_theme_oled_black
    ThemeChoice.AMBER_EMBER -> R.string.settings_theme_amber_ember
    ThemeChoice.MAGENTA_NOIR -> R.string.settings_theme_magenta_noir
}
```

- [ ] **Step 5: Compile + lint + full test + detekt (the device-free CI lane)**

Run: `./tools/gradle-8.9/bin/gradle.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest :app:detekt --no-daemon`
Expected: BUILD SUCCESSFUL; all unit tests pass; lint and detekt clean. (If `lintDebug` flags a missing `values-uk` key or vice-versa, re-check Steps 1-2 for parity.)

- [ ] **Step 6: Commit Tasks 5 + 6 together**

```bash
git -c user.name="ViacheslavMelnichenko" -c user.email="melnichenkoviatcheslav@gmail.com" \
  add app/src/main/java/com/nexatv/launcher/MainActivity.kt \
      app/src/main/java/com/nexatv/launcher/ui/settings/SettingsScreen.kt \
      app/src/main/res/values/strings.xml \
      app/src/main/res/values-uk/strings.xml
git -c user.name="ViacheslavMelnichenko" -c user.email="melnichenkoviatcheslav@gmail.com" \
  commit -m "feat(settings): apply the persisted theme and add the Theme switcher row"
```

---

## Task 7: Changelog, spec finalize, cleanup

**Files:**
- Modify: `CHANGELOG.md`
- Modify: `docs/superpowers/specs/2026-10-21-theming-switchable-color-themes-design.md`
- Delete: `theme-preview.html` (untracked scratch file)

- [ ] **Step 1: Add the changelog entry**

In `CHANGELOG.md`, under `## [Unreleased]` → `### Added`, prepend:

```markdown
- Switchable color themes (Theming EPIC): a Settings "Theme" row picks from four dark palettes
  (Pastel Dark, OLED Black, Amber Ember, Magenta Noir). The choice persists via `SettingsProto.theme`
  (ADR-0008/0009) and applies live — the palette is resolved at the activity root, so the first frame
  always draws the default and a stored non-default recolors in one frame (no relayout, no cold-start
  cost). Design: `docs/superpowers/specs/2026-10-21-theming-switchable-color-themes-design.md`.
```

- [ ] **Step 2: Mark the spec as implemented**

In the spec, change the `**Status:**` line to:

```markdown
**Status:** Implemented (2026-10-21) — see `docs/superpowers/plans/2026-10-21-theming-switchable-color-themes.md`
```

- [ ] **Step 3: Delete the scratch preview file**

Run: `rm -f theme-preview.html`
Expected: no output. (It was never tracked; `git status` should show nothing to commit for it.)

- [ ] **Step 4: Final device-free CI lane (safety re-run)**

Run: `./tools/gradle-8.9/bin/gradle.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest :app:detekt --no-daemon`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git -c user.name="ViacheslavMelnichenko" -c user.email="melnichenkoviatcheslav@gmail.com" \
  add CHANGELOG.md docs/superpowers/specs/2026-10-21-theming-switchable-color-themes-design.md
git -c user.name="ViacheslavMelnichenko" -c user.email="melnichenkoviatcheslav@gmail.com" \
  commit -m "docs(theming): changelog + mark theming spec implemented"
```

---

## Owner-dependent (not code; after merge)

- **On-device manual check (performance rules 8 + NFR-018):** switch across all four themes live and
  confirm the one-frame recolor with no relayout; reboot and confirm the chosen theme is restored;
  verify **Amber Ember** and **Magenta Noir** accent-vs-error distinction at 10 ft; run `StartupBenchmark`
  with a stored **default** theme and confirm it is unchanged (NFR-001 p50 ≤ 2.0 s) with zero swap on start.

---

## Self-review notes

- **Spec coverage:** Persistence (Task 1-2), catalog + map (Task 3, all four palettes incl. Magenta Noir),
  activity wiring (Task 5), switcher UI + strings (Task 6), tests — repository/mapping/contrast/ViewModel
  (Tasks 2-4), performance rules honoured (static CompositionLocal unchanged; sync default first frame;
  lazy fail-soft read with `initialValue`; no blocking read; no cross-tree animation; `backdropBrush` stays
  behind `remember(colors)` in `HomeScreen`, untouched; new theme = one val + one branch). On-device checks
  are owner tasks, listed above.
- **Type consistency:** `ThemeChoice` {PASTEL_DARK, OLED_BLACK, AMBER_EMBER, MAGENTA_NOIR} + `ThemeChoice.Default`;
  `ThemeId` proto {UNSPECIFIED=0, PASTEL_DARK=1, OLED_BLACK=2, AMBER_EMBER=3, MAGENTA_NOIR=4}; `colorScheme()`,
  `setTheme`, `theme` used identically across tasks. `SettingOptionChip` is the renamed `TileSizeOption`, used
  by both rows.
- **No placeholders:** every code step shows full code; every run step shows the exact command + expected result.
