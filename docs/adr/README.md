# Architecture Decision Records (ADRs)

ADRs capture **meaningful** architectural decisions and their reasoning. Trivial coding decisions do
**not** get an ADR. Each ADR is immutable once Accepted — supersede it with a new ADR rather than editing.

## Format

```
# ADR-XXXX: Title

## Status
Proposed / Accepted / Superseded / Rejected

## Context
## Decision
## Alternatives
## Consequences
## Rationale
```

## Index

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-0001](./ADR-0001-build-vs-fork.md) | Build vs. Fork | **Accepted** (2026-10-20) — greenfield, permissive license, clean-room references |
| [ADR-0002](./ADR-0002-flutter-vs-kotlin-compose.md) | Flutter vs. Kotlin/Compose-for-TV | **Accepted** (2026-10-20) — Kotlin + Jetpack Compose-for-TV, direct Android APIs (no Flutter) |
| [ADR-0003](./ADR-0003-cicd-signing-release.md) | CI/CD, signing, release & update/rollback | **Accepted** (2026-10-20) — GitHub Actions, tag-driven signed releases, single long-lived key + offline backup, manual sideload updates |
| [ADR-0004](./ADR-0004-app-enumeration-visibility.md) | App enumeration & package visibility | **Accepted** (2026-10-20) — `LauncherApps.getActivityList` via ROLE_HOME, no `QUERY_ALL_PACKAGES` |
| [ADR-0005](./ADR-0005-glass-translucency-technique.md) | "Glass" / translucency technique | **Accepted** (2026-10-20, measurement-gated) — pre-baked "fake glass" default; real-time blur opt-in, gated on-device |

## Planned / potential ADRs (per `initial.md` §25)

Resolved: Flutter vs. Kotlin/Compose (ADR-0002) · Application discovery strategy (ADR-0004) ·
CI/CD, signing & release (ADR-0003) · "Glass"/translucency (ADR-0005).

Still open / potential:

- State management
- Persistence
- Native Android integration
- Launcher HOME strategy (default-Home registration, reboot & crash-recovery behavior — verify on-device)
- Configuration architecture
- Plugin / integration architecture (post-MVP)
