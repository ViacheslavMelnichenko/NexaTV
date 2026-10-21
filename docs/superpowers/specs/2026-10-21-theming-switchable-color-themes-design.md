# Design — NexaTV switchable color themes (persisted)

**Date:** 2026-10-21
**Status:** Approved (brainstorm) — ready for an implementation plan
**Implements:** the **Theming** EPIC in [`project/BACKLOG.md`](../../project/BACKLOG.md) (owner directive 2026-10-20:
easy theme switching that survives restart). Extends [ADR-0009](../../adr/ADR-0009-configuration-architecture.md)
(configuration architecture) and [ADR-0008](../../adr/ADR-0008-persistence-proto-datastore.md) (Proto DataStore),
built on the theme-agnostic token layer from the Design System (NFR-016, `docs/design/DESIGN-SYSTEM.md`).

## Goal

Let the owner pick from a small catalog of color themes in Settings. The choice applies **live** and is
**restored after reboot**. The token layer is already theme-agnostic (a theme = one `NexaColorScheme`
instance), so this epic adds: a theme catalog, a persisted `theme` setting, the enum→palette mapping, the
Settings switcher row, and the activity wiring — **without regressing cold-start or switch performance**
(explicit owner constraint).

## Non-goals

- **No wallpaper / background image** selection (owner decision 2026-10-21). Each theme already supplies its
  own backdrop via the two-stop `backdropTop→backdropBottom` wash; user-selectable images are a separate epic
  (asset pipeline + memory/GPU cost on the 32-bit device) and stay deferred.
- **No light/daytime theme** in this cut — evaluated and rejected: a large bright surface at 10 ft in a dark
  TV room causes glare, and pastel accents lose their identity on a light base. May be revisited later.
- No per-element custom colors, no theme import/export, no dynamic/wallpaper-derived palettes.
- No change to the token *contract* (semantic roles) or to any screen's color usage — screens keep reading
  `NexaTheme.colors` only.

## Theme catalog (first cut — 3 dark themes)

All three are dark (zero glare risk) and each differs on a **different axis** so the switcher feels
meaningful. Contrast verified against NFR-018 (body ≥ 4.5:1; large text / focus ring ≥ 3:1) on the worst
rendered backdrop. Each theme is a complete set of the 11 existing `NexaColorScheme` roles.

### 1. Pastel Dark — default, unchanged

The reference identity (calm, blue-cool cinematic). Values stay exactly as `PastelDarkColors` today.

| Role | Value |
|---|---|
| surface | `#0F1419` |
| surfaceVariant | `#1B2430` |
| onSurface | `#E6EAF0` |
| onSurfaceMuted | `#9AA6B2` |
| accent | `#8EC5FF` |
| onAccent | `#0F1419` |
| focusRing | `#BFA2FF` |
| success | `#A6E3B5` |
| error | `#F2A9A9` |
| backdropTop | `#17202C` |
| backdropBottom | `#080B0F` |

Legibility: onSurface≈15:1, muted≈6.7:1 — clear pass.

### 2. OLED True-Black

Maximum contrast, deepest black; power/burn-in friendly on OLED. Same pastel accents, floor dropped to pure
black. On the TCL LCD it simply reads as a very dark Pastel Dark (no harm).

| Role | Value |
|---|---|
| surface | `#000000` |
| surfaceVariant | `#11161D` |
| onSurface | `#ECEFF4` |
| onSurfaceMuted | `#9AA6B2` |
| accent | `#8EC5FF` |
| onAccent | `#000000` |
| focusRing | `#BFA2FF` |
| success | `#A6E3B5` |
| error | `#F2A9A9` |
| backdropTop | `#0A0E14` |
| backdropBottom | `#000000` |

Legibility: onSurface≈18:1 (best in catalog). The unfocused tile hairline (`surfaceVariant` on pure black) is
subtle by design — the focus affordance is the 3dp ring + lift, not the surface delta (DESIGN-SYSTEM §AppTile).

### 3. Amber Ember

Same dark base, warm/cozy amber accent instead of cool blue — the cheapest "feels different" theme (hue-only).

| Role | Value |
|---|---|
| surface | `#0F1419` |
| surfaceVariant | `#1B2430` |
| onSurface | `#E6EAF0` |
| onSurfaceMuted | `#9AA6B2` |
| accent | `#FFC68E` |
| onAccent | `#0F1419` |
| focusRing | `#FF9E8E` |
| success | `#A6E3B5` |
| error | `#F09393` |
| backdropTop | `#17202C` |
| backdropBottom | `#080B0F` |

Legibility: amber is high-luminance → dark `onAccent` passes easily; accent-on-surface clears the 3:1 ring/large
floor. `error` is cooled to `#F09393` and `focusRing` pushed to coral so the three warm hues (accent / focus /
error) stay distinguishable at 10 ft — **verify the accent-vs-error distinction on-device**.

> Deferred, trivial drop-ins later (same pattern): **Evergreen** (teal accent `#7FD8C9`), a Light theme.

## Components

### A. Persistence & state model (extends ADR-0009)

- **Proto:** add a field to `SettingsProto`, not the root reserved tag 3. ADR-0009 established that all
  settings live in `SettingsProto`; ADR-0008 forbids reusing a tag. Root `reserved 2, 3` stays untouched
  (tag 3 reserved forever).
  ```proto
  message SettingsProto {
    TileSize tile_size = 1;
    ThemeId  theme = 2;            // default THEME_ID_UNSPECIFIED -> Pastel Dark
  }

  enum ThemeId {
    THEME_ID_UNSPECIFIED = 0;      // owner never chose -> Pastel Dark
    THEME_ID_PASTEL_DARK = 1;
    THEME_ID_OLED_BLACK  = 2;
    THEME_ID_AMBER_EMBER = 3;
    // future palettes: 4, 5, … (additive, never renumber)
  }
  ```
- **Type:** a proto enum (mirrors `TileSize`), not a free string — type-safe exhaustive mapping, additive
  growth, matches the one proven pattern in `SettingsRepository`. Old `.pb` files deserialize to
  `UNSPECIFIED` with no migration.
- **Repository seam** (`SettingsRepository`, mirror `tile_size` exactly):
  - `val theme: Flow<ThemeChoice> = dataStore.data.map { it.settings.theme.toDomain() }`
  - `suspend fun setTheme(choice: ThemeChoice)` → `updateData { it.toBuilder().setSettings(it.settings
    .toBuilder().setTheme(choice.toProto())) }` (writes only the `settings` sub-message; favorites &
    tile_size untouched).
  - Add internal `ThemeId.toDomain()` / `ThemeChoice.toProto()` beside the tile-size mappings;
    `UNSPECIFIED`/unknown → default.

### B. Theme catalog & enum→palette mapping (ui/theme)

- Add `OledBlackColors` and `AmberEmberColors` as top-level `val NexaColorScheme` instances next to
  `PastelDarkColors` in `NexaColorScheme.kt` (new hex constants in `Color.kt`, referenced only by palettes).
- A single source-of-truth map in the theme/ui layer resolves a domain `ThemeChoice` → `NexaColorScheme`
  (keeps storage layout/color-free per ADR-0009). `ThemeChoice` is the domain enum the repository exposes;
  the default/unknown entry resolves to `PastelDarkColors`.

### C. Activity wiring (MainActivity.setContent)

- Currently `setContent { NexaTheme { LauncherApp() } }`. Change to resolve the theme one level above
  `LauncherApp`: collect the theme flow with `collectAsStateWithLifecycle(initialValue = ThemeChoice.default)`
  and pass `NexaTheme(colors = NexaThemes[theme]) { LauncherApp() }`.
- First frame renders the synchronous in-memory default; when DataStore emits, a non-default theme recolors in
  one frame (colors only, **no relayout**). A stored default theme (typical) produces **no swap at all**.

### D. Settings switcher UI

- Add a **"Theme"** row to `SettingsScreen` above/below the existing tile-size row, reusing the exact
  `TileSizeRow`/`TileSizeOption` pattern (a `SectionHeader` + a `Row` of selectable chips that write on
  select, no Save button). Options come from the `ThemeChoice` entries; the selected one gets the persistent
  accent border, focus gets the ring.
- Selecting writes through `onSelectTheme(choice)` → `setTheme` → the theme Flow updates `NexaTheme` live.
- New strings: `settings_theme` + one label per theme (en/uk), following the existing
  `settings_tile_size_*` keys.

## Data flow

```
Settings chip select ─▶ onSelectTheme(choice) ─▶ SettingsRepository.setTheme ─▶ DataStore (settings.theme)
                                                                                      │
MainActivity: theme Flow .collectAsStateWithLifecycle(initial = default)  ◀──────────┘
   └─▶ NexaTheme(colors = NexaThemes[choice]) ─▶ staticCompositionLocalOf swap ─▶ whole UI recomposes once
cold start: first frame = PastelDark default (sync); stored default ⇒ no swap; non-default ⇒ 1-frame recolor
```

## Performance rules (owner's hard constraint — the implementation MUST follow)

1. Keep `LocalNexaColors` as `staticCompositionLocalOf`; read colors only via `NexaTheme.colors`. Rare switch
   ⇒ a one-time full recomposition is the right trade; token reads stay zero-overhead on every frame/start.
2. First frame draws the synchronous in-memory default (`PastelDarkColors`). Read the DataStore theme lazy &
   fail-soft with `initialValue = default` (same posture as the weather widget).
3. **No** `runBlocking` / blocking DataStore read on the main thread in `onCreate`/before `setContent`; the
   theme read stays off the critical cold-start render path.
4. Never gate `NexaTheme` on a `Loading` state for the theme — never delay the first frame to read a palette.
5. **No cross-tree color animation** — the palette swap is instant (one frame). At most one cheap root-level
   `Crossfade` ≤ ~150 ms, and that is **off by default**.
6. Any `Brush`/derived-from-palette object stays behind `remember(colors)` (as `HomeScreen` already does for
   the backdrop); never build brushes/objects in a hot path per recomposition.
7. A new theme is only another top-level `val NexaColorScheme` — no new CompositionLocal, no consumer-side
   branching.
8. After the feature, run the `StartupBenchmark` (NFR-001 p50 ≤ 2.0 s) and frame-timing on a switch; a stored
   **default** theme must produce **zero** swap on start.

## Testing

- **Repository (JVM, no device — ADR-0008 pattern):** default (empty store) resolves to Pastel Dark;
  `setTheme` round-trips each `ThemeChoice`; writing theme leaves favorites + tile_size intact; unknown/future
  enum value → default.
- **Mapping:** every `ThemeChoice` maps to a non-null `NexaColorScheme`; `UNSPECIFIED`→Pastel Dark.
- **Contrast guard (lightweight):** a unit test asserting each palette's onSurface/surface contrast ≥ 4.5:1
  and accent/focus ≥ 3:1 (cheap luminance math) so a future palette can't silently regress NFR-018.
- **On-device (owner, manual):** switch across all three themes live; confirm the one-frame recolor (no
  relayout); reboot restores the chosen theme; **Amber Ember accent-vs-error distinction** verified at 10 ft;
  StartupBenchmark unchanged with a stored default.

## Out of scope / deferred

- Wallpaper/background-image selection (separate epic). Light theme and Evergreen teal accent (trivial later
  drop-ins). Per-element custom colors; theme import/export; dynamic/wallpaper-derived palettes.
