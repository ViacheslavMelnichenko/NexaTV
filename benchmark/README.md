# `:benchmark` — performance harness (device-gated)

This module measures `:app` on the **real TCL**; it never ships in a release and has no JVM/CI
lane. Everything here needs a connected device (ADR-0002 target: Android TV OS 14 / API 34).

It does two jobs:

- **Macrobenchmark** (`StartupBenchmark`) — cold-start time (NFR-001) with and without the Baseline
  Profile, plus frame timing while traversing the Home grid with the D-pad.
- **Baseline Profile** (`BaselineProfileGenerator`) — records the hot startup path so it is
  AOT-compiled on first run instead of JIT-warmed.

## Why it is scaffolded but not yet run

The code compiles in the normal build, but the numbers and the generated profile only exist after a
run **on the device**. Per the project rules, on-device verification is an owner step — so this is
wired and buildable, and the actual run is deferred until the TCL is attached.

## Running it (when the TCL is connected)

```bash
# Generate app/src/main/baseline-prof.txt from the device, then it is bundled automatically.
./tools/gradle-8.9/bin/gradle.bat :app:generateBaselineProfile --no-daemon

# Startup + frame-timing numbers (results print to the build output / are saved as JSON).
./tools/gradle-8.9/bin/gradle.bat :benchmark:connectedBenchmarkAndroidTest --no-daemon
```

## Notes specific to a launcher

NexaTV registers only `HOME` / `LEANBACK_LAUNCHER` intent-filters (no plain `LAUNCHER`), so the
benchmarks launch `MainActivity` by explicit component (`LauncherTarget.launchHome`) rather than the
default launch intent.

Because NexaTV is itself the active HOME, a cold launch needs the launcher to first be off the
foreground or `am start` reports "already running" and nothing is measured. So each iteration first
surfaces system Settings (`LauncherTarget.leaveHome`, not timed) so the measured `launchHome()` is a
genuine, confirmable foreground transition. `MainActivity` also calls `reportFullyDrawn()` (via
`ReportDrawnWhen`) once the grid replaces the initial Loading state, so `timeToFullDisplay` reflects a
usable Home, not just the first empty frame.

## Status (2026-10-20): harness works, automated install does not

On the dev TCL the installed HOME is the **debug** build `com.nexatv.launcher.debug`, while the
benchmark measures the **release** app id `com.nexatv.launcher`. The `com.android.test` connected task
(`connectedBenchmarkReleaseAndroidTest`) installs the *test* APK but **does not install the app under
test** — it even removes a pre-installed one — so every run fails with `Unable to confirm activity
launch completion []` (the target package is simply absent at `am start`).

This is an install-wiring gap, **not** a performance or launcher-code problem. Verified by hand: with
`com.nexatv.launcher` installed (`adb install app/build/outputs/apk/benchmarkRelease/app-benchmarkRelease.apk`),
`am start -W -n com.nexatv.launcher/.MainActivity` after `leaveHome` + `force-stop` gives a clean
`LaunchState: COLD`, **TotalTime ≈ 477 ms** — so the launch path and the two fixes above are correct.

To get the full before/after numbers later, either install the benchmarkRelease app before the run
(and keep the test from uninstalling it) or set a matching HOME/app-id so the connected task installs
the measured app. Left as a separate follow-up; the harness itself is complete and committed.
