# Android TV — Device Audit & Launcher Model (TCL)

> **Status:** ◐ Read-only device audit largely complete (ADB, 2026-10-20) — identity, ABI, SoC/GPU,
> display/refresh, HOME environment and TV inputs are `FACT`-confirmed. Remaining: full `pm list packages`
> capture and the §3 launcher-model behaviors (reboot / crash-recovery), which require active on-device
> verification. **No pairing codes / IPs / ports are recorded here** — those are runtime data and must
> never be committed.

## 1. Device identity (seeded, to be ADB-confirmed)

| Field | Value | Source |
|-------|-------|--------|
| Manufacturer | TCL | FACT (initial.md) |
| Android TV OS | 14 | FACT (initial.md) |
| Security patch | 2025-12-05 | FACT (initial.md) |
| Kernel | 5.15.180-android14-11 | FACT (initial.md) |
| Build | tcl9618-user 14 UTT2.250416.001 AU02 release-keys | FACT (initial.md) |
| Product information | V8-0012T01-LF1V643.003175 | FACT (initial.md) |
| Model | Smart TV Pro | FACT (ADB 2026-10-20) |
| Product / device codename | G08_4K_GB / G08 | FACT (ADB) |
| SDK level | 34 | FACT (ADB) |
| Build fingerprint | TCL/G08_4K_GB/G08:14/UTT2.250416.001/AU02:user/release-keys | FACT (ADB) |
| CPU ABI | **armeabi-v7a** (abilist: armeabi-v7a, armeabi) — **32-bit ARM only, no arm64** | FACT (ADB) |
| CPU cores | 4 | FACT (ADB) |
| RAM | 2,451,284 kB (~2.34 GiB total) | FACT (ADB `/proc/meminfo`) |
| Resolution | Panel 3840x2160 (4K); **UI override 1920x1080** | FACT (ADB `wm size`) |
| Density | 320 dpi (xhdpi) app @1080p UI; physical 640 dpi @4K | FACT (ADB `wm density` / `dumpsys display`) |
| Refresh rate | **60 Hz, fixed** — single display mode `3840x2160@60`; UI override `1920x1080@60`; `mRefreshRateChangeable=false` → **frame budget 16.67 ms** | FACT (ADB `dumpsys display` 2026-10-20) |
| SoC | **MediaTek MT9689** (`ro.soc.model`); platform `merak`; `ro.hardware=mt5896`; product board `tcl9618` | FACT (ADB `getprop`) |
| CPU core | **Cortex-A73** (`dalvik.vm.isa.arm.variant`), 32-bit `armeabi-v7a` | FACT (ADB `getprop`) |
| GPU | **ARM Mali-G52**, OpenGL ES 3.2 (driver `r44p1`), EGL 1.5; ASTC + `EXT_shader_framebuffer_fetch` supported | FACT (ADB `dumpsys SurfaceFlinger` 2026-10-20) |

## 2. Launcher / HOME environment (to be ADB-confirmed)

| Field | Value | Source |
|-------|-------|--------|
| Current default launcher (package/activity) | `com.google.android.apps.tv.launcherx` → `…home.HomeActivity` (Google TV "LauncherX") | FACT (ADB `resolve-activity`) |
| Available HOME activities | 3: (1) `com.google.android.apps.tv.launcherx/.home.HomeActivity` (active); (2) `com.google.android.tungsten.setupwraith/.RecoveryActivity` (setup/recovery); (3) `com.android.tv.settings/.system.FallbackHome` (**system fallback HOME**) | FACT (ADB `pm query-activities … HOME`) |
| Launchable apps | 48 via `LAUNCHER`, **54 via `LEANBACK_LAUNCHER`** (TV apps) | FACT (ADB `query-activities`) |
| Relevant TV system features | `android.software.leanback`, `leanback_only`, `android.hardware.type.television`, `android.hardware.tv.tuner`, `live_tv` | FACT (ADB `pm list features`) |
| TV input services | **7** (`android.media.tv.TvInputService`): `com.mediatek.AirplayAPK/.AirPlayTvInputService` (AirPlay); `com.tcl.channelplus/…InputService` (IPTV); `com.tcl.tvinput/.TvPassThroughService`; `com.tcl.tvinput/.TunerInputService`; Google Videos `…tvinput.playback.TvInputService` + `…VideoPreviewInputService` | FACT (ADB `cmd package query-services` 2026-10-20) |
| Installed launcher packages | _see HOME activities above; full `pm list packages` not yet captured_ | ADB to confirm |

## 3. Android launcher model (research notes — `initial.md` §17)

To document (verified on the actual TCL where noted):
- HOME intent; `MAIN` + `HOME` + `DEFAULT` categories
- What makes an Activity a launcher Activity
- How Android / Google TV chooses the HOME application
- Behavior when multiple HOME applications exist
- Behavior after reboot
- Behavior when the custom launcher crashes, and the recovery path

> Do not assume "set an APK as HOME" behaves identically across firmware. Verify on the TCL.

## 4. Safety notes

Daily-use production device. Early phase is **read-only**. Do not disable the stock launcher, uninstall
system apps, or modify system partitions. Keep Settings reachable at all times. First NexaTV install must
be reversible. See `initial.md` §16.

**Recovery insight (FACT):** the system exposes `com.android.tv.settings/.system.FallbackHome` as a HOME
activity. This is the natural fallback if a custom launcher is ever set as default and misbehaves — but
during early development we will NOT change the default HOME at all; we launch NexaTV explicitly instead.

**Build implication (FACT):** the device is **32-bit ARM only** (`armeabi-v7a`, no `arm64-v8a`). Any APK
we build must ship a `armeabi-v7a` (or universal) binary; an arm64-only build will not install/run.

**GPU / translucency implication (FACT → INFERENCE):** the GPU is an **ARM Mali-G52** (OpenGL ES 3.2),
a low-to-mid-range part — capable of `RenderEffect` backdrop blur in principle, but at a 60 Hz / 16.67 ms
frame budget on an always-resident launcher it must be **measured, not assumed** (ADR-0005: "fake glass"
default; real-time blur stays behind the on-device fps/PSS gate). This upgrades ADR-0005's earlier
"older 32-bit GPU, exact part UNKNOWN" to a known device — the measurement gate decision is unchanged.
