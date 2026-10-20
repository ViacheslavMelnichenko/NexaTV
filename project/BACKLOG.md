# NexaTV — Backlog

> Hierarchy: **EPIC → Feature → Task.** Keep it lean — no artificial tasks (`initial.md` §23).
> Priority: P0 (now) · P1 (next) · P2 (later). Status: ☐ Todo · ◐ Doing · ☑ Done · ⏸ Blocked.

---

## EPIC: Phase 0 — Discovery  _(M0)_

### Feature: ADB connectivity & environment
- ☑ **P0** Verify/install ADB on the Windows dev host → standalone Google platform-tools (v37.0.1) at `C:\Users\ViacheslavM\platform-tools` (not on PATH; invoked by full path, no system change). 2026-10-20.
- ☑ **P0** Wireless ADB connection to the TCL established — TV shows as `device` in `adb devices -l` (model `Smart_TV_Pro`, codename `G08`). 2026-10-20.
  - *Note:* pairing code/IP/port are runtime-only — never committed.
- ☑ **P0** Android build toolchain obtained **without full Android Studio** — corporate policy blocks only the Studio installer CDN (`edgedl.me.gvt1.com`, 403), while the SDK repo (`dl.google.com`) is reachable. Installed via command-line `sdkmanager` into `%LOCALAPPDATA%\Android\Sdk`: cmdline-tools 22.0, platform-tools 37.0.1, `platforms;android-34`, `build-tools;34.0.0`. JDK 17.0.19 already on PATH (satisfies ADR-0003). 2026-10-20.

### Feature: Safe device audit (read-only)
- ◐ **P0** Run read-only audit; record in `docs/technical/ANDROID-TV.md`. Identity, ABI, **SoC (MT9689)/GPU (Mali-G52)**, display/refresh (60 Hz fixed), HOME environment and TV inputs (7) captured 2026-10-20. *Remaining:* full `pm list packages`; §3 launcher-model behaviors (reboot/crash-recovery) need active verification.

### Feature: Open-source launcher research
- ☐ **P1** Research FLauncher, LTvLauncher(+Extended), Yukuza, Tarang, TV Launcher, LauncherCompose.
  - *Acceptance:* each per-project doc filled from the real repo; claims labelled FACT/INFERENCE/ASSUMPTION/UNKNOWN with source refs.
- ☐ **P1** Build the feature matrix (`docs/research/FEATURE_MATRIX.md`). *Depends on:* per-project research.
- ☐ **P1** License analysis (`docs/research/LICENSE_ANALYSIS.md`). *Depends on:* per-project research.
- ☐ **P1** Architecture comparison (`docs/research/ARCHITECTURE_COMPARISON.md`). *Depends on:* per-project research.

### Feature: Architectural decision
- ☑ **P1** Decide & finalize ADR-0001 (Build vs. Fork) → **greenfield, permissive license, clean-room references** (2026-10-20).
- ☑ **P0** Draft & decide ADR-0002: Flutter vs. Kotlin/Compose-for-TV → **Accepted: Kotlin + Jetpack Compose-for-TV** (direct Android APIs, no Flutter), 2026-10-20.
- ☑ **P0** Validation spike (gate) — **PASS** 2026-10-20 (`docs/technical/SPIKE-ADR-0002.md`). Compose-for-TV builds (AGP 8.7.3 / Gradle 8.9 / Kotlin 1.9.24 / tv-material 1.0.0), APK has no native (`primaryCpuAbi=null`) so installs/runs on `armeabi-v7a` TCL, D-pad focus owner-confirmed on screen, cold start ~833 ms (`am start -W`, ceiling). Spike code throwaway/git-ignored.
  - ☑ **P1** Precise baselines via **Macrobenchmark** — 2026-10-20. NFR-001 cold-start (`COLD`, `CompilationMode.None`, 10 iters): **min 937.9 / median 984.4 / max 1346.8 ms** (budget ≤2.0 s p50 → **PASS**). NFR-002 idle PSS (`dumpsys meminfo`): **≈48.6 MB**. Device: TCL G08, 4×1.39 GHz, 2.5 GB. See `docs/technical/SPIKE-ADR-0002.md`.
    - ☐ **P2** NFR-017 glass fps/PSS gate — still open; needs the real glass UI (M1+), not the spike.
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
