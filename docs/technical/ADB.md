# ADB — Android Debug Bridge Reference (for NexaTV)

> **Status:** ☐ Template. ADB is a first-class development tool for this project. The user is new to ADB,
> so entries should be clear but technically accurate. Record **real** observed output as evidence; never
> invent output. **Never commit pairing codes, device IPs, or ports** — those are runtime data.

## What ADB is

`adb` (host client) talks to `adbd` (daemon on the TV) to run shell commands, install/uninstall APKs,
query packages, and launch activities. Over a TV we use **wireless debugging** (pair once, then connect).

## Development host

- OS: **Windows 11** (per environment). Shell used here: Git Bash.
- ADB install status: **Installed** (FACT, 2026-10-20). Android Debug Bridge 1.0.41 / platform-tools **37.0.1**.
  - Installed via `winget install --id Google.PlatformTools -e --source winget`.
  - Binary: `%LOCALAPPDATA%\Microsoft\WinGet\Packages\Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe\platform-tools\adb.exe`
  - winget modified PATH and added an `adb` alias, but the alias is not usable from the current Git Bash
    session; invoke adb by full path (or restart the shell to pick up the new PATH).

## Connection workflow (wireless)

> Pairing code / IP / port are shown on the TV at runtime and change — treat as throwaway, keep them out of Git.

| Step | Command (fill real host values at runtime) | Expected result |
|------|--------------------------------------------|-----------------|
| Verify adb present | `adb --version` | Version string |
| Pair (one-time) | `adb pair <IP>:<PAIR_PORT>` → enter code | "Successfully paired" |
| Connect | `adb connect <IP>:<CONNECT_PORT>` | "connected to …" |
| List devices | `adb devices -l` | TV listed as `device` |

## Safe read-only commands (device audit — run these first)

| Purpose | Command | Notes |
|---------|---------|-------|
| Full properties | `adb shell getprop` | Large; grep specific keys |
| Model / brand | `adb shell getprop ro.product.model` / `...brand` | |
| Android version | `adb shell getprop ro.build.version.release` | Expect `14` |
| SDK level | `adb shell getprop ro.build.version.sdk` | |
| Build fingerprint | `adb shell getprop ro.build.fingerprint` | |
| CPU ABI | `adb shell getprop ro.product.cpu.abi` | |
| CPU cores | `adb shell cat /proc/cpuinfo` | |
| Memory | `adb shell cat /proc/meminfo` | |
| Display | `adb shell wm size` / `adb shell wm density` | Resolution / density |
| Current HOME / launcher | `adb shell cmd package resolve-activity -c android.intent.category.HOME -a android.intent.action.MAIN` | Identifies active launcher |
| List HOME activities | `adb shell pm query-activities -a android.intent.action.MAIN -c android.intent.category.HOME` | All candidate launchers |
| List packages | `adb shell pm list packages` | Installed packages |
| List launchable apps | `adb shell cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.LAUNCHER` | TV app discovery basis |
| System features | `adb shell pm list features` | e.g. `android.software.leanback` |
| Logcat | `adb logcat` | Live logs (evidence) |

> Record the actual audited values in [ANDROID-TV.md](./ANDROID-TV.md).

## Commands reserved for later (NOT in early read-only phase)

`pm install` / `pm uninstall`, `am start` (launching our activity), changing default HOME. These modify
the device — follow the safety checklist in `initial.md` §16 before using any of them.

## Updating NexaTV on the TV

Updates are sideloaded (ADR-0003 D7). The signed APK comes from the GitHub Release.

| Action | Command | Notes |
|--------|---------|-------|
| Update in place | `adb install -r nexatv-X.Y.Z.apk` | Same signing cert → data preserved |
| First switch off debug | `adb install -r nexatv-X.Y.Z.apk` | Release id `com.nexatv.launcher` is a **separate** package from the `.debug` build; after installing, re-point the accessibility-HOME override to it (then optionally `adb uninstall com.nexatv.launcher.debug`) |
| Roll back | `adb install -r nexatv-<prev>.apk` | Previous release APK is kept on GitHub |

> The release build is **minified (R8)**; always run the on-device smoke test in
> [RELEASE-CHECKLIST.md](../testing/RELEASE-CHECKLIST.md) before trusting a new version.

## Command log (evidence)

| Date | Command | Observed output (summary) |
|------|---------|---------------------------|
| _TBD_ | _TBD_ | _TBD_ |
