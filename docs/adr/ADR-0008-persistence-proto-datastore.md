# ADR-0008: Local persistence via Proto DataStore

## Status

**Accepted** — 2026-10-20. First persistence decision for NexaTV; nothing is superseded. Accepted on the
owner's explicit choice (2026-10-20) of Proto DataStore over Room and Preferences DataStore, after the
options were laid out. Referenced as a dependency by the M4 favorites/categories work (`project/BACKLOG.md`)
and the Theming EPIC (theme-selection persistence).

## Context

M4 (Application Discovery) introduces the first state that must **survive a restart**: the user's
**favorites** (an *ordered* set of pinned apps) and **basic categories** (user-assigned groupings). Later
milestones add more of the same shape — the selected **theme** (Theming EPIC) and general **settings/layout**
(M6). None of this is large, none is relational-by-nature, and all of it is read on the UI path at cold
start, so reads must be cheap and non-blocking (NFR-001 cold-start budget, NFR-003 off-main work).

Constraints that shape the choice:

- **FACT (`initial.md` / manifest, NFR-021):** NexaTV holds **no `INTERNET` permission** and
  `allowBackup=false` — persistence is strictly **local, on-device**, no cloud/sync surface.
- **FACT:** the data is small and structured: an ordered list of app keys (package/component + user), a
  handful of category assignments, a theme id, a few flags. Typical size is kilobytes.
- **FACT:** the app is Kotlin + coroutines/Flow throughout (ADR-0002); the UI already consumes `Flow`
  (`appChanges()`, `HomeUiState`). A `Flow`-native store composes with no adapter layer.
- **CONSTRAINT:** schema will **evolve** (favorites first, then categories, then theme/config) — the store
  must support additive, versioned change without hand-rolled migration bugs.
- **FACT:** `SharedPreferences` is legacy (synchronous disk I/O on the main thread, no transactional
  guarantees, no first-class Flow) and is not a candidate.

The question ADR-0008 answers: **what local store backs favorites, categories, and later theme/config?**

## Decision

1. **Use Jetpack Proto DataStore** as the single local persistence mechanism for structured app state
   (favorites, categories, theme selection, settings). A Protobuf schema defines the persisted model; a
   generated typed accessor exposes it as `Flow<T>` for reads and a transactional `updateData {}` for writes.
2. **One typed serializer, additive schema evolution.** Each new piece of state is a new field in the proto
   with a safe default — old data deserializes without a migration script, new fields read their default
   until written. This is the specific reason Proto was chosen over Preferences DataStore (below).
3. **Repository seam, not DataStore in the UI.** DataStore is wrapped behind a `data/`-layer repository
   (mirroring `InstalledAppsRepository`/`HomeRoleRepository`), exposing domain types (`AppKey`, `Favorites`)
   and `Flow`s — so the ViewModels and composables never see Protobuf or DataStore APIs and the store stays
   swappable and unit-testable.
4. **All I/O off the main thread.** DataStore does disk work on `Dispatchers.IO` by default; reads reach the
   UI as a cold `Flow` collected in `viewModelScope`. No synchronous reads on the cold-start path (NFR-001/003).
5. **Local only.** The DataStore file lives in app-internal storage; consistent with no-INTERNET and
   `allowBackup=false` (NFR-021). No export/sync is in scope.

## Alternatives

| Option (recommended **bold**) | Verdict |
|---|---|
| **Proto DataStore** | **Chosen.** Type-safe, `Flow`-native, transactional, additive schema evolution via Protobuf defaults; minimal boilerplate for small structured state; composes with the existing coroutine/Flow stack |
| Preferences DataStore | **Rejected.** Same engine but untyped key/value — an *ordered* favorites list must be hand-serialized (CSV/JSON into a string), which is error-prone and loses type-safety as the schema grows. Fine for a flat flag or two, not for the evolving structured model here |
| Room (SQLite) | **Rejected for now.** Powerful, but relational querying is overkill for a kilobyte-scale ordered list + a few assignments; more code, a DAO/entity layer, and migration machinery for no current benefit. Reconsider only if categories grow into rich queryable metadata (filters/joins) — a future ADR can supersede this |
| SharedPreferences | **Rejected.** Legacy: main-thread synchronous I/O, no transactions, no Flow; explicitly superseded by DataStore |
| Hand-rolled JSON file | **Rejected.** Reimplements what DataStore already gives (atomic writes, corruption handling, Flow) with more bugs |

## Consequences

**Positive**
- Type-safe, transactional local state that reads as a `Flow` straight into the existing ViewModels — no
  adapter layer, no main-thread I/O, within the cold-start/NFR budgets.
- Additive Protobuf evolution means adding categories, then theme, then settings is a field addition with a
  default — not a migration script per change.
- The repository seam keeps DataStore an implementation detail: swappable (e.g. to Room later) and unit-testable
  without a device.

**Negative / costs (honest)**
- **Protobuf toolchain:** adds the `protobuf` Gradle plugin, a `.proto` schema, and codegen to the build —
  a one-time setup cost and a new moving part in CI.
- **Boilerplate for the serializer:** Proto DataStore needs a `Serializer<T>` and a typed accessor; slightly
  more upfront than Preferences DataStore (the price of type-safety).
- **Single-process assumption:** DataStore is single-process; fine here (one launcher process) but worth
  noting if a future background component ever writes the same store.
- **Not relational:** if categories later need cross-entity queries, Proto will feel strained — that is the
  documented trigger to revisit via Room (a superseding ADR).

## Rationale

The state to persist is small, local, structured, and schema-evolving, and it is consumed on a Flow-based UI
path with a tight cold-start budget. Proto DataStore matches every one of those: typed and transactional
(unlike Preferences DataStore's hand-serialized strings), Flow-native and off-main-thread by default (unlike
SharedPreferences), and far lighter than Room for kilobyte-scale ordered data. Room's relational power buys
nothing for an ordered favorites list today, so it is deferred — not dismissed — with a clear trigger
(queryable category metadata) for a future superseding ADR. Wrapping it behind a repository keeps the choice
reversible.

## Follow-ups

- **Schema v1:** define the initial `.proto` (favorites = ordered list of app keys; category assignments;
  reserved fields for theme id + settings) and the `Serializer<T>` with safe defaults.
- **Repository + tests:** add the favorites repository over DataStore with unit tests (add/remove/reorder,
  default-empty, round-trip) using an in-memory/temp-file DataStore — no device needed.
- **App-key identity:** decide the stable key for a favorite (component + user handle) so reorder/pin survives
  app updates; record it with the schema.
- **CI:** confirm the protobuf plugin builds under the ADR-0003 pipeline (codegen on the CI runner).
