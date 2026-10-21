# Android TV — Device Audit & Launcher Model (TCL)

> **Status:** ◐ Read-only device audit largely complete (ADB, 2026-10-20) — identity, ABI, SoC/GPU,
> display/refresh, HOME environment and TV inputs are `FACT`-confirmed. Remaining: full `pm list packages`
> capture and the §3 launcher-model behaviors (reboot / crash-recovery), which require active on-device
> verification. **No pairing codes / IPs / ports are recorded here** — those are runtime data and must
> never be committed.

## 1. Device identity (seeded, to be ADB-confirmed)

| Field | Value | Source |
|-------|-------|--------|
| Manufacturer | TCL | FACT (seed) |
| Android TV OS | 14 | FACT (seed) |
| Security patch | 2025-12-05 | FACT (seed) |
| Kernel | 5.15.180-android14-11 | FACT (seed) |
| Build | tcl9618-user 14 UTT2.250416.001 AU02 release-keys | FACT (seed) |
| Product information | V8-0012T01-LF1V643.003175 | FACT (seed) |
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
| `ROLE_HOME` (RoleManager) | **Present & functional**; current holder `com.google.android.apps.tv.launcherx`; `cmd role` interface works (BROWSER role resolves too) → confirms the mechanism NexaTV will use to become Home (FR-001/FR-015) | FACT (ADB `cmd role get-role-holders android.app.role.HOME` 2026-10-20) |
| Available HOME activities | 3: (1) `com.google.android.apps.tv.launcherx/.home.HomeActivity` (active); (2) `com.google.android.tungsten.setupwraith/.RecoveryActivity` (setup/recovery); (3) `com.android.tv.settings/.system.FallbackHome` (**system fallback HOME**) | FACT (ADB `pm query-activities … HOME`) |
| Launchable apps | 48 via `LAUNCHER`, **54 via `LEANBACK_LAUNCHER`** (TV apps) | FACT (ADB `query-activities`) |
| Relevant TV system features | `android.software.leanback`, `leanback_only`, `android.hardware.type.television`, `android.hardware.tv.tuner`, `live_tv` | FACT (ADB `pm list features`) |
| TV input services | **7** (`android.media.tv.TvInputService`): `com.mediatek.AirplayAPK/.AirPlayTvInputService` (AirPlay); `com.tcl.channelplus/…InputService` (IPTV); `com.tcl.tvinput/.TvPassThroughService`; `com.tcl.tvinput/.TunerInputService`; Google Videos `…tvinput.playback.TvInputService` + `…VideoPreviewInputService` | FACT (ADB `cmd package query-services` 2026-10-20) |
| Installed launcher packages | _see HOME activities above; full `pm list packages` not yet captured_ | ADB to confirm |

## 3. Android launcher model (research notes)

To document (verified on the actual TCL where noted):
- HOME intent; `MAIN` + `HOME` + `DEFAULT` categories
- What makes an Activity a launcher Activity
- How Android / Google TV chooses the HOME application
- Behavior when multiple HOME applications exist
- Behavior after reboot
- Behavior when the custom launcher crashes, and the recovery path

> Do not assume "set an APK as HOME" behaves identically across firmware. Verify on the TCL.

## 3a. M2 first install & on-device verification (2026-10-20)

First reversible install of the NexaTV launcher on the real TCL — debug variant
(`com.nexatv.launcher.debug`), started **explicitly** (`am start`), the default HOME left untouched.

| Check | Result | Source |
|-------|--------|--------|
| Install | `adb install -r app-debug.apk` → `Success`; no crash in logcat | FACT (ADB) |
| Render (FR-013/FR-014) | Grid of real app icons under an "Apps" header; labels + focus ring draw correctly | FACT (screencap) |
| Focus visible (FR-011) | Focused tile shows the `focusRing` border + lifted surface, legible across the room | FACT (screencap) |
| D-pad nav (FR-006) | Each press moves focus exactly one cell (Gallery → Media Player → Message Box) | FACT (screencap after each key) |
| Launch (FR-003) | OK on a focused tile starts **that** app (`com.tcl.messagebox/.activity.MainActivity`), not the first | FACT (`dumpsys … topResumedActivity`) |
| Reversibility | `HOME` returns to the stock `…launcherx/.home.HomeActivity`; stock launcher never modified | FACT (ADB) |
| Recovery | `adb uninstall com.nexatv.launcher.debug` removes it cleanly | FACT (plan) |

> **Testing-method note (not an app bug):** injecting `input keyevent` ~3 s after `am start` made the
> *first* D-pad event appear "absorbed" (focus didn't move) — the window was still taking input focus, so
> the first injected key was dropped by the system, not the app. With the window settled (~5 s), the first
> press moves correctly. A real remote press (human reaction time) always lands after settle, so this never
> affects real use. `Modifier.focusRestorer { firstTile }` on the grid was kept regardless — it restores
> focus to the first tile when returning from a launched app.

## 3b. M3 HOME-role detection & "become Home" findings (2026-10-20)

Validating ADR-0006 on the real TCL surfaced two firmware behaviors that the ADR's planned
`RoleManager`-based approach did not anticipate. Both are `FACT` (ADB, debug variant installed).

| Observation | Result | Source |
|-------------|--------|--------|
| `RoleManager.isRoleAvailable(ROLE_HOME)` | **false** on this firmware → an `isRoleAvailable && isRoleHeld` check wrongly reports "not Home" | FACT (app showed the pre-Home prompt while the role record named NexaTV) |
| Role record vs. resolver disagree | `cmd role get-role-holders HOME` returned `nexatv` while `resolve-activity` + the HOME button both opened **stock** LauncherX | FACT (ADB) |
| Programmatic set to a third-party launcher | `cmd package set-home-activity …/nexatv` → `Success`, but the **effective** HOME stayed stock (resolver + HOME button unchanged) | FACT (ADB) |
| Programmatic set back to stock | `set-home-activity …/launcherx` → record **and** resolver both become stock (consistent) | FACT (ADB) |

**Conclusions:**
- **Detection (fixed).** `HomeRoleRepository.isDefaultHome()` now resolves the HOME intent via
  `PackageManager.resolveActivity(…, MATCH_DEFAULT_ONLY)` and compares the resolved package to our own.
  This matches the real HOME button on every firmware; the `RoleManager` role API is **not** a reliable
  source of truth here (`isRoleAvailable` false; role record stale).
- **Becoming Home (blocked on this firmware).** The user-consented role dialog **does** fire: pressing the
  pre-Home prompt launches `permissioncontroller/…RequestRoleActivity` ("Set NexaTV as your default home
  app?" with Google TV / NexaTV / Setup Wizard), and granting it updates the role-holder record to NexaTV.
  **But the effective HOME never changes:** `resolve-activity`, an explicit HOME intent, and the physical
  HOME button all still open stock Google TV LauncherX — both immediately after the grant **and after a
  full reboot** (owner confirmed the stock grid on screen post-reboot, 2026-10-20). `cmd package
  set-home-activity …/nexatv` likewise reports `Success` with no effect.
  - **Conclusion (FACT→INFERENCE):** on this TCL Google TV firmware a third-party launcher **cannot become
    the effective HOME** through the normal `ROLE_HOME` path — Google TV pins its own LauncherX to the HOME
    button regardless of the role holder. This matches the documented Google TV (vs. AOSP Android TV)
    launcher-replacement restriction and directly challenges FR-001 on the target device.
  - **Not attempted (safety):** forcing the fallback by disabling/freezing the stock launcher
    (`pm disable-user …launcherx`) is **forbidden** by the project safety rules in early development and was
    not tried. Open options to evaluate with the owner: launch NexaTV as a *secondary* launcher / remapped
    button, an OEM-specific setting, or revisiting FR-001 scope.

## 3c. M3 accessibility-override — validated on-device (2026-10-20, ADR-0007)

Since `ROLE_HOME` replacement is blocked (§3b), we prototyped and **validated** the Projectivy-style
accessibility override (`NexaOverrideService`): a bound `AccessibilityService` that watches for the stock
launcher coming to the foreground (what HOME triggers) and raises NexaTV over it. All rows are `FACT`
(ADB + on-device, debug variant, owner enabled the service by remote).

| Observation | Result |
|-------------|--------|
| Enable via `adb settings put secure enabled_accessibility_services …` | Service shows **Enabled** but `Bound services:{}` — **never bound**. TCL `AppBoot`/`AUTO_START` manager logs `forbid bind service … callee doesn't have OP_AUTO_START permission`. Granting `appops set … AUTO_START allow` was still insufficient to bind |
| Enable via Settings → Accessibility (owner consent on remote) | **Bound** immediately (`Bound services:{Service[label=NexaTV Home override, eventTypes=TYPE_WINDOW_STATE_CHANGED]}`) |
| Press HOME | `topResumedActivity = com.nexatv.launcher…/MainActivity` — **NexaTV raised over stock** (repeated 2×) |
| Stability (loop/flicker) | Top activity stable across samples; **no loop** (service filters on the stock package, ignores its own window) |
| Back from NexaTV | Stays on NexaTV (override re-catches the stock launcher) — desirable for a Home; spawns a fresh task instance (minor) |
| App grid in override mode | After gating on `isEffectivelyHome()` (role **or** override), HOME → NexaTV shows the **full app grid** (LauncherApps enumerates without the role) |
| Survives app reinstall (`install -r`) | Service stays **Bound** |

**Conclusions:**
- **The override works on this TCL today** — HOME opens NexaTV without disabling the stock launcher
  (reversible: owner toggles the service off in Settings → Accessibility). This is the adopted mechanism
  (**ADR-0007**), superseding ADR-0006's role request on Google TV firmware.
- **Binding requires the consented Settings enable** — `adb`-only enabling is ignored by this firmware
  (TCL `AppBoot` blocks the autostart bind). So the override is always a real, owner-granted state.
- **Detection** now uses `HomeRoleRepository.isEffectivelyHome()` = resolver-is-us **or**
  `NexaOverrideService` present in `AccessibilityManager.getEnabledAccessibilityServiceList(…)`.
- **Caveats (INFERENCE):** a 2025 Google TV update reportedly broke this on some firmware (works here, could
  regress on OTA); the accessibility-consent dialog is a heavy one-time setup step; a brief stock-launcher
  flash precedes NexaTV. Secondary-launcher instructions remain the fallback.

## 3d. HOME override — clean-install regression & persistence limits (2026-10-21, v0.2.0)

After the first **signed release** (`com.nexatv.launcher`, `versionCode=200`) was **clean-installed**
(wiped + fresh sideload), the override no longer became HOME by the §3c recipe. Root-caused on the real
TCL with `logcat` + `dumpsys`. All rows `FACT` (ADB, release variant, 2026-10-21) unless marked.

| Observation | Result |
|-------------|--------|
| Why `adb`/UI-toggle enable didn't bind | `TclAppBoot` vetoes the accessibility bind: `[TclAppBoot]:forbid bind service …NexaOverrideService`, `reason='callee doesn't have OP_AUTO_START permission'`. The blocking appop is **`AUTO_START`** (default `ignore` for sideloaded apps) |
| Working bind recipe (adb) | `appops set com.nexatv.launcher AUTO_START allow` **+ force a delta** in the service list (set `enabled_accessibility_services` to a dummy, then back to the real service) **+** `accessibility_enabled 1`. Plain `settings put` with the same value does **not** rebind — no delta → `AccessibilityManagerService` never re-evaluates. (This refines §3c: `AUTO_START allow` *is* sufficient, but only with the forced delta.) |
| Survives **sleep/wake** (remote power = suspend) | ✅ **Yes.** The NexaTV task and the binding both survive; HOME keeps raising NexaTV. This is the daily case |
| Survives a **true reboot / power-loss** | ❌ **No.** On boot the pending bind is forbidden by `TclAppBoot`, which **resets** `AUTO_START` → `ignore` and `accessibility_enabled` → `0`. Isolation test: the `AUTO_START allow` op *does* persist across reboot **when no accessibility bind is pending at boot** — so it is the forbidden boot-bind itself that resets the op; the boot-bind is forbidden regardless |
| UI to grant `OP_AUTO_START` persistently | **None on this build.** TCL Guard → PermissionShield exposes only Camera / Record audio / Location; App Manager only App Installation / Uninstallation. `adb` grant is the only path, and it is reset by the boot-bind (above) |
| Override + **cold** NexaTV (no live task) | **BAL-blocked.** Even when bound, the service raises NexaTV with `startActivity(FLAG_ACTIVITY_NEW_TASK \| REORDER_TO_FRONT)`. With no existing NexaTV task (post-reboot / after process eviction) this is a cold **background** activity launch, which Android 14 **BAL silently blocks** (no log, no `ActivityTaskManager` START) → HOME shows the stock launcher. The override can only **reorder an already-live** task to front |

**Conclusions:**
- **In daily use the override works:** HOME opens NexaTV and the state survives sleep/wake. The prior
  §3c "validated" status held because `AUTO_START` was granted / the task stayed alive at that time; the
  clean reinstall reset `OP_AUTO_START` and exposed the regression (FACT→INFERENCE).
- **A true reboot breaks HOME** and currently requires re-running the adb recipe once, then opening NexaTV
  once to create a live task. No within-safety, device-only way to persist the boot-bind exists on this
  firmware (no `OP_AUTO_START` UI; stock launcher must not be disabled — §4).
- Open mitigations to weigh with the owner: a one-shot post-reboot adb helper; an in-app foreground nudge
  so a task always exists; revisit the secondary-launcher fallback for the reboot case.

## 4. Safety notes

Daily-use production device. Early phase is **read-only**. Do not disable the stock launcher, uninstall
system apps, or modify system partitions. Keep Settings reachable at all times. First NexaTV install must
be reversible.

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
