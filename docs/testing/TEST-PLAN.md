# NexaTV — Test Plan & Traceability

This is the single testing document: test layers, CI gates, manual TV scenarios, and the
requirement → test map. It absorbs the former `TRACEABILITY.md`. Android TV behavior depends on the real
device, so **manual testing on the TCL is first-class**. Requirements are defined in
[PRD.md](../product/PRD.md) §7–§8 and §15–§19.

## Test layers

| Layer | Where | Runs in CI | Notes |
|-------|-------|------------|-------|
| JVM unit tests | `app/src/test/` (JUnit 4, coroutines-test, MockK, real `org.json`) | Yes | Covers logic that does not need Android |
| Compose UI / instrumented | Not written yet | No | Next step for focus-order and D-pad traversal (FR-006/FR-011) |
| Performance | [`:benchmark`](../../benchmark/README.md) (Macrobenchmark + Baseline Profile) | No | Device-gated; there is no 32-bit ARM runner (ADR-0003 D9) |
| Manual TV | The TCL, scenarios below | No | Required before trusting a release |

### Automated suites (JVM)

| Suite | Covers |
|-------|--------|
| `HomeViewModelTest` | Home-role gating and pre-Home prompt (FR-015); Loading/Empty/Apps states (FR-014); live reload on app change (FR-002); launch delegation (FR-003) |
| `InstalledAppsRepositoryTest` | De-dup, self-exclusion and sorting of discovered apps (FR-002/FR-016); profile-aware identity |
| `FavoritesRepositoryTest` | Pin/unpin/reorder and resolution against installed apps (FR-004, FR-008) |
| `SettingsRepositoryTest` | Tile size, theme and background settings mapping and persistence (FR-007, ADR-0009) |
| `WeatherRepositoryTest` | Open-Meteo parsing and fail-soft behavior (ADR-0010) |
| `HomeEnvironmentViewModelTest` | Shared weather polling cadence; no polling while Home is hidden (ADR-0013) |
| `SceneResolverTest` | Time/weather → background scene; every scene bundled within the artwork budget (ADR-0013) |
| `ThemeCatalogTest` | Every theme resolves to a palette; every palette clears the contrast floors (NFR-018) |

## CI gates (`.github/workflows/ci.yml`, ADR-0003)

Every PR and push to `main` runs:
- `assembleDebug`, Android Lint, `testDebugUnitTest` and detekt;
- an ABI assert (`armeabi-v7a` native libraries only);
- the debug APK size budget, **≤ 45 MB**;
- a gitleaks secret scan.

The debug budget was raised from 30 MB when Compose 1.13's debug-only `ui-tooling` added about 16 MB.

Other workflows:
- **Release** (tag `vX.Y.Z`): a signed APK with a **≤ 20 MB** budget.
- **Preview APK** (`codex/**` branches): a **≤ 30 MiB** budget.

Performance NFRs are gated **manually on the device**, because CI has no 32-bit ARM device (NFR-013).

## Manual TV scenarios (run on the TCL)

Record evidence: logcat, a photo or a short video. Use `M#` IDs in the traceability tables below.

| # | Scenario | Steps | Expected |
|---|----------|-------|----------|
| M1 | First launch | Fresh install, enable the override, press HOME | Prompt until effectively Home, then favorites/grid; no partial list |
| M2 | Home button | Press HOME from All apps, Settings and another app | NexaTV Home appears, reset to the Home screen |
| M3 | D-pad navigation | Traverse every focusable element | Focus always visible and predictable; no trap |
| M4 | App launching | OK on a tile; also an app that fails to start | App starts; failure shows an in-UI error and keeps focus |
| M5 | Back button | Press BACK on each screen | Predictable; no dead end |
| M6 | Reboot | Reboot the TV | Comes up as Home; favorites and settings intact |
| M7 | Sleep / wake | Standby, then wake | Correct state; no crash |
| M8 | No network | Disable Wi-Fi | Everything except weather works (offline-first) |
| M9 | App installed / removed | Install or uninstall an app | Grid updates within about 2 s without a restart |
| M10 | Corrupt state file | Truncate `launcher_state.pb` | Resets to defaults; no crash loop |
| M11 | Launcher crash | Force-stop or crash NexaTV | Recovers; the stock launcher stays reachable |
| M12 | Update | `adb install -r` a higher `versionCode` | Upgrades cleanly; data kept |
| M13 | Uninstall | Remove NexaTV | Clean removal; the prior Home is back |
| M14 | Fallback to stock | Disable the override | The stock launcher is Home again |
| M15 | Favorites | Pin, unpin, Move (◄ ► OK / BACK) | Order persists; OK after a move does not open the menu; focus stays in the row after an unpin |

## Traceability — functional requirements

A requirement is **verified** only with an objective criterion, at least one automated or documented manual
test, and a recorded passing run. No manual run has been recorded in this file yet. On-device evidence for
M2/M3 lives in [ANDROID-TV.md](../technical/ANDROID-TV.md) §3a–§3c.

| Req | Acceptance (summary) | Automated | Manual |
|-----|----------------------|-----------|--------|
| FR-001 | NexaTV is effectively Home (override, ADR-0007); stock never disabled | `HomeViewModelTest` | M2, M14 |
| FR-002 | Lists launchable apps; add/remove reflected ≤ 2 s; off-main | `HomeViewModelTest`, `InstalledAppsRepositoryTest` | M1, M9 |
| FR-003 | Launch starts the app; a failure is shown in the UI and keeps focus | `HomeViewModelTest` | M4 |
| FR-004 | Pin/unpin favorites; shown first; persist | `FavoritesRepositoryTest` | M15 |
| FR-005 | Categories — **deferred** ([ROADMAP](../../project/ROADMAP.md)) | — | — |
| FR-006 | Fully D-pad operable; focus visible; no trap | — | M3, M5 |
| FR-007 | Settings reachable and change real preferences | `SettingsRepositoryTest` | M3 |
| FR-008 | Favorites, order and settings survive restart and reboot | `FavoritesRepositoryTest` | M6 |
| FR-009 | Cold start to a focused Home within budget | — | P1, M6 |
| FR-011 | Focus visible from 3 m; ring contrast ≥ 3:1 | — | M3 |
| FR-012 | Content and focus ring inside the safe area | — | M3 |
| FR-013 | Canonical square-icon tile (ADR-0012) | — | M3 |
| FR-014 | Never blank; Empty/no-apps states; in-UI launch error | `HomeViewModelTest` | M1, M4 |
| FR-015 | No `QUERY_ALL_PACKAGES`; pre-Home prompt | `HomeViewModelTest` | M1 |
| FR-016 | Excludes self and non-launchable apps; `PACKAGE_CHANGED` handled | `InstalledAppsRepositoryTest` | M9 |
| FR-017 | Long-press App info | — | M15 |

## Traceability — non-functional requirements

| Req | Target (summary) | Verification |
|-----|------------------|--------------|
| NFR-001 | Cold start ≤ 2.0 s p50 / ≤ 3.0 s p95 on the TCL | P1 `:benchmark` (baseline 984 ms median, [ROADMAP](../../project/ROADMAP.md)) |
| NFR-002 | Idle PSS ≤ about 150 MB | P2 `dumpsys meminfo` (baseline about 48.6 MB) |
| NFR-003 | No UI-thread block during discovery; frame budget met | P3 `:benchmark` frame timing |
| NFR-004/005 | Works offline; reliable across reboots | M6, M8 |
| NFR-006/021 | No ads/telemetry; least privilege. `INTERNET` is allowed **only for weather** (amended by ADR-0010) | Manifest review, M8 |
| NFR-007 | Reversible; the stock launcher stays selectable | M13, M14 |
| NFR-008..011, 014 | CI gate, tag-driven signed release, SemVer, secret/ABI/size checks | CI (above), ADR-0003 |
| NFR-012 | Pinned toolchain (`libs.versions.toml`, ADR-0014) | CI |
| NFR-013 | Manual pre-release performance gate on the TCL | P1, P2 |
| NFR-015 | Update and rollback via `adb install -r` | M12, [ADB.md](../technical/ADB.md) |
| NFR-016 | Token-only UI; body text ≥ 18sp (tile names use the 15sp `labelMedium` label token) | Design-system review ([checklist](../design/DESIGN-SYSTEM.md)) |
| NFR-017 | Fake-glass default; real-time blur only if ≥ 58 fps | ⏸ deferred |
| NFR-018 | Contrast ≥ 4.5:1 body / ≥ 3:1 focus ring | `ThemeCatalogTest`, M3 |
| NFR-019/020 | Font scale; en+uk strings | M3 (no automated check yet) |
| NFR-023 | Crash recovery; reboot soak | M6, M10, M11 |
