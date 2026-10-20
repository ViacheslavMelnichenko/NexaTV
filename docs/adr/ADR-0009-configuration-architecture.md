# ADR-0009: Configuration architecture (user settings)

## Status

**Accepted** — 2026-10-20. First configuration decision for NexaTV. **Builds on (does not supersede)**
[ADR-0008](./ADR-0008-persistence-proto-datastore.md) — settings are additive state in the same Proto
DataStore. Opens the M5 "Cinematic Home & Settings" milestone; its first deliverable (M5.1) is the
settings **foundation**: a reachable Settings screen, a persisted store, and one working parameter
(tile size). Theme and background selection are later M5 slots that extend the same model.

## Context

M5 introduces the first **user-adjustable configuration**. The immediate driver is tile size — the home
grid/favorites tiles are currently fixed `private val`s in `HomeScreen.kt`, and the owner wants to choose
how large they are. More of the same shape follows (theme id, background, favorites sort order), so the
question is not "store one number" but "what is the general configuration seam".

Constraints that shape the choice:

- **FACT (ADR-0008):** a Proto DataStore already backs app state in a single `launcher_state.pb`
  (`LauncherStateProto`), with field tag **4 already reserved for settings** and tag 3 for theme id.
  Adding settings is the additive evolution ADR-0008 explicitly planned for — not a new store.
- **FACT:** there is **no navigation library** (no Compose Navigation / androidx.navigation). The UI is a
  single `MainActivity` that renders one screen via a `when` over `HomeUiState`. DI is manual through the
  Compose `viewModel { }` factory; there is no Hilt/ServiceLocator.
- **FACT:** tile size is consumed on the home render path, so the chosen value must reach `HomeScreen` as a
  `Flow`/state at cold start without blocking (NFR-001/003) — the same path favorites already use.
- **CONSTRAINT (design brief, owner = inspiration not law):** Settings is reached from a **top-right icon**
  on home; the surface is dark, minimal, D-pad-first. Full cinematic top bar (clock/date + Search/Settings/
  Wi-Fi) is a later M5 slot; M5.1 only needs a reachable, forward-compatible entry.
- **CONSTRAINT:** schema will keep growing (theme, background, sort). Each addition must be a defaulted field,
  never a renumber/migration (ADR-0008's additive contract).

The question ADR-0009 answers: **how are user settings modelled, stored, surfaced, and reached** — such that
tile size works today and theme/background/sort drop in later without rework.

## Decision

1. **Reuse the ADR-0008 Proto DataStore; settings are an additive message.** Un-reserve tag `4` in
   `LauncherStateProto` and add `SettingsProto settings = 4;`. No new store, no new file, no migration —
   old `.pb` data reads `settings` as its default. `SettingsProto` v1 carries one field:
   `TileSize tile_size = 1;` (enum: `TILE_SIZE_UNSPECIFIED=0`, `SMALL`, `MEDIUM`, `LARGE`), where
   `UNSPECIFIED` means "owner never chose" and resolves to the **MEDIUM** default. Theme id, background, and
   sort order are future fields (2, 3, …) added the same way.
2. **A `SettingsRepository` mirrors `FavoritesRepository`.** Same store, constructor-injected
   `DataStore<LauncherStateProto>`; exposes `val settings: Flow<Settings>` (a small Android-free domain type)
   and `suspend fun setTileSize(size: TileSize)` via `updateData { it.toBuilder()… }`. Pure mapping logic is
   `internal` and unit-tested with a temp-file store — no device needed (ADR-0008 test pattern).
3. **Settings reach the UI through the existing ViewModel/Flow seam.** `HomeViewModel` gains the settings
   `Flow`; the resolved `TileSize` is part of `HomeUiState.Apps` (or a parallel state) so `HomeScreen`
   renders at the chosen size. No composable ever sees DataStore/Protobuf.
4. **Tile size is a semantic token, not raw dp in the screen.** The S/M/L → (width, height) dp mapping lives
   in one place (a small `TileSizes` map in the theme/ui layer), replacing the loose constants in
   `HomeScreen.kt`. The screen asks for a `TileSize`; the mapping yields dp. This keeps "how big is Large"
   a single edit and lets a future "custom size" slider slot in behind the same token.
5. **Navigation stays state-based (no nav library yet).** A top-level `screen` state in `MainActivity`
   toggles Home ↔ Settings; **BACK** returns to Home. Entry is a **focusable top-right Settings icon** on the
   home surface — the minimal seed of the brief's future top bar. This avoids pulling in Compose Navigation
   for a two-screen app; a nav library becomes its own ADR only if screen count/graph depth justifies it.
6. **Settings screen is a plain D-pad list.** v1 shows one row — "Tile size: Small / Medium / Large" — as
   focusable options that write through the repository immediately (no Save button); the home grid updates
   live via the Flow. New settings are new rows.

## Alternatives

| Decision point | Options (recommended **bold**) | Verdict |
|---|---|---|
| Store | **Extend ADR-0008 Proto DataStore (SettingsProto, tag 4)** · new Preferences DataStore for settings · separate settings store file | **Chosen:** tag 4 is already reserved for exactly this; one store = one serializer, one cold read, additive evolution. A second store/engine duplicates machinery for kilobytes of flags |
| Settings model | **Typed `SettingsProto` + enum** · untyped key/value | **Chosen:** type-safety and defaults scale as theme/background/sort arrive; stringly-typed keys rot (same reasoning that chose Proto over Preferences in ADR-0008) |
| Navigation | **State toggle in MainActivity** · Compose Navigation · separate Activity | **Chosen for now:** two screens don't justify a nav graph; a state toggle is the smallest reversible step. Compose Navigation / multi-activity deferred to a future ADR if routes multiply |
| Tile-size mapping | **Semantic S/M/L token → dp map** · store raw dp in the proto · keep dp constants in the screen | **Chosen:** semantic sizes are stable across densities and let a future "custom" option reuse the token; raw dp in the proto leaks layout into storage |
| Settings entry | **Top-right focusable icon (brief's seed)** · item in the app grid · long-press menu | **Chosen:** matches the design brief and forward-fits the cinematic top bar; a grid item conflates "an app" with "settings", and settings is global, not per-app |

## Consequences

**Positive**
- Tile size works end-to-end today and theme/background/sort are each a defaulted proto field + a list row
  later — no migration, no new store, no nav rework.
- The repository + semantic-token seams keep DataStore and raw dp out of the UI; everything is unit-testable
  without a device, consistent with ADR-0008.
- A reachable Settings screen seeds the M5 cinematic top bar without committing to a nav library prematurely.

**Negative / costs (honest)**
- **State-based navigation has a ceiling.** Two screens are fine; once there are nested routes / deep links /
  a back stack beyond "Settings → Home", this will strain and want a real nav solution (a future ADR — the
  explicit trigger is >2 destinations or a non-trivial back stack).
- **Shared single store:** settings and favorites write the same `.pb`; fine for one process (ADR-0008's
  single-process assumption still holds) but both writers touch one file.
- **Top bar is only seeded, not built:** M5.1 adds a lone Settings icon, so the home layout changes again in
  the later cinematic pass — accepted as incremental.

## Rationale

The configuration need is small, local, structured, schema-evolving, and consumed on the same Flow-based home
path as favorites — the exact profile ADR-0008 already solved. So settings extend that decision additively
(tag 4) rather than introducing a parallel mechanism, and they travel the proven repository → ViewModel →
`Flow` seam to the UI. Navigation is kept at the smallest thing that works (a state toggle) because a
two-screen app does not earn a nav library; that choice is deliberately reversible and carries its own future
trigger. Modelling tile size as a semantic S/M/L token (not raw dp) keeps storage layout-free and leaves room
for a later custom-size control behind the same token. Each piece is the minimum that makes tile size work now
while theme, background, and sort slot in later as field + row additions.

## Follow-ups

- **Schema — done** (`feat`, this milestone): tag 4 un-reserved, `SettingsProto` (`TileSize tile_size = 1;`) +
  enum added additively; serializer round-trip confirms old `.pb` reads the MEDIUM default.
- **Repository + tests — done** (`feat`, this milestone): `SettingsRepository` over the shared store (the store
  delegate extracted into `LauncherStateStore` so favorites + settings share one `.pb`); unit tests cover
  default = MEDIUM, set/round-trip S/M/L, the proto↔domain mapping, and favorites staying untouched.
- **Tile-size token:** replace `HomeScreen.kt`'s `TILE_*` constants with a `TileSize → dp` map; `HomeViewModel`
  exposes the resolved size; grid/favorites render from it.
- **Settings surface:** top-right Settings icon on home (focusable, D-pad), state toggle to a D-pad Settings
  list with the tile-size row writing live; BACK returns home.
