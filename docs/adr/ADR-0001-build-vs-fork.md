# ADR-0001: Build vs. Fork

## Status

**Accepted** — 2026-10-20. (Decided by the project owner after Phase 0 research.)

## Context

NexaTV needs its foundational decision: build the launcher from scratch, fork an existing open-source
Android TV launcher, or take a hybrid path. Phase 0 research investigated six candidates directly from
their repositories (Phase-0 research, now archived). The decisive findings:

- **Licensing (FACT):** every launcher that could realistically serve as a fork base is **GPL-3.0**:
  FLauncher, all LTvLauncher forks (incl. Extended), and the strongest native candidate `tv-launcher`.
  GPL-3.0 is strong copyleft — forking any of them forces NexaTV to be GPL-3.0 and to disclose source.
  The only permissively-licensed code is **LauncherCompose (Apache-2.0)**, whose scope is a hotel/IPTV
  guest launcher (narrow reuse value), and **Yukuza (MIT declared in README but with no `LICENSE` file)**,
  which is a due-diligence blocker until a real license file exists.
  See the Phase-0 license analysis (archived).
- **Device constraints (FACT, from the audit):** target TCL is **32-bit ARM only** (`armeabi-v7a`),
  4 cores, ~2.3 GiB RAM, UI at 1080p. Favors a lean implementation; both Flutter and Kotlin/Compose-for-TV
  remain viable. See [`../technical/ANDROID-TV.md`](../technical/ANDROID-TV.md).
- **Project philosophy (`initial.md` §34):** NexaTV must be clean, maintainable, independent, and
  explicitly *not* a generic AI-generated app — architecture and decisions must remain intentional.

The project owner chose a **permissive license (MIT/Apache)** for NexaTV. That choice is incompatible
with copying GPL-3.0 code, which removes "fork a GPL project" from the table as a code-reuse strategy.

## Decision

**Build NexaTV greenfield under a permissive license (MIT or Apache-2.0; exact choice is a minor
follow-up).** Existing GPL-3.0 launchers (FLauncher, LTvLauncher, `tv-launcher`) are used **strictly as
clean-room design references** — we study their behavior and architecture for ideas, and **never copy
their code** into NexaTV. Permissively-licensed code (LauncherCompose, Apache-2.0) may be reused only
with recorded attribution and a clearance entry; Yukuza remains **blocked for reuse** until it publishes a
proper `LICENSE` file.

This corresponds to **Option C/D (greenfield with references)** in the architecture comparison. The
**technology choice (Flutter vs. Kotlin/Compose-for-TV) is deferred to ADR-0002** — it is orthogonal to
build-vs-fork and deserves its own evidence-based decision.

## Alternatives

Evaluated against license · effort · maintainability · architecture · UX flexibility · TV compatibility ·
independence · upgrade path. (Full evidence in the Phase-0 architecture comparison, archived.)

| Option | License outcome | Effort | Independence | Verdict |
|--------|-----------------|--------|--------------|---------|
| Fork FLauncher / LTvLauncher (Flutter) | Forces **GPL-3.0**; inherits Firebase telemetry + 2023-era debt | Low (fast start) | Low (upstream + copyleft) | ❌ Rejected — conflicts with permissive goal |
| Fork `tv-launcher` (Kotlin/Compose-TV) | Forces **GPL-3.0**; no tests to inherit | Medium | Low | ❌ Rejected — same license conflict |
| Fork another (Yukuza / Tarang / LauncherCompose) | Yukuza = MIT w/o LICENSE (blocked); Tarang = no license (blocked); LauncherCompose = Apache but hotel-scoped | Med–High | Mixed | ❌ Rejected as a *base* (reuse selectively instead) |
| **Greenfield + clean-room references** | **Permissive, our choice** | High | **High** | ✅ **Chosen** |
| Hybrid (greenfield + Apache-2.0 snippets) | Permissive, with attribution | High | High | ◐ Folded into the chosen option (reuse allowed where license-cleared) |

## Consequences

**Positive**
- Full freedom over license, architecture, and UX; no inherited copyleft, telemetry, or technical debt.
- Architecture stays intentional and understandable (aligns with `initial.md` §34).
- Clean-room references de-risk the hardest parts (we know *what good looks like* before building).

**Negative / costs**
- Significantly more engineering effort. We must reimplement Android-TV edge cases ourselves:
  app discovery (`LEANBACK_LAUNCHER` + `LAUNCHER`, dedup, live package updates), D-pad focus traversal
  (FLauncher's row-by-row policy and `tv-launcher`'s focus-restorer patterns are reference models),
  HOME registration & default-launcher handling (`RoleManager ROLE_HOME`), and optionally TV inputs/channels.
- No free upstream maintenance; NexaTV owns all fixes.

**Mitigations**
- Keep the MVP small (PRD FR-001..FR-009). Use the research docs as implementation checklists.
- Maintain a reuse clearance log in `LICENSE_ANALYSIS.md`; enforce the clean-room rule (`initial.md` §30).

## Rationale

GPL-3.0 copyleft is fundamentally incompatible with the owner's permissive-license goal, so every
fork-a-GPL-project path is out for code reuse. Forking would also import architecture decisions,
telemetry, and technical debt that contradict NexaTV's "intentional, clean, independent" philosophy.
Greenfield costs more up front, but the Phase 0 research gives us concrete, license-safe reference
designs for the expensive TV-specific mechanics — which is exactly what makes a from-scratch build
tractable rather than naive. We therefore build NexaTV ourselves, standing on the *lessons* of the
existing ecosystem rather than its *code*.

## Follow-ups

- **ADR-0002 — Flutter vs. Kotlin/Compose-for-TV** (technology selection).
- Pick the exact permissive license (MIT vs Apache-2.0) and add the `LICENSE` file.
- Begin the PRD ([`../product/PRD.md`](../product/PRD.md)) to lock the MVP scope.
