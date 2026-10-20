# ADB — Android Debug Bridge Reference (for NexaTV)

> **Status:** ☐ Template. ADB is a first-class development tool for this project. The user is new to ADB,
> so entries should be clear but technically accurate. Record **real** observed output as evidence; never
> invent output. **Never commit pairing codes, device IPs, or ports** — those are runtime data.

## What ADB is

`adb` (host client) talks to `adbd` (daemon on the TV) to run shell commands, install/uninstall APKs,
query packages, and launch activities. Over a TV we use **wireless debugging** (pair once, then connect).

## Development host

- OS: **Windows 11** (per environment). Shell used here: Git Bash.
- ADB install status on this machine: _UNKNOWN — verify (`adb --version`)_.

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

## Command log (evidence)

| Date | Command | Observed output (summary) |
|------|---------|---------------------------|
| _TBD_ | _TBD_ | _TBD_ |
