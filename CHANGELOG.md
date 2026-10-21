# Changelog

All notable changes to NexaTV are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versioning follows [SemVer](https://semver.org/) (ADR-0003 D5).

## [Unreleased]

### Added
- Release pipeline (ADR-0003): tag-driven `.github/workflows/release.yml` builds, signs (env-fed
  keystore from the protected `release` Environment), asserts ABI + 20 MB size budget, verifies the
  signature, and publishes a GitHub Release with SHA-256 + cert fingerprint + git SHA. Gradle gains an
  env-fed release `signingConfig` and tag-derived `versionName`/`versionCode`. Owner setup in
  `docs/technical/RELEASE-SIGNING.md`; manual on-device gate in `docs/testing/RELEASE-CHECKLIST.md`.
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

### Verified
- M2 on the real TCL (2026-10-20): first reversible install, grid renders real icons, D-pad
  moves one cell per press, OK launches the focused app, `HOME` returns to the stock launcher.
  Recorded in `docs/technical/ANDROID-TV.md` §3a.
