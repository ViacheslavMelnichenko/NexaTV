# Changelog

All notable changes to NexaTV are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versioning follows [SemVer](https://semver.org/) (ADR-0003 D5).

## [Unreleased]

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
