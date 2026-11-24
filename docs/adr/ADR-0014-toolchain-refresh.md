# ADR-0014 — Toolchain and dependency refresh

Date: 2026-11-24. Status: Proposed — CI green required, then on-device acceptance on the TCL.

## Context

The toolchain was pinned to the ADR-0002 validation spike (AGP 8.7, Kotlin 1.9.24, Compose compiler
1.5.14, Compose BOM 2024.06, Gradle 8.9). By November 2026 that is two years behind. Current androidx
releases need compileSdk 35–36, and Kotlin 1.9 prevents adopting them. The owner asked for a full
update rather than minor bumps only.

## Decision

- **Build:** Gradle 9.9.2 and AGP 9.5.2.
  - Use AGP 9's built-in Kotlin. Modules no longer apply `org.jetbrains.kotlin.android`. The root
    build declares it `apply false` only to pin the Kotlin Gradle Plugin version.
  - Kotlin 2.4.21, with Compose compiled by the Kotlin Compose compiler plugin at the same version.
    Strong skipping is therefore on by default.
- **SDK levels:**
  - `compileSdk` 36.
  - `minSdk` and `targetSdk` stay at 34. The single device target is unchanged (ADR-0002), and runtime
    behavior changes of newer target levels are not taken on in this step.
- **Libraries:**
  - Compose BOM 2026.11.01 and tv-material 1.1.0.
  - core-ktx 1.19.1, lifecycle 2.11.1 and activity-compose 1.13.0.
  - DataStore 1.3.0.
  - Protobuf 4.36.2 (lite) with protobuf-gradle-plugin 0.10.1, the first release compatible with AGP 9.
    The persisted schema and wire format are unchanged.
  - Benchmark and baselineprofile 1.5.0.
  - Current test libraries.
- **Static analysis:**
  - detekt stays on 1.23.8, because 2.0 is still alpha.
  - Its embedded compiler refuses a newer Kotlin on its classpath, so the `detekt` configuration is
    pinned to Kotlin 2.0.21.
  - Analysis of the project's Kotlin 2.4 sources is unaffected for the syntax in use.

## Consequences

- No local build step changes for contributors other than needing JDK 17+ and SDK platform 36.
- Revisit the detekt pin when detekt 2.0 is stable.
- Performance NFRs (startup, frame timing) must be re-measured on the TCL with the `:benchmark` module
  before the next release, since both the Compose runtime and compiler changed.
- Rollback is a revert of the single migration commit. The proto schema is untouched, so stored state
  stays readable in both directions.
