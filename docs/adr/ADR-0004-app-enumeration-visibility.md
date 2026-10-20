# ADR-0004: Application enumeration & package visibility (no QUERY_ALL_PACKAGES)

## Status

**Accepted** — 2026-10-20. (Implements PRD v2 FR-015/FR-016 and reinforces the privacy posture
NFR-006/NFR-021. Accepted under the owner's standing delegation on non-blocking choices; the decision is
forced by platform policy + the project's privacy stance, so there is no genuine owner trade-off to defer.)

## Context

A launcher must enumerate installed, launchable apps (FR-002). On Android 11+ (API 30+) this collides with
**package visibility filtering**, so *how* NexaTV sees other apps is an explicit decision, not an incidental
API call.

- **FACT (SDK 34):** the device is API 34 → full package-visibility filtering applies; a naive
  `PackageManager.getInstalledApplications()` returns a **filtered** list.
- **FACT:** `QUERY_ALL_PACKAGES` is a **sensitive, policy-gated** permission (Google Play restricts it to a
  short approved-use list; it is broad and privacy-hostile). It directly contradicts NFR-006 (privacy) and
  NFR-021 (least privilege) and is **not needed by a launcher**.
- **FACT:** `LauncherApps` is the first-party API **designed for launchers**. The **default Home app**
  (the holder of `RoleManager ROLE_HOME`) is exempt from package-visibility filtering for launchable
  activities, so `LauncherApps.getActivityList(null, user)` returns the full set of launchable activities —
  **without** `QUERY_ALL_PACKAGES`. `LauncherApps` is also **user-profile aware** (work/▮ secondary
  profiles) and exposes a `Callback` for live add/remove/change events.
  *(INFERENCE on the exact exemption boundary → confirmed on-device in the ADR-0002 spike / FR-015 acceptance:
  "with ROLE_HOME granted, launchable count == stock launcher's, delta 0".)*
- **FACT:** before NexaTV is set as default Home, visibility may be limited → a partial/empty grid would be
  a confusing failure, not a correct state (FR-015 mandates an explicit first-run prompt instead).

## Decision

1. **Primary enumeration API = `LauncherApps.getActivityList(null, userHandle)`** across the current user's
   profiles (`LauncherApps.getProfiles()` / `UserManager`), de-duplicated. This yields launchable activities
   with label + icon + component directly, and relies on **ROLE_HOME-granted visibility** — **no**
   `QUERY_ALL_PACKAGES`.
2. **Live updates via `LauncherApps.Callback`** (`onPackageAdded/Removed/Changed/…`), not polling; this
   satisfies the FR-002 "appears/disappears ≤2 s without restart" criterion. (`ACTION_PACKAGE_*` broadcasts
   are a fallback only if a gap is found on this firmware.)
3. **Do NOT declare `QUERY_ALL_PACKAGES`.** Do **not** rely on broad `PackageManager` enumeration for the
   app list.
4. **Targeted visibility via a minimal `<queries>` manifest element** *only* for specific intents NexaTV must
   resolve (e.g. `ACTION_APPLICATION_DETAILS_SETTINGS` for FR-017 "App info", or a specific launch intent) —
   each entry justified in docs. This is the least-privilege alternative to a blanket query.
5. **Pre-Home state (FR-015):** until NexaTV holds `ROLE_HOME`, show an explicit "Set NexaTV as default Home
   to see all apps" state — never a silently partial or empty grid.
6. **Exclusions (FR-016):** filter out no-launch, disabled, suspended packages, NexaTV itself, and
   (configurable) the stock launcher; all work happens off the UI thread (NFR-003).

## Alternatives

| Option (recommended **bold**) | Visibility cost | Verdict |
|---|---|---|
| **`LauncherApps.getActivityList` + `ROLE_HOME` exemption** | None — no sensitive permission | **Chosen.** First-party launcher API, profile-aware, live callbacks, zero privacy cost |
| `QUERY_ALL_PACKAGES` | A sensitive, policy-gated, privacy-hostile permission | **Rejected.** Contradicts NFR-006/021; unnecessary for a launcher; Play-restricted |
| `PackageManager.queryIntentActivities(LAUNCHER)` + narrow `<queries>` | A `<queries>` intent filter | Workable fallback, but `LauncherApps` is the leanback-correct, profile-aware, live-update API → used only as a diagnostic fallback |
| Broad `PackageManager.getInstalledApplications()` | Returns a filtered list without the permission | **Rejected.** Returns incomplete results under visibility filtering; wrong tool for launchable apps |

## Consequences

**Positive**
- **Zero** sensitive permissions for the core feature → NFR-006/021 upheld structurally; no Play-policy risk
  if ever distributed more widely.
- Profile-aware and event-driven out of the box (meets FR-002's live-update latency, FR-016 exclusions).
- Visibility is **coupled to being the Home app**, which is exactly NexaTV's role — semantically correct.

**Negative / costs (honest)**
- **Full app visibility depends on being the default Home.** Before that, the list is limited → mitigated by
  the explicit first-run prompt (FR-015), not a silent partial list.
- `LauncherApps` behavior/exemption must be **verified on this TCL firmware** (INFERENCE until the spike).
- Any future need to resolve a *non-launchable* package (e.g. a settings deep-link) requires an explicit
  `<queries>` entry — a small, deliberate, documented addition rather than a blanket permission.

## Rationale

The platform already provides the exact, privacy-preserving tool a launcher is supposed to use: a Home app
gets launchable-activity visibility through `LauncherApps` **by virtue of its role**, so the broad,
sensitive `QUERY_ALL_PACKAGES` is simply unnecessary — using it would trade the project's privacy posture for
nothing. Coupling visibility to the Home role is also honest about the one edge it creates (pre-Home state),
which we handle with a clear prompt rather than a confusing empty grid. Narrow `<queries>` entries keep any
future targeted-intent needs least-privilege and auditable.

## Follow-ups

- **In the ADR-0002 validation spike / FR-015 acceptance:** confirm on the real TCL that, with `ROLE_HOME`
  granted, `getActivityList` returns a launchable count equal to the stock launcher's (delta 0), and that the
  `LauncherApps.Callback` fires on install/uninstall within the ≤2 s budget.
- Record any firmware-specific quirk in `docs/technical/ANDROID-TV.md`; if `LauncherApps` proves incomplete
  on this unit, document the `<queries>`+`PackageManager` fallback actually used.
- When FR-017 (App info / Uninstall) is scheduled, add the specific `<queries>` intents it needs and justify
  each.
