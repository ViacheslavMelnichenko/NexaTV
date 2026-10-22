# Changelog

All notable changes to NexaTV are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versioning follows [SemVer](https://semver.org/) (ADR-0003 D5).

## [Unreleased]

## [0.3.1] - 2026-10-22

### Fixed
- The Home calendar now refreshes its highlighted day and month at midnight instead of retaining the
  date from the first composition.
- App discovery now keeps the same launcher activity from separate Android profiles distinct.

### Changed
- Weather, dimming and build-version labels are localized through Android string resources.
- App-tile labels now use the shared TV-readable typography token rather than a component-local size.

## [0.3.0] - 2026-10-21

Adaptive backgrounds reach the main NexaTV launcher. The Home screen now renders a
time-of-day and weather-aware scene behind the grid; the standalone **NexaTV Preview**
app remains available for isolated on-device testing.

### Added
- Sixteen bundled landscape scenes: dawn, day, sunset and night in clear, cloudy, rainy and snowy weather.
- Automatic and manual scene selection, four-second transitions, blur and dimming controls.
- Optional subtle rain and snow, disabled by default and paused while Home is hidden.
- Shared, lifecycle-bound weather updates with sunrise/sunset and stale-observation handling.

### Validation
- CI covers compilation, JVM tests, Android Lint and detekt for both the main and Preview builds.
- Actual TCL visual review, D-pad responsiveness and memory/frame measurements remain pending.

## [0.3.0-preview.1] - 2026-10-21

First adaptive-background prerelease, distributed as the standalone **NexaTV Preview** app
(`com.nexatv.launcher.preview`). It has separate settings and favorites, opens from the TV apps
menu, and does not register as HOME or include the accessibility override service.

### Added
- Sixteen bundled landscape scenes: dawn, day, sunset and night in clear, cloudy, rainy and snowy weather.
- Automatic and manual scene selection, four-second transitions, blur and dimming controls.
- Optional subtle rain and snow, disabled by default and paused while Home is hidden.
- Shared, lifecycle-bound weather updates with sunrise/sunset and stale-observation handling.
- Downloadable Preview APK with checksum and installation guide.

### Validation
- Preview and ordinary debug CI cover compilation, JVM tests, Android Lint and detekt.
- Preview packaging checks size, ABI and absence of HOME/accessibility registration.
- Actual TCL visual review, D-pad responsiveness and memory/frame measurements remain pending.

## [0.2.1] - 2026-10-21

### Fixed
- HOME now returns to the Home grid from the All apps and Settings sub-screens. The HOME override
  (ADR-0007) only reorders the singleTask `MainActivity` to the front, which preserved the open
  sub-screen; the override's re-raise intent now carries `ACTION_SHOW_HOME`, and `onNewIntent` resets
  the nav state to the Home route so HOME always lands home.

## [0.2.0] - 2026-10-21

### Changed
- Settings redesign: each setting is now a compact card row — its name on the left, smaller
  option chips (18sp) on the right — so the screen reads like a real TV settings menu instead of
  loose oversized blocks. The screen sits on the base surface so the cards lift off it, and
  unselected chips keep a muted outline for visibility.

### Added
- A faint build version (`NexaTV v<version>`) pinned to the bottom-right of Settings, read from
  `BuildConfig.VERSION_NAME` (BuildConfig enabled for this one field).

## [0.1.0] - 2026-10-21

First tagged, signed release. Sideloaded on the real TCL (Android TV OS 14, `armeabi-v7a`).

### Added
- Switchable color themes (Theming EPIC): a Settings "Theme" row picks from four dark palettes
  (Pastel Dark, OLED Black, Amber Ember, Magenta Noir). The choice persists via `SettingsProto.theme`
  (ADR-0008/0009) and applies live — the palette is resolved at the activity root, so the first frame
  always draws the default and a stored non-default recolors in one frame (no relayout, no cold-start
  cost).
- Release pipeline (ADR-0003): tag-driven `.github/workflows/release.yml` builds, signs (env-fed
  keystore from the protected `release` Environment), asserts ABI + 20 MB size budget, verifies the
  signature, and publishes a GitHub Release with SHA-256 + cert fingerprint + git SHA. Gradle gains an
  env-fed release `signingConfig` and tag-derived `versionName`/`versionCode`. Owner setup (keystore
  secrets, on-device gate) lives alongside the workflow in `.github/workflows/release.yml`.
- Project foundation (M1): Gradle setup, version catalog, wrapper 8.9, and the
  `:app` module (`com.nexatv.launcher`, SDK 34) with a Compose-for-TV scaffold —
  HOME + LEANBACK_LAUNCHER manifest, design tokens, en/uk strings, placeholder Home.
- CI gate (`.github/workflows/ci.yml`): assemble, Android Lint, unit tests, detekt,
  ABI assert, APK-size budget, gitleaks secret scan (ADR-0003 D1).
- Design System: semantic color roles (`NexaColorScheme`) behind `NexaTheme`, spacing/radius
  grid, readable type scale (body ≥18sp), and core components (`SectionHeader`, focusable
  `AppTile`) with previews. Debug-only Design System gallery renders every token + component.
  Documented in `docs/design/DESIGN-SYSTEM.md`.
- Minimal launcher (M2): app discovery via `LauncherApps` across profiles (ADR-0004, no
  `QUERY_ALL_PACKAGES`), a D-pad-navigable app grid that launches on OK, canonical tiles with
  icon + monogram fallback, safe-area insets, and Loading/Empty/launch-error states.
- `Modifier.focusRestorer` on the Home grid so focus returns to the first tile when coming
  back from a launched app.
- HOME via accessibility override (M3, ADR-0007): the launcher binds as an accessibility service to
  act as HOME on Google TV, which ignores third-party `ROLE_HOME`. Mechanism recorded in
  `docs/technical/ANDROID-TV.md`.
- Favorites + persistence (ADR-0008): a curated favorites row backed by Proto DataStore
  (`SettingsProto`), surviving reinstalls and cold starts.
- All-apps screen: a full D-pad-navigable grid of every launchable app, reachable from Home.
- Calendar and weather widgets (ADR-0010): a date/calendar widget and a network-backed weather
  widget on Home.
- Tile-size setting: a Settings row to choose the Home tile size, persisted via `SettingsProto`.

### Verified
- M2 on the real TCL (2026-10-20): first reversible install, grid renders real icons, D-pad
  moves one cell per press, OK launches the focused app, `HOME` returns to the stock launcher.
  Recorded in `docs/technical/ANDROID-TV.md` §3a.
