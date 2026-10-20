# Architecture Comparison

> **Status:** ☐ Not started. Feeds directly into [ADR-0001 (Build vs. Fork)](../adr/ADR-0001-build-vs-fork.md).
> Keep claims evidence-based; label `FACT` / `INFERENCE` / `ASSUMPTION` / `UNKNOWN`.

## 1. Per-project architecture summary

| Project | UI stack | State mgmt | Native/Android bridge | Persistence | Modularity | Notable strengths | Notable risks |
|---------|----------|------------|-----------------------|-------------|------------|-------------------|---------------|
| FLauncher | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ |
| LTvLauncher (+Extended) | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ |
| Yukuza | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ |
| Tarang | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ |
| TV Launcher | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ |
| LauncherCompose | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ | _UNKNOWN_ |

## 2. Strategic options (per `initial.md` §10)

### Option A — Fork FLauncher / LTvLauncher
- **Pros:** large existing codebase, existing TV-specific behavior, Flutter already used, faster initial progress.
- **Cons:** license implications, inherited architecture & technical debt, upstream divergence, customization friction.
- **Evidence / assessment:** _TBD_

### Option B — Fork another existing launcher
- **Candidate(s):** _TBD_
- **Pros / Cons / assessment:** _TBD_

### Option C — Greenfield (Flutter + Android native integration)
- **Pros:** full control, clean architecture, no inherited debt.
- **Cons:** significantly more work, must reimplement Android TV edge cases.
- **Evidence / assessment:** _TBD_

### Option D — Hybrid (references + selective, license-cleared reuse; independent architecture)
- Currently the **preferred hypothesis**, not a decision.
- **Evidence / assessment:** _TBD_

## 3. Technology axis — Flutter vs. Kotlin/Compose

| Criterion | Flutter | Kotlin / Compose(-for-TV) |
|-----------|---------|---------------------------|
| D-pad / focus handling maturity | _UNKNOWN_ | _UNKNOWN_ |
| Native Android TV API access | _UNKNOWN_ (via platform channels) | _UNKNOWN_ (direct) |
| Startup / memory on TCL hardware | _UNKNOWN_ (measure on device) | _UNKNOWN_ (measure on device) |
| UI iteration speed | _UNKNOWN_ | _UNKNOWN_ |
| Ecosystem / reference projects | _UNKNOWN_ | _UNKNOWN_ |
| User's existing setup | Flutter already installed (FACT, per initial.md) | Would require Android/Kotlin toolchain setup |

→ Final technology choice recorded in a dedicated ADR (Flutter vs. Kotlin/Compose).

## 4. Preliminary recommendation
_To be written only after sections 1–3 are evidence-backed. Do not pick the easiest option by default._
