# Spike result — ADR-0002 stack validation (Kotlin + Jetpack Compose-for-TV)

> **Status:** ☑ **PASS** (2026-10-20). This records the outcome of the Phase 0 → M1 validation gate
> for [ADR-0002](../adr/ADR-0002-flutter-vs-compose.md). The spike **code is throwaway** (built under
> `spike/compose-tv-hello/`, git-ignored — never part of history); only this evidence is kept.

## Purpose

Before committing to M1 (Foundation) on the ADR-0002 stack, prove on the **real TCL** that Kotlin +
Jetpack Compose-for-TV: (a) builds, (b) installs and runs on the device's **32-bit `armeabi-v7a`** ABI,
(c) gives working, visible **D-pad focus**, and (d) cold-starts within the NFR-001 budget.

## Toolchain used (FACT, 2026-10-20)

| Component | Version |
|-----------|---------|
| JDK | Temurin 17.0.19 |
| Android SDK | platform `android-34`, build-tools `34.0.0`, platform-tools `37.0.1` |
| Gradle | 8.9 (checksum-verified against the official SHA-256) |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 1.9.24 (Compose compiler extension 1.5.14) |
| Compose BOM | 2024.06.00 |
| `androidx.tv:tv-material` | 1.0.0 (stable) |

Spike app: one `ComponentActivity` → `setContent` → tv-material `Surface` + a 3×4 grid of focusable
tv-material `Button`s, initial focus on the first tile. Declared `LEANBACK_LAUNCHER` only (**not** HOME —
Phase 0 discipline: the launcher role is untouched).

## Results (FACT — from the device)

| Check | Result |
|-------|--------|
| Compile + `assembleRelease` | ✓ BUILD SUCCESSFUL |
| APK native code | `primaryCpuAbi=null`, `mergeReleaseNativeLibs NO-SOURCE` → **no `.so`; ABI-agnostic**, installs on `armeabi-v7a` |
| APK size | ~14 MB (debug-signed, non-minified, no Baseline Profile) |
| Install on TCL | ✓ `adb install -r` → `Success` (reversible; HOME role unchanged) |
| Launch / foreground | ✓ `mCurrentFocus = com.nexatv.spike/.MainActivity` |
| Cold start (rough) | `am start -W` → `LaunchState: COLD`, `TotalTime: 833 ms` — comfortably within NFR-001 (≤2.0 s p50) |
| D-pad focus on screen | ✓ **Owner-confirmed on the TV**: tile grid visible, focus indicator visible, focus moves predictably with the physical remote |

## Caveats

- 833 ms is `am start -W`, **not** Macrobenchmark, on a debug-signed, non-minified build with **no
  Baseline Profile** — treat it as a ceiling, not the NFR-001 baseline. A minified + profiled build will
  be faster.
- Precise **NFR-001** (cold-start p50/p95), **NFR-002** (idle PSS) and the **NFR-017** glass fps/PSS gate
  still require on-device **Macrobenchmark** — deferred to a dedicated measurement pass.

## Decision

ADR-0002 is **validated on the real device**. The stack is cleared for M1 (Foundation). No stack change.
The throwaway spike is kept installed only as long as useful for the Macrobenchmark pass, then removed.
