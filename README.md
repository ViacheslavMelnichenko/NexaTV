# NexaTV

A custom, fast, D-pad-first **Android TV launcher** targeting a TCL Smart TV (Android TV OS 14).

> **Status:** Phase 0 — Discovery. No launcher code is being written yet. The current goal is to
> understand the existing open-source ecosystem and the target device, then make an explicit,
> documented architecture decision (build vs. fork) before implementation begins.

## Vision

Deliver a polished Home experience that is faster, cleaner, and more customizable than the stock
Google TV / TCL launcher — remote-first, offline-first, privacy-friendly, and low-resource.

See [`initial.md`](./initial.md) for the full project master instructions (the source of truth for
scope, principles, and process).

## Engineering process

```
RESEARCH → REQUIREMENTS → ARCHITECTURE → DECISION → PLAN → IMPLEMENT
  → BUILD → INSTALL → TEST ON REAL TV → OBSERVE → LOG/EVIDENCE → FIX → DOCUMENT → COMMIT
```

AI-generated code is acceptable. AI-generated architecture *without reasoning* is not. Decisions are
captured in ADRs; research is backed by evidence, not memory.

## Repository layout

| Path | Purpose |
|------|---------|
| `initial.md` | Project master instructions (constitution). |
| `CLAUDE.md` | Operating rules for AI-assisted work in this repo. |
| `docs/research/` | Open-source launcher research, feature matrix, license & architecture comparison. |
| `docs/adr/` | Architecture Decision Records. |
| `docs/product/` | Product Requirements Document (PRD). |
| `docs/technical/` | ADB reference and Android TV device/launcher notes. |
| `docs/testing/` | Test plan (unit, widget, integration, manual TV scenarios). |
| `project/` | Backlog and milestones. |

## Current phase checklist (M0 — Discovery & Device Audit)

1. Establish ADB connectivity to the TCL TV
2. Perform a safe, read-only device audit
3. Research existing launchers (FLauncher, LTvLauncher & forks, Yukuza, Tarang, TV Launcher, LauncherCompose)
4. Analyze licenses
5. Analyze architectures
6. Build the feature matrix
7. Compare architectures
8. Author ADR-0001 (Build vs. Fork)
9. Decide the implementation direction
10. Only then create the NexaTV code foundation

## Safety

The TV is a **daily-use production device**. All early work is read-only. The stock launcher is never
disabled, nothing is uninstalled, and the first NexaTV install must be reversible. Runtime/pairing
information (IP, port, pairing codes) is **never** committed. See `initial.md` §16.

## License

TBD — to be chosen as part of the architecture decision (see `docs/research/LICENSE_ANALYSIS.md`).
