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
- ☐ **P0** Draft & decide ADR-0002: Flutter vs. Kotlin/Compose-for-TV. *Depends on:* ADR-0001 ✓. ← **next**
- ☐ **P1** Pick exact permissive license (MIT vs Apache-2.0) and add `LICENSE` file.

### Feature: Android launcher-model understanding
- ☐ **P2** Document HOME intent / multiple-HOME / reboot / crash-recovery behavior, verified on the TCL.

---

## EPIC: Phase 1 — Foundation  _(M1, not started)_
- ☐ **P2** Create the code scaffold for the chosen stack. *Depends on:* ADR-0001 + stack ADR.

---

## EPIC: Phase 2+ — Launcher capabilities  _(M2+, placeholder)_
Derived from the PRD once written. MVP target: FR-001..FR-009 (see `docs/product/PRD.md`).
