# Open-Source Android TV Launchers — Research Index

> **Status:** Not started. This is the entry point for Phase 0 discovery research.
> Every claim recorded here or in linked documents must be labelled `FACT`, `INFERENCE`,
> `ASSUMPTION`, or `UNKNOWN`, with a source reference (repo URL + commit/date observed).

## Purpose

Understand the existing Android TV launcher ecosystem so NexaTV's build-vs-fork decision
(see [`../adr/ADR-0001-build-vs-fork.md`](../adr/ADR-0001-build-vs-fork.md)) is grounded in evidence,
not assumption.

## Candidates

| # | Project | Repo (to confirm) | Primary tech (to confirm) | License (to confirm) | Detail doc | Status |
|---|---------|-------------------|---------------------------|----------------------|-----------|--------|
| 1 | FLauncher | UNKNOWN | UNKNOWN | UNKNOWN | [FLAUNCHER.md](./FLAUNCHER.md) | ☐ Not started |
| 2 | LTvLauncher | UNKNOWN | UNKNOWN | UNKNOWN | [LTVLAUNCHER.md](./LTVLAUNCHER.md) | ☐ Not started |
| 3 | LTvLauncher Extended | UNKNOWN | UNKNOWN | UNKNOWN | [LTVLAUNCHER.md](./LTVLAUNCHER.md) | ☐ Not started |
| 4 | Yukuza Launcher | UNKNOWN | UNKNOWN | UNKNOWN | [YUKUZA.md](./YUKUZA.md) | ☐ Not started |
| 5 | Tarang Launcher | UNKNOWN | UNKNOWN | UNKNOWN | [TARANG.md](./TARANG.md) | ☐ Not started |
| 6 | TV Launcher (nielsvanvelzen) | UNKNOWN | UNKNOWN | UNKNOWN | [TV_LAUNCHER.md](./TV_LAUNCHER.md) | ☐ Not started |
| 7 | LauncherCompose | UNKNOWN | UNKNOWN | UNKNOWN | _TBD_ | ☐ Not started |
| 8 | Other active launchers found during research | — | — | — | _TBD_ | ☐ Not started |

## Research objectives (applied to each project)

- **A. License** — type, commercial use, fork/redistribution/source-disclosure/attribution, compatibility.
- **B. Technology** — Kotlin/Java/Flutter/Compose, Android APIs, plugins, platform channels, storage, state, DI, networking, persistence.
- **C. Architecture** — structure, layers, modules, responsibilities, Android integration, UI, state, persistence, platform abstraction.
- **D. TV-specific** — HOME intent, launcher registration, app discovery/launch, D-pad/focus/remote keys, TV restrictions, input handling, uninstall/app info, system settings.
- **E. UX** — layout, navigation, focus indicators, animations, categories, customization, app management, accessibility.
- **F. Quality** — tests, CI, release process, benchmarks, code quality, issue activity, maturity, maintenance status.
- **G. Limitations** — what each project does poorly or does not support.

## Cross-cutting outputs

- [FEATURE_MATRIX.md](./FEATURE_MATRIX.md) — feature comparison across all candidates.
- [LICENSE_ANALYSIS.md](./LICENSE_ANALYSIS.md) — consolidated license findings and reuse implications.
- [ARCHITECTURE_COMPARISON.md](./ARCHITECTURE_COMPARISON.md) — side-by-side architecture evaluation.
