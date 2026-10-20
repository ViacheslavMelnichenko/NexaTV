# Changelog

All notable changes to NexaTV are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versioning follows [SemVer](https://semver.org/) (ADR-0003 D5).

## [Unreleased]

### Added
- Project foundation (M1): Gradle setup, version catalog, wrapper 8.9, and the
  `:app` module (`com.nexatv.launcher`, SDK 34) with a Compose-for-TV scaffold —
  HOME + LEANBACK_LAUNCHER manifest, design tokens, en/uk strings, placeholder Home.
- CI gate (`.github/workflows/ci.yml`): assemble, Android Lint, unit tests, detekt,
  ABI assert, APK-size budget, gitleaks secret scan (ADR-0003 D1).
- Design System: semantic color roles (`NexaColorScheme`) behind `NexaTheme`, spacing/radius
  grid, readable type scale (body ≥18sp), and core components (`SectionHeader`, focusable
  `AppTile`) with previews. Debug-only Design System gallery renders every token + component.
  Documented in `docs/design/DESIGN-SYSTEM.md`.
