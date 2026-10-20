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

## Precise baselines (FACT — Macrobenchmark + dumpsys, 2026-10-20)

Measured on the **real TCL** (`StartupTimingMetric`, `StartupMode.COLD`, `CompilationMode.None()` — i.e.
**no Baseline Profile, worst case**; 10 iterations; `app:benchmark` = non-debuggable, release-like).

**Device under test:** TCL `G08` / model `Smart TV Pro`, Android 14 (SDK 34), **4 CPU cores @ 1.39 GHz
(locked)**, **2.5 GB RAM**. Low-end TV silicon — so these numbers are a conservative floor; real SoCs in
the target range will not be faster than this.

| NFR | Metric | Result | Budget | Verdict |
|-----|--------|--------|--------|---------|
| **NFR-001** | Cold start `timeToInitialDisplayMs` | **min 937.9 / median 984.4 / max 1346.8 ms** | ≤ 2.0 s p50 | ✓ **PASS** (median ≈ 0.98 s, worst-case max well under budget) |
| **NFR-002** | Idle PSS (`dumpsys meminfo`, TOTAL PSS) | **≈ 48.6 MB** (48 640 KB; Java 3.3 / Native 10.7 / Code 25.3 MB) | *(baseline — no M1 budget set yet)* | Recorded |

Per-iteration cold-start runs (ms): 1346.8, 949.2, 946.2, 1088.9, 973.5, 937.9, 1053.7, 995.3 (Macrobenchmark
drops warmup iterations from the reported set). Perfetto traces are kept under the spike's
`macrobenchmark/build/outputs/…` for this pass only (throwaway).

## Caveats

- The 833 ms figure above is `am start -W`, **not** Macrobenchmark — superseded by the median **984 ms**
  here. Both are **no-Baseline-Profile** numbers; a minified + profiled M1 build should be faster still.
- Idle PSS (48.6 MB) is a **single post-launch idle snapshot** of the *spike* (3×4 tv-material grid), not
  the M1 launcher. It sets the order of magnitude, not a contractual budget.
- **NFR-017** (glass surface fps / PSS gate) is **not** covered here — it needs the actual glass UI and a
  frame-timing pass in M1+.

## Decision

ADR-0002 is **validated on the real device**, now with **precise NFR-001 / NFR-002 baselines** above. The
stack is cleared for M1 (Foundation). No stack change. The Macrobenchmark pass is **complete**; the
throwaway spike APK can be uninstalled from the TCL at the owner's discretion.
