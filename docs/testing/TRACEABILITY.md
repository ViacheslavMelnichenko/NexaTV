# NexaTV — Requirement → Test Traceability Matrix

> **Status:** ☐ Stub (2026-10-20). Created per PRD v2 §19 "Definition of Done & Traceability".
> Every requirement in [PRD.md](../product/PRD.md) maps here to its verification. A requirement is **Done**
> only when it has (1) an objective acceptance criterion, (2) ≥1 automated test **or** a documented manual
> test, and (3) a recorded passing run. Test IDs link to [TEST-PLAN.md](./TEST-PLAN.md) manual scenarios
> (`M#`) or to automated suites once they exist (`U#` unit, `UI#` Compose/instrumented, `P#` Macrobenchmark).
>
> **Type:** `unit` · `ui` (Compose/instrumented, incl. D-pad injection) · `perf` (on-device Macrobenchmark) ·
> `manual` (on real TCL) · `ci` (pipeline assertion). **Status:** ☐ Todo · ◐ In progress · ☑ Verified.

## Functional requirements

| Req | Acceptance criterion (summary — see PRD §7) | Test type | Test ref | Status |
|-----|---------------------------------------------|-----------|----------|--------|
| FR-001 | Registers as HOME; settable via `ROLE_HOME`; HOME opens NexaTV; stock never disabled | manual + ui | M2, M14 | ☐ |
| FR-002 | Lists launchable apps; add/remove reflected ≤2 s, no restart; off-main | ui + manual | M1, M9 | ☐ |
| FR-003 | Launch intent starts app; failed launch = in-UI, focus-preserving | ui + manual | M4 | ☐ |
| FR-004 | Mark/unmark favorite; shown first; persists across reboot | ui + manual | M6 | ☐ |
| FR-005 | Grouping (Favorites + All); identical across 2 cold starts & 1 reinstall | ui + manual | M1 | ☐ |
| FR-006 | Fully D-pad operable; focus always visible; last-focus restored; no trap | ui + manual | M3, M5 | ☐ |
| FR-007 | Settings reachable; changes ≥1 real pref; opens set-default-Home flow | ui + manual | M2 | ☐ |
| FR-008 | Favorites/order/settings survive restart **and** reboot | manual | M6 | ☐ |
| FR-009 | Cold-starts to interactive focused Home within budget; comes up as Home post-reboot | perf + manual | P1, M6 | ☐ |
| FR-011 | Focus shows ≥2 signals; indicator ≥3:1; settles ≤150 ms; readable at 3 m/55″ | ui + manual | M3 | ☐ |
| FR-012 | Content + focus ring within ≥5% safe area; first/last row items fully visible | manual | M3 | ☐ |
| FR-013 | Canonical tile; deterministic art order; ≥10 real apps render uniformly | ui + manual | — | ☐ |
| FR-014 | Skeleton (never blank); empty/no-apps states; in-UI focus-preserving launch error | ui | — | ☐ |
| FR-015 | `getActivityList` via ROLE_HOME; no `QUERY_ALL_PACKAGES`; pre-Home prompt; count delta 0 | ui + manual + ci | M1 | ☐ |
| FR-016 | Excludes no-launch/disabled/suspended/self/(stock); `PACKAGE_CHANGED` subscribed | ui | — | ☐ |
| FR-017 | Long-press App info (all) / Uninstall (user apps only, never system) — *build-later* | ui + manual | M13 | ☐ |

## Non-functional requirements

| Req | Target / measure (summary — see PRD §8, §15–§19) | Test type | Test ref | Status |
|-----|--------------------------------------------------|-----------|----------|--------|
| NFR-001 | Cold start ≤2.0 s p50 / ≤3.0 s p95 on TCL (Macrobenchmark) | perf | P1 | ☐ |
| NFR-002 | Idle PSS ≤ ~150 MB; no leaks across navigation | perf | P2 | ☐ |
| NFR-003 | No UI-thread block during discovery; no ANR; frame budget met | perf + ui | P3 | ☐ |
| NFR-004 | FR-001..009 fully work with no network | manual | M8 | ☐ |
| NFR-005 | Reliable after repeated reboots; state intact | manual | M6 | ☐ |
| NFR-006 | Zero ads/telemetry/account; no data leaves device (no `INTERNET`) | ci + manual | — | ☐ |
| NFR-007 | Reversible install; stock stays selectable; uninstall restores prior Home | manual | M13, M14 | ☐ |
| NFR-008 | PR/main CI: assemble+lint+ktlint/detekt+unit+gitleaks+ABI, ≤10 min, required | ci | — | ☐ |
| NFR-009 | `main` protected; tag-driven release; never on fork PRs | ci | — | ☐ |
| NFR-010 | Single long-lived key, v2+v3; secrets in GH env + offline backup; fail-fast | ci + manual | — | ☐ |
| NFR-011 | SemVer; strictly-increasing `versionCode`; release has APK+SHA-256+cert+git SHA | ci | — | ☐ |
| NFR-012 | Pinned toolchain; release = CI-from-tag (full repro = non-goal) | ci | — | ☐ |
| NFR-013 | Manual pre-release Macrobenchmark gate on TCL; block if p95>3.0 s or PSS>~150 MB | perf (manual) | P1, P2 | ☐ |
| NFR-014 | gitleaks + ABI assert + APK-size budget (COULD: SBOM/vuln/Dependabot/denylist) | ci | — | ☐ |
| NFR-015 | Update = `adb install -r` higher `versionCode`; prev APK kept; documented rollback | manual | M12 | ☐ |
| NFR-016 | Single theme: ≥6 pastel roles, body ≥18sp, 8dp grid; zero hardcoded hex/sp (lint) | ci + ui | — | ☐ |
| NFR-017 | Fake-glass default; real-time blur only if ≥58 fps on TCL & no NFR-001/002 regress | perf | P3 | ☐ |
| NFR-018 | Contrast ≥4.5:1 body / ≥3:1 large & focus-ring vs worst real backdrop; opaque scrim | ui + manual | M3 | ☐ |
| NFR-019 | Usable un-clipped at font-scale ≥1.3×; sp only; TalkBack semantics | ui + manual | — | ☐ |
| NFR-020 | Strings in resources (en+uk); +30% length tolerance; locale from system | ui | — | ☐ |
| NFR-021 | Least privilege; `allowBackup=false`; no `INTERNET`/`ACCESS_NETWORK_STATE` (CI) | ci | — | ☐ |
| NFR-022 | Config documented owner-local non-sensitive; at-rest encryption out of MVP scope | manual | — | ☐ |
| NFR-023 | `qaDebug` local crash/ANR export; zero release egress; 50× reboot soak 0 crash/0 ANR | manual + ci | M6, M11 | ☐ |

## Notes

- `—` under Test ref = no dedicated scenario authored yet; add one in TEST-PLAN.md when the FR/NFR is scheduled.
- Automated test IDs (`U#`/`UI#`/`P#`) are assigned when the suites are created in M1+ (code not written yet —
  Phase 0 discipline, PRD §13 / CLAUDE.md Hard Rule 1).
