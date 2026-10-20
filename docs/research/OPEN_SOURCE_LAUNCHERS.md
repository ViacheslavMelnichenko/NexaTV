# Open-Source Android TV Launchers — Research Index

> **Status:** First research pass complete (2026-10-20) — all 6 candidates investigated from their actual
> repositories. Pending human review. Every claim in the linked documents is labelled `FACT`, `INFERENCE`,
> `ASSUMPTION`, or `UNKNOWN`, with a source reference (repo URL + commit/date observed).

## Purpose

Understand the existing Android TV launcher ecosystem so NexaTV's build-vs-fork decision
(see [`../adr/ADR-0001-build-vs-fork.md`](../adr/ADR-0001-build-vs-fork.md)) is grounded in evidence,
not assumption.

## Candidates

| # | Project | Repo | Primary tech | License | Detail doc | Status |
|---|---------|------|--------------|---------|-----------|--------|
| 1 | FLauncher | gitlab.com/flauncher/flauncher | Flutter/Dart + Kotlin | **GPL-3.0** | [FLAUNCHER.md](./FLAUNCHER.md) | ☑ Researched |
| 2 | LTvLauncher | github.com/LeanBitLab/LtvLauncher | Flutter (FLauncher fork) | **GPL-3.0** | [LTVLAUNCHER.md](./LTVLAUNCHER.md) | ☑ Researched |
| 3 | LTvLauncher Extended | github.com/niposch/LTvLauncher-Extended | Flutter (fork) | **GPL-3.0** | [LTVLAUNCHER.md](./LTVLAUNCHER.md) | ☑ Researched |
| 4 | Yukuza Launcher | github.com/urunkarpm/yukuza-launcher | Kotlin / Compose-for-TV | MIT *(README only, no LICENSE file)* | [YUKUZA.md](./YUKUZA.md) | ☑ Researched |
| 5 | Tarang Launcher | github.com/mohitkyadav/tarang-launcher | Kotlin | **None** *(all-rights-reserved)* | [TARANG.md](./TARANG.md) | ☑ Researched |
| 6 | TV Launcher (nielsvanvelzen) | github.com/nielsvanvelzen/tv-launcher | Kotlin / Compose-for-TV | **GPL-3.0** | [TV_LAUNCHER.md](./TV_LAUNCHER.md) | ☑ Researched |
| 7 | LauncherCompose | github.com/karuhun-developer/LauncherCompose | Kotlin / Compose (Now-in-Android) | **Apache-2.0** | [TV_LAUNCHER.md](./TV_LAUNCHER.md) | ☑ Researched |
| 8 | Other active launchers | — | — | — | _as needed_ | ☐ Not started |

> **License takeaway:** every Flutter candidate (FLauncher + all LTvLauncher forks) is **GPL-3.0**, and
> the strongest native candidate (`tv-launcher`) is also **GPL-3.0** — none can be *copied* into a permissive
> NexaTV codebase. The only permissive-reuse options are **LauncherCompose (Apache-2.0)** and, with
> caveats, **Yukuza (MIT declared but missing a LICENSE file)**. See [LICENSE_ANALYSIS.md](./LICENSE_ANALYSIS.md).

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
