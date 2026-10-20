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
default launch intent. When NexaTV is itself the active HOME, `pressHome()` surfaces it — watch that
cold-start iterations still force-stop between runs (Macrobenchmark handles this for `StartupMode.COLD`).
