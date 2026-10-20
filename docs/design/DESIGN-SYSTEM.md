# NexaTV Design System

> Status: **living** · Scope: visual foundation for every NexaTV screen · Owner: whoever touches UI next.
> This describes *decisions and behaviour*, not the code line-by-line. The code is the source of the exact
> values; this page is the source of the *rules*.

## Why a design system comes before screens

Screens that hardcode a color or a size cannot be re-themed and drift apart over time. So the token layer
and the first shared components exist **before** the launcher UI (M2). Two hard rules follow from this:

1. **Screens never hardcode color, spacing, or radius.** They read tokens through `NexaTheme`.
2. **A theme is one object.** Adding a theme later means supplying another `NexaColorScheme` instance —
   no screen changes. This is what makes the later *theme-switch* step a drop-in. See
   [`ADR`s and the Theming epic in the backlog](../../project/BACKLOG.md).

## Architecture

```
NexaTheme { … }                      // composable: installs a palette + typography
  └─ CompositionLocalProvider(LocalNexaColors provides colors)
       └─ MaterialTheme(tv-material)  // tv-material components get derived defaults

NexaTheme.colors   -> NexaColorScheme  (the active palette, via CompositionLocal)
NexaTheme.spacing  -> NexaSpacing       (dp grid)
NexaTheme.radius   -> NexaRadius        (corner radii)
```

- `NexaColorScheme` (`ui/theme/NexaColorScheme.kt`) is the **semantic palette** — roles, not raw hex.
- `NexaTheme` (`ui/theme/Theme.kt`) is both the installer composable **and** the token accessor object,
  mirroring the `MaterialTheme` pattern (`NexaTheme.colors`, `NexaTheme.spacing`, `NexaTheme.radius`).
- Raw hex lives only in `ui/theme/Color.kt` and is referenced **only** by palette definitions.

## Tokens

### Color roles (`NexaColorScheme`)

| Role             | Meaning / usage                                          |
|------------------|----------------------------------------------------------|
| `surface`        | Base screen background (dark TV-room surface).            |
| `surfaceVariant` | Raised/lifted surfaces, focused tile background.         |
| `onSurface`      | Primary text/icon on surfaces.                           |
| `onSurfaceMuted` | Secondary text, captions, de-emphasised labels.          |
| `accent`         | Brand/active accent (primary tv-material color).         |
| `onAccent`       | Content drawn on top of `accent`.                        |
| `focusRing`      | The D-pad focus ring — the single focus affordance.      |
| `success`        | Positive status.                                         |
| `error`          | Error status (maps to tv-material `error`).              |

Default theme: **Pastel Dark** (`PastelDarkColors`) — calm pastel accents on a dark surface (NFR-016).

### Spacing (`NexaSpacing`, dp grid)

`xs 4` · `sm 8` · `md 16` · `lg 24` · `xl 48` · `screenMargin 48`

TV screens need generous margins (overscan + 10-foot viewing); `screenMargin` is the standard edge padding.

### Radius (`NexaRadius`)

`sm 8` · `md 16` · `lg 24`

### Typography (`NexaTypography`, tv-material `Typography`)

Overridden slots guarantee readable body text at 10 feet (NFR-016, **body ≥ 18sp**):
`bodyLarge 20sp` · `bodyMedium 18sp` · `labelLarge 18sp`. All other slots keep tv-material defaults.

## Components

Located in `ui/components/`. Each is `internal`, token-driven, and ships with `@Preview`s.

### `SectionHeader(title)`

A row/section title (`headlineMedium`, `onSurface`). Used to label groups of tiles.

### `AppTile(label, onClick)`

The focusable building block of the launcher.

- **Focus treatment is the contract:** when focused it raises to `surfaceVariant` and draws a 3dp
  `focusRing` border; unfocused it sits on `surface` with a 1dp `surfaceVariant` hairline. The ring **is**
  the affordance — `indication = null` disables the Material ripple (wrong idiom for a D-pad).
- Focus is tracked via the `clickable` `MutableInteractionSource` (`collectIsFocusedAsState`), so DPAD
  focus/click come from one source of truth.
- Size is passed by the caller (via `modifier`), because tile dimensions are a layout concern of each
  screen, not of the component.

## Home layout & remote interaction (M4)

Owner decisions (2026-10-20), binding for the Home screen and every per-item action:

- **Favorites live in their own row.** The Home screen is a **"Обране" (Favorites) row** above the full
  **"Усі застосунки"** grid (Google TV / FLauncher idiom). Favorites are always within reach and initial
  focus lands in that row; the separation is explicit rather than pinned-first-in-grid. Favorites are an
  *ordered* set (ADR-0008) and render in the user's pin/reorder order.
- **The remote has no "Menu" button, so long-press OK is the context-action trigger** — this is the single
  interaction model for *all* per-item actions across the launcher, not just favorites:
  - **Short OK = the primary action** (launch the app). Never repurpose short OK for anything else — it is
    the one gesture that must always just launch.
  - **Long-press OK (≈0.5 s) = a small context menu** over the focused tile: **Закріпити/Відкріпити**
    (pin/unpin), **Перемістити** (reorder — enters a move mode), **Інформація** (app info), **Скасувати**.
    The menu is the extensible home for future per-item actions (hide, uninstall, …).
- **Why a menu, not an instant toggle:** it is self-evident (nothing hidden behind an undiscoverable
  gesture), it scales to more actions, and it keeps reorder reachable without a separate entry point. The
  cost is one extra step to pin; accepted for discoverability.

Recorded here (not a separate ADR) because it is a design-system interaction rule; the persistence it relies
on is [ADR-0008](../adr/ADR-0008-persistence-proto-datastore.md).

## The Design System gallery (debug-only)

`DesignSystemGalleryActivity` (in **`src/debug`**, so it is absent from release) renders every token and
component on one scrollable screen — color swatches, the type scale, and a focusable row of `AppTile`s.
In a debug build it appears as a separate TV entry **“NexaTV Design System.”** Use it to eyeball the system
on the real TV and to catch visual regressions before they reach a screen.

## Rules of use (checklist for any new UI)

- [ ] No raw hex, dp, or sp literals in a screen — read `NexaTheme.colors/spacing/radius`.
- [ ] New reusable visual element → a component in `ui/components/` with a `@Preview`, added to the gallery.
- [ ] Focusable elements show the `focusRing` focus treatment; no Material ripple on TV.
- [ ] Body/reading text stays ≥ 18sp.
- [ ] New theme → a new `NexaColorScheme` instance only; never edit a screen to re-color it.
