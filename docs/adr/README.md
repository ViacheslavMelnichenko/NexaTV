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

## Planned / potential ADRs (per `initial.md` §25)

- Flutter vs. Kotlin/Compose
- State management
- Persistence
- Native Android integration
- Application discovery strategy
- Launcher HOME strategy
- Configuration architecture
- Plugin / integration architecture
