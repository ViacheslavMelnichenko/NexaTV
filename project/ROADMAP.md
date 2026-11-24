# NexaTV — Roadmap

This file is the single source for **milestone status and open work**. It replaces the former
`MILESTONES.md` and `BACKLOG.md`, whose history is in git.

Where details live:
- Decisions: [ADRs](../docs/adr/README.md).
- What shipped in each release: the [CHANGELOG](../CHANGELOG.md).
- On-device evidence: [ANDROID-TV.md](../docs/technical/ANDROID-TV.md) §3a–§3c.

> Status legend: ☑ Done · ◐ Done, owner check pending · ⏸ Deferred (owner paused, not on the agenda).

## Milestones

| ID | Milestone | Status | Evidence / decisions |
|----|-----------|--------|----------------------|
| M0 | Discovery & device audit | ☑ 2026-10-20 | Read-only TCL audit (ANDROID-TV.md); OSS launcher research folded into ADR-0001 |
| M1 | Architecture & foundation | ☑ 2026-10-20 | ADR-0001…0005; Apache-2.0; Kotlin + Compose-for-TV scaffold |
| M2 | Minimal launcher | ☑ 2026-10-20 | `LauncherApps` grid, D-pad focus, launch on OK, Loading/Empty/error states (ADR-0004); verified on the TCL (ANDROID-TV.md §3a) |
| M3 | HOME integration | ☑ 2026-10-20 | Firmware blocks `ROLE_HOME` (ADR-0006), so NexaTV uses a reversible accessibility override (ADR-0007); verified on the TCL (§3b–§3c) |
| M4 | Application discovery | ☑ core | Live add/remove, Proto DataStore (ADR-0008), All apps screen, favorites, Home widgets (ADR-0010). "Basic categories" ⏸ |
| — | Design System & Theming | ☑ | [DESIGN-SYSTEM.md](../docs/design/DESIGN-SYSTEM.md): four themes, S/M/L tiles (ADR-0009, ADR-0012) |
| — | Adaptive backgrounds | ☑ v0.3.0 | ADR-0013; real-TV performance acceptance pending |
| — | CI & release pipeline | ☑ | ADR-0003. Releases v0.1.0 to v0.3.2 are tagged. Toolchain refreshed in ADR-0014 |
| M5 | Production TV UX | ⏸ | |
| M6 | Configuration & customization | ⏸ | Partly delivered early (settings, themes, backgrounds) |
| M7 | Media / NAS integrations | ⏸ | |
| M8 | Advanced integrations | ⏸ | Weather was delivered early (ADR-0010) |

## Pending owner verification (on the TCL)

These need the owner's eyes on the real device. Nothing here is blocked on code.

- ◐ **Favorites UI.** Check pin/unpin, the long-press context menu and **Перемістити** reorder visually.
- ◐ **v0.3.2 and later fixes.** The 0.3.2 crash fix and the unreleased changes have not been smoke-tested on
  the TCL. Check that tile names fit on one line, that OK after a reorder does not open the menu, and that
  focus stays in the row after an unpin.
- ◐ **Toolchain refresh (ADR-0014).** Run a smoke test, then re-measure cold start and frame timing with
  [`:benchmark`](../benchmark/README.md) against the baseline below.
- ◐ **Adaptive backgrounds (ADR-0013).** Run the real-TV performance acceptance
  ([checklist](../docs/testing/ADAPTIVE-BACKGROUND-PREVIEW.md)).

## Open / deferred work

- ⏸ **Basic categories (M4).** Group apps (media / games / system) for faster D-pad reach, persisted in the
  ADR-0008 store.
- ⏸ **ADR-0007 follow-ups.** One is an accessibility-consent onboarding from the pre-Home prompt. The other
  is a fallback if an OTA breaks the override.
- ⏸ **NFR-017 glass fps/PSS gate.** Needs the real glass UI, so it is deferred with M5+.
- **`:benchmark` install wiring.** The connected task does not install the measured app. See the
  [known issue](../benchmark/README.md#known-issue-automated-install).
- ⏸ **Milestones M5–M8.**

## Performance baseline (2026-10-20, TCL G08)

Measured with Macrobenchmark on the debug-era build, before the ADR-0014 toolchain refresh. These are
the figures to compare against.
- **Cold start (NFR-001).** `COLD`, `CompilationMode.None`, 10 iterations: min 938, median 984 and
  max 1347 ms. The budget is ≤ 2.0 s p50.
- **Idle PSS (NFR-002).** About 48.6 MB.
- **Device.** 4 × 1.39 GHz, 2.5 GB RAM, `armeabi-v7a`, 60 Hz.
