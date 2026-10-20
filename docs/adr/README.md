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
| [ADR-0001](./ADR-0001-build-vs-fork.md) | Build vs. Fork | **Accepted** (2026-10-20) — greenfield, permissive license (**Apache-2.0**, chosen 2026-10-20), clean-room references |
| [ADR-0002](./ADR-0002-flutter-vs-kotlin-compose.md) | Flutter vs. Kotlin/Compose-for-TV | **Accepted** (2026-10-20) — Kotlin + Jetpack Compose-for-TV, direct Android APIs (no Flutter) |
| [ADR-0003](./ADR-0003-cicd-signing-release.md) | CI/CD, signing, release & update/rollback | **Accepted** (2026-10-20) — GitHub Actions, tag-driven signed releases, single long-lived key + offline backup, manual sideload updates |
| [ADR-0004](./ADR-0004-app-enumeration-visibility.md) | App enumeration & package visibility | **Accepted** (2026-10-20) — `LauncherApps.getActivityList` via ROLE_HOME, no `QUERY_ALL_PACKAGES` |
| [ADR-0005](./ADR-0005-glass-translucency-technique.md) | "Glass" / translucency technique | **Accepted** (2026-10-20, measurement-gated) — pre-baked "fake glass" default; real-time blur opt-in, gated on-device |
| [ADR-0006](./ADR-0006-home-role-registration.md) | HOME registration, pre-Home prompt & reversibility | **Accepted** (2026-10-20) — pre-Home prompt gates the grid; reversible, no boot receiver. **Become-Home mechanism superseded by [ADR-0007](./ADR-0007-home-override-accessibility.md)** on Google TV firmware that blocks `ROLE_HOME` replacement |
| [ADR-0007](./ADR-0007-home-override-accessibility.md) | Becoming HOME via an accessibility override | **Accepted** (2026-10-20, device-validated) — Google TV blocks third-party `ROLE_HOME`; `NexaOverrideService` raises NexaTV when the stock launcher appears; owner-enabled in Settings → Accessibility, reversible, stock launcher never disabled |
| [ADR-0008](./ADR-0008-persistence-proto-datastore.md) | Local persistence | **Accepted** (2026-10-20) — **Proto DataStore** for favorites/categories/theme/settings; typed, `Flow`-native, additive schema evolution; local only (no INTERNET, `allowBackup=false`); Room deferred until categories need relational queries |
| [ADR-0009](./ADR-0009-configuration-architecture.md) | Configuration architecture (user settings) | **Accepted** (2026-10-20) — settings as an additive `SettingsProto` in the ADR-0008 store (tag 4); `SettingsRepository` + Flow seam; state-based Home↔Settings nav (no nav library yet); tile size as a semantic S/M/L token; opens M5 |
| [ADR-0010](./ADR-0010-weather-widget-network.md) | Home weather widget & the first outbound network call | **Accepted** (2026-10-20) — **amends NFR-021**: adds `INTERNET` for weather only (no telemetry/other network); Open-Meteo (keyless, no secret); `HttpURLConnection`+`org.json` (no new dep); fixed Wrocław coords (no GPS); lazy, fail-soft off the cold-start path |

## Planned / potential ADRs (per `initial.md` §25)

Resolved: Flutter vs. Kotlin/Compose (ADR-0002) · Application discovery strategy (ADR-0004) ·
CI/CD, signing & release (ADR-0003) · "Glass"/translucency (ADR-0005) ·
Launcher HOME strategy (ADR-0006 — registration & pre-Home prompt; ADR-0007 — accessibility override after
on-device validation showed Google TV blocks `ROLE_HOME` replacement) · Persistence (ADR-0008 — Proto DataStore).

Resolved (M5): Configuration architecture (ADR-0009 — settings as additive Proto DataStore state, accepted).

Still open / potential:

- State management
- Native Android integration
- Plugin / integration architecture (post-MVP)
