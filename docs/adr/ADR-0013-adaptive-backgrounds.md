# ADR-0013 — Optional adaptive backgrounds and an isolated preview app

Date: 2026-10-21. Status: Implemented for preview; real-TV performance acceptance pending.

## Context

The user approved an optional landscape that reflects time and weather without sacrificing the
launcher's responsiveness. Existing installations should retain their chosen theme. Visual review
must be possible before merging and without replacing the owner's working launcher.

## Decision

- Bundle 16 WebP variants of one fixed river-city composition: four periods × four weather groups.
  Keep artwork below 5 MiB (covered by a JVM catalog test); no runtime image fetch or generation.
  Prompt provenance is recorded in `docs/design/BACKGROUND-ARTWORK.md`.
- Add Theme / Automatic / Manual modes, three blur choices, dimming and optional rain/snow motion.
  Use an additive optional Proto message; absence preserves Theme for regular builds. Preview defaults
  to Automatic with soft blur and motion off. Manual selection changes only the scenery.
- Share weather through a lifecycle-subscribed ViewModel. Refresh every 30 minutes while Home is
  resumed, retry failures every 5 minutes, reject data older than 3 hours. No worker/boot receiver.
  Extend the existing Open-Meteo request with epoch sunrise/sunset; no new network service or SDK.
  Wrocław remains the configured city. Period calculation uses Europe/Warsaw and valid same-day solar
  times. Offline fallback is cloudy with 06:30 / 18:30 solar anchors; time advances independently.
- Decode off-main, capped at 1920 px width. Soft/strong blur is calculated once at 480 px width.
  Retain at most two displayed images; crossfade the incoming image over an opaque outgoing image
  for 4 seconds. Stop motion and discard displayed images when Home is not resumed.
- Optional precipitation uses 36 particles in an isolated draw layer at 20 Hz. It is disabled by
  default and respects system animation disabling. No video, WebView, per-frame blur or moving trees.
- Add a `preview` build type: `.preview` package, separate app label/data, ordinary launcher entry,
  no HOME intent and no accessibility override service. Preview bypasses the Home-role gate only.
- Build downloadable, disposable-debug-signed preview artifacts on `codex/**` branch pushes.
  Promote a successful branch artifact to a GitHub prerelease with an annotated `vX.Y.Z-preview.N`
  tag on that exact commit. The APK's version name matches the tag without `v`; it retains the
  separate Preview package and debug signing identity. Publish its checksum and installation guide.
  The production signing job skips `-preview.` tags, and its stable-version comparison excludes
  prereleases so a standalone Preview cannot block the next production release. Main remains PR-only.

## Tradeoffs and validation

AI weather edits preserve the main geometry but are not pixel-perfect relighting; soft blur reduces
minor differences during transitions. Snow cover is an artistic condition cue, not a snow-depth model.
The moon is decorative rather than an astronomical simulation. Seasons and custom cities are deferred.
Weather cache is process-local; after process death offline startup uses the neutral fallback.

JVM tests cover scene boundaries, weather mapping/freshness, asset availability/size, subscription
polling, preview gating and settings migration/round-trip. CI covers compilation, lint, tests,
analysis and APK size/ABI. Actual rendering, D-pad behavior, memory and frame timing require the TCL;
see `docs/testing/ADAPTIVE-BACKGROUND-PREVIEW.md`. No performance result is claimed before measurement.
