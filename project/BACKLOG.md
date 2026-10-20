# NexaTV — Backlog

> Hierarchy: **EPIC → Feature → Task.** Keep it lean — no artificial tasks (`initial.md` §23).
> Priority: P0 (now) · P1 (next) · P2 (later). Status: ☐ Todo · ◐ Doing · ☑ Done · ⏸ Blocked.

---

## EPIC: Phase 0 — Discovery  _(M0)_

### Feature: ADB connectivity & environment
- ☐ **P0** Verify ADB is installed on the Windows dev host (`adb --version`); install if missing.
  - *Acceptance:* `adb --version` prints a version.
- ☐ **P0** Establish wireless ADB connection to the TCL (pair + connect).
  - *Acceptance:* TV shows as `device` in `adb devices -l`. *Depends on:* ADB installed.
  - *Note:* pairing code/IP/port are runtime-only — never commit.

### Feature: Safe device audit (read-only)
- ☐ **P0** Run read-only audit commands; record results in `docs/technical/ANDROID-TV.md`.
  - *Acceptance:* identity, display, launcher/HOME, features sections filled with real output.
  - *Depends on:* ADB connection.

### Feature: Open-source launcher research
- ☐ **P1** Research FLauncher, LTvLauncher(+Extended), Yukuza, Tarang, TV Launcher, LauncherCompose.
  - *Acceptance:* each per-project doc filled from the real repo; claims labelled FACT/INFERENCE/ASSUMPTION/UNKNOWN with source refs.
- ☐ **P1** Build the feature matrix (`docs/research/FEATURE_MATRIX.md`). *Depends on:* per-project research.
- ☐ **P1** License analysis (`docs/research/LICENSE_ANALYSIS.md`). *Depends on:* per-project research.
- ☐ **P1** Architecture comparison (`docs/research/ARCHITECTURE_COMPARISON.md`). *Depends on:* per-project research.

### Feature: Architectural decision
- ☑ **P1** Decide & finalize ADR-0001 (Build vs. Fork) → **greenfield, permissive license, clean-room references** (2026-10-20).
- ☑ **P0** Draft & decide ADR-0002: Flutter vs. Kotlin/Compose-for-TV → **Accepted: Kotlin + Jetpack Compose-for-TV** (direct Android APIs, no Flutter), 2026-10-20.
- ☐ **P0** Validation spike (gate, ~½ day): trivial Compose-for-TV release APK on the TCL — confirm `armeabi-v7a` install/run, `tv-material` D-pad focus, cold-start/RSS via Macrobenchmark. *Depends on:* ADR-0002.
- ☑ **P1** Pick exact permissive license (MIT vs Apache-2.0) and add `LICENSE` file → **Apache-2.0** chosen; `LICENSE` + `NOTICE` added (2026-10-20).

### Feature: Android launcher-model understanding
- ☐ **P2** Document HOME intent / multiple-HOME / reboot / crash-recovery behavior, verified on the TCL.

---

## EPIC: Phase 1 — Foundation  _(M1, not started)_
- ☐ **P2** Create the code scaffold for the chosen stack. *Depends on:* ADR-0001 + stack ADR.

---

## EPIC: Phase 1 — Foundation (cont.)
- ☑ **P0** Author PRD v1 — MVP locked to **FR-001..FR-009** under NFR-001..NFR-007 (`docs/product/PRD.md`, 2026-10-20, Draft v1 for review).
- ☑ **P0** PRD critics review + author **PRD v2** — folded in MUST+MVP-blocking and SHOULD(doc-now) deltas from the 6-lens expert panel: new sections §15 Delivery/CI-CD, §16 Design System, §17 Accessibility, §18 Permissions, §19 Testability/Observability/DoD; added **FR-011..FR-017** and **NFR-008..NFR-023** (2026-10-20, Draft v2 for review).
- ☑ **P1** Record the PRD-v2 follow-up ADRs (Hard Rule 6):
  - ☑ **ADR-0003** — CI/CD, signing, release & update/rollback strategy (GitHub Actions, tag-driven signed releases, single long-lived key + offline backup, manual sideload, reproducibility = non-goal) — covers NFR-008..015. **Accepted** 2026-10-20.
  - ☑ **ADR-0004** — App enumeration without `QUERY_ALL_PACKAGES` (`LauncherApps.getActivityList` via `ROLE_HOME`) — covers FR-015/016. **Accepted** 2026-10-20.
  - ☑ **ADR-0005** — "Glass"/translucency technique (fake-glass default; real-time blur gated on-device) — covers NFR-017. **Accepted** (measurement-gated) 2026-10-20.
- ☑ **P1** Create the FR×Test traceability matrix stub (`docs/testing/TRACEABILITY.md`) + refresh TEST-PLAN.md for the Kotlin stack. 2026-10-20.

## EPIC: Phase 2+ — Launcher capabilities  _(M2+, placeholder)_
MVP target: **FR-001..FR-009** (see `docs/product/PRD.md`). Tasks per FR are broken out when M2 planning starts.
