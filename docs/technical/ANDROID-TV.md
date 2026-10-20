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
| Model | _UNKNOWN_ | ADB to confirm |
| Product / device codename | _UNKNOWN_ | ADB to confirm |
| SDK level | _UNKNOWN (expect 34)_ | ADB to confirm |
| Build fingerprint | _UNKNOWN_ | ADB to confirm |
| CPU ABI | _UNKNOWN_ | ADB to confirm |
| CPU cores | _UNKNOWN_ | ADB to confirm |
| RAM | _UNKNOWN_ | ADB to confirm |
| Resolution | _UNKNOWN_ | ADB to confirm |
| Density | _UNKNOWN_ | ADB to confirm |
| Refresh rate | _UNKNOWN_ | ADB to confirm |

## 2. Launcher / HOME environment (to be ADB-confirmed)

| Field | Value | Source |
|-------|-------|--------|
| Current default launcher (package/activity) | _UNKNOWN_ | ADB `resolve-activity` |
| Available HOME activities | _UNKNOWN_ | ADB `pm query-activities` |
| Installed launcher packages | _UNKNOWN_ | ADB |
| TV input services | _UNKNOWN_ | ADB |
| Relevant system features (e.g. `android.software.leanback`) | _UNKNOWN_ | ADB `pm list features` |
| Relevant package names | _UNKNOWN_ | ADB |

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
