# NexaTV — Test Plan

> **Status:** ☐ Template. Stack chosen (ADR-0002 → Kotlin + Jetpack Compose-for-TV). Android TV behavior
> depends on the real device, so **manual TV testing is first-class**, not an afterthought. The per-FR/NFR
> acceptance mapping lives in [TRACEABILITY.md](./TRACEABILITY.md) (PRD v2 §19).

## Test layers

### Unit tests
Business logic (app discovery mapping, categorization, favorites/ordering, config parsing) where it has
value. Framework: **JUnit** (JVM unit tests run in CI — ADR-0003/NFR-008).

### Widget / UI tests
Important UI behavior where practical — especially **focus order and D-pad traversal** (FR-006/FR-011).
Framework: **Compose UI test** / instrumented tests injecting `DPAD_*`/`BACK`/`CENTER` (PRD v2 §19).

### Integration tests
Launcher behaviors where practical (discovery → render → launch; persistence across restart). _TBD._

### Manual TV tests (required)

Run on the actual TCL. Record evidence (logcat, photos/video where useful).

| # | Scenario | Steps | Expected | Result | Evidence |
|---|----------|-------|----------|--------|----------|
| 1 | First launch | _TBD_ | _TBD_ | ☐ | |
| 2 | Home button | Press Home | NexaTV (or stock, per phase) appears | ☐ | |
| 3 | D-pad navigation | Traverse all focusable elements | Focus visible & predictable | ☐ | |
| 4 | App launching | Select an app | App starts | ☐ | |
| 5 | Back button | Press Back | Predictable behavior; no dead-end | ☐ | |
| 6 | Reboot | Reboot TV | Launcher behaves reliably | ☐ | |
| 7 | Sleep / wake | Standby then wake | State correct; no crash | ☐ | |
| 8 | Wi-Fi unavailable | Disable network | Core still works (offline-first) | ☐ | |
| 9 | App missing / uninstalled | Remove a listed app | Graceful handling | ☐ | |
| 10 | Malformed configuration | Corrupt config | Safe recovery, no crash loop | ☐ | |
| 11 | Launcher crash | Force-crash | Recovery path works; fallback reachable | ☐ | |
| 12 | Update installation | Install new build | Upgrades cleanly | ☐ | |
| 13 | Uninstall | Remove NexaTV | Clean removal | ☐ | |
| 14 | Fallback to stock launcher | Trigger fallback | Stock launcher reachable | ☐ | |

### Performance tests (on real device)
Macrobenchmark + Baseline Profiles on the actual TCL (`armeabi-v7a`), never an emulator — cold-start p50/p95
(NFR-001), idle PSS (NFR-002), scroll/focus frame budget incl. any translucency (NFR-017). Run as the manual
pre-release gate (ADR-0003 D9 / NFR-013).

## CI (ADR-0003)
PR/main gate: `ktlint`/`detekt` + Android Lint + JVM unit tests + `assembleDebug` + `gitleaks` + ABI assert
(no 64-bit `.so`) + APK-size budget, via GitHub Actions (NFR-008). Tag-driven signed release with provenance
(NFR-009..011). Performance NFRs are gated **manually on-device** because CI has no 32-bit ARM device
(NFR-013).
