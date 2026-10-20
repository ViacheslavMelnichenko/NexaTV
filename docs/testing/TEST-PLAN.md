# NexaTV — Test Plan

> **Status:** ☐ Template. Expands once the architecture/stack is chosen. Android TV behavior depends on
> the real device, so **manual TV testing is first-class**, not an afterthought.

## Test layers

### Unit tests
Business logic (app discovery mapping, categorization, favorites/ordering, config parsing) where it has
value. Framework: _TBD (depends on stack — Dart `test` / JUnit)._

### Widget / UI tests
Important UI behavior where practical — especially **focus order and D-pad traversal**. Framework: _TBD._

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

## CI (later — do not over-engineer early)
Once the app exists: format/lint, tests, static analysis (e.g. `flutter analyze`), build artifact,
via GitHub Actions. Add CI when it provides value (`initial.md` §27).
