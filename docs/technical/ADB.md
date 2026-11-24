# ADB — Android Debug Bridge Reference (for NexaTV)

> Reference for working with the TCL over ADB. Record **real** observed output as evidence; never invent
> output. **Never commit pairing codes, device IPs or ports.** Those are runtime data.

## What ADB is

`adb` (host client) talks to `adbd` (daemon on the TV) to run shell commands, install/uninstall APKs,
query packages, and launch activities. Over a TV we use **wireless debugging** (pair once, then connect).

## Development host

Any OS with Google **platform-tools** (`adb`) on `PATH`. For example, on macOS use
`brew install --cask android-platform-tools`; on Windows use
`winget install --id Google.PlatformTools -e`. The project was first set up on Windows with
platform-tools 37.0.1. Builds need JDK 17+ and Android SDK platform 37.1 (ADR-0014).

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

## Commands that modify the device

`pm install` / `pm uninstall`, `am start` and changing the default HOME all modify the device. Follow the
device-safety rules (AGENTS.md rule 3) before using any of them.

## Updating NexaTV on the TV

Updates are sideloaded (ADR-0003 D7). The signed APK comes from the GitHub Release.

| Action | Command | Notes |
|--------|---------|-------|
| Update in place | `adb install -r nexatv-X.Y.Z.apk` | Same signing cert → data preserved |
| First switch off debug | `adb install -r nexatv-X.Y.Z.apk` | Release id `com.nexatv.launcher` is a **separate** package from the `.debug` build; after installing, re-point the accessibility-HOME override to it (then optionally `adb uninstall com.nexatv.launcher.debug`) |
| Roll back | `adb install -r nexatv-<prev>.apk` | Previous release APK is kept on GitHub |

> The release build is **minified (R8)**; always run the on-device smoke test before trusting a new
> version (the manual on-device gate is described in `.github/workflows/release.yml`).
