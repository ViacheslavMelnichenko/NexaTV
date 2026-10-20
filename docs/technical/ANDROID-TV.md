# Android TV — Device Audit & Launcher Model (TCL)

> **Status:** ☐ Partially seeded from `initial.md` §2 (non-runtime facts only). All `ADB-confirmed`
> fields are pending the read-only device audit. **No pairing codes / IPs / ports are recorded here** —
> those are runtime data and must never be committed.

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
| Density | 320 dpi (xhdpi) | FACT (ADB `wm density`) |
| Refresh rate | _UNKNOWN_ | ADB to confirm (`dumpsys display`) |

## 2. Launcher / HOME environment (to be ADB-confirmed)

| Field | Value | Source |
|-------|-------|--------|
| Current default launcher (package/activity) | `com.google.android.apps.tv.launcherx` → `…home.HomeActivity` (Google TV "LauncherX") | FACT (ADB `resolve-activity`) |
| Available HOME activities | 3: (1) `com.google.android.apps.tv.launcherx/.home.HomeActivity` (active); (2) `com.google.android.tungsten.setupwraith/.RecoveryActivity` (setup/recovery); (3) `com.android.tv.settings/.system.FallbackHome` (**system fallback HOME**) | FACT (ADB `pm query-activities … HOME`) |
| Launchable apps | 48 via `LAUNCHER`, **54 via `LEANBACK_LAUNCHER`** (TV apps) | FACT (ADB `query-activities`) |
| Relevant TV system features | `android.software.leanback`, `leanback_only`, `android.hardware.type.television`, `android.hardware.tv.tuner`, `live_tv` | FACT (ADB `pm list features`) |
| TV input services | _UNKNOWN_ | ADB to confirm |
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
