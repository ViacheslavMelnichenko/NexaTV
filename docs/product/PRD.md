# NexaTV — Product Requirements Document (PRD)

> **Status:** Draft v1 for review — 2026-10-20. Authored after Phase 0 research, ADR-0001 (greenfield,
> permissive), ADR-0002 (Kotlin + Jetpack Compose-for-TV), and the Apache-2.0 license decision.
> Requirements use **stable IDs** (`FR-00x`, `NFR-00x`) that must **not** be renumbered once assigned.
> Evidence labels: `FACT` (verified), `ASSUMPTION` (plausible, unverified), `UNKNOWN` (open).

## 1. Problem statement

The TCL unit ships with the stock Google TV / TCL launcher as its Home experience. The project owner's
motivation (`initial.md` §1) is that it does **not** provide the Home experience they want: it is
content/recommendation-and-ad-oriented rather than an app-first, fast, fully owner-controlled surface.

- `ASSUMPTION` (owner-stated, not yet itemised against this firmware): the stock launcher emphasises
  promoted content/ads and cloud recommendations over a clean, predictable app grid, and offers limited
  layout/favorites control. Concrete, reproducible pain points will be recorded during the device-safety
  work (see Follow-ups) so this section becomes `FACT`.

NexaTV replaces the Home experience with a **polished, fast, D-pad-first, offline-first, ad-free launcher**
the owner fully controls — standing alongside the stock launcher (installed reversibly, never disabling or
removing system apps per `initial.md` §16).

## 2. Vision

A polished, fast, customizable, remote-first Android TV Home experience — offline-first and
privacy-friendly — that is a better daily Home than the stock launcher on the owner's own hardware.

## 3. Goals

- G1 — Be a **reliable, always-available Home**: register as a HOME app and start cleanly, including after
  reboot, without destabilising the device.
- G2 — Make launching installed apps **faster and more predictable** than the stock launcher, with
  always-obvious D-pad focus.
- G3 — Give the owner **control**: favorites, basic categories, simple settings, persisted across reboots.
- G4 — Stay **lean** on the audited 32-bit / ~2.34 GiB device (low memory, fast startup).
- G5 — Be **offline-first and ad-free / privacy-friendly** — no mandatory accounts, no default telemetry.

## 4. Non-goals (MVP)

- Phone/tablet/touch/mouse support — **TV + D-pad only**.
- Mandatory cloud accounts, advertising, or default analytics/telemetry.
- Replacing or disabling the stock launcher, uninstalling system apps, or modifying system partitions.
- Media/NAS/Jellyfin/Plex/Kodi/Home Assistant integrations, Continue Watching, weather, screensaver,
  notifications — all **post-MVP** (§14), evaluated separately.
- Multi-device / non-TCL hardware support (nice-to-have later; not an MVP constraint).

## 5. Target environment

Primary device — TCL Smart TV, Android TV OS 14 (audited specs in
[ANDROID-TV.md](../technical/ANDROID-TV.md)). The requirements below are weighed against these `FACT`s:

| Property | Value | Relevance |
|---|---|---|
| OS / SDK | Android TV OS 14 / SDK 34 | `RoleManager ROLE_HOME`, `LauncherApps`, `tv-material` all available |
| CPU ABI | **armeabi-v7a only** (no arm64) | Pure-Kotlin/Compose app ships no `.so` → ABI-agnostic (ADR-0002) |
| Cores / RAM | 4 cores / ~2.34 GiB | Launcher is always-resident → memory budget matters (NFR-002) |
| Display | 4K panel, **UI at 1080p**, density 320 | Design target is 1080p 10-foot UI |
| Input | Physical remote / D-pad only | No touch, no mouse (UX §9) |
| Recovery | `com.android.tv.settings/.system.FallbackHome` | Safe fallback exists → install must stay reversible (NFR-007) |

**Tech stack (FACT, per ADRs):** greenfield Kotlin + Jetpack Compose-for-TV (`androidx.tv:tv-material`),
direct Android APIs, Apache-2.0 licensed, no Flutter.

## 6. Personas

- **The Owner (primary)** — expert .NET/C# engineer, first Android project; wants a fast, clean,
  controllable Home and will be the main daily user and the developer.
- **Household viewer (secondary)** — non-technical; must be able to find and launch apps with the remote
  without instructions; predictable focus is essential.

## 7. Functional requirements (FR)

> Derived from `initial.md` §1 capability list and the §13 MVP. FR-001..FR-009 are the **MVP**.
> Acceptance criteria are verification-ready; final numeric targets (startup, memory) are set by NFRs.

| ID | Requirement | Priority | MVP? | Acceptance criteria | Status |
|----|-------------|----------|------|---------------------|--------|
| FR-001 | Register as an Android HOME app | P0 | Yes | App declares a HOME launcher Activity (MAIN + HOME + DEFAULT + LEANBACK_LAUNCHER). User can set NexaTV as default Home via `RoleManager ROLE_HOME` / system picker. Pressing HOME opens NexaTV. **Stock launcher is never disabled; user can switch back at any time.** | ☐ |
| FR-002 | Discover installed applications | P0 | Yes | On launch, NexaTV lists launchable apps via `LauncherApps`/`PackageManager` (LEANBACK_LAUNCHER ∪ LAUNCHER, de-duplicated), with label + icon. Live-updates on install/uninstall/enable/disable (`ACTION_PACKAGE_*`) without a manual refresh. Discovery runs off the UI thread. | ☐ |
| FR-003 | Launch an application | P0 | Yes | Selecting an app (OK) starts its leanback/main launch intent; a failed launch shows a non-crashing message and keeps focus. | ☐ |
| FR-004 | Favorites | P0 | Yes | User can mark/unmark any app as favorite (e.g. long-press → menu). Favorites appear in a dedicated, first-focused row/section and persist across restart/reboot. | ☐ |
| FR-005 | Basic categories | P0 | Yes | Apps are grouped into at least Favorites + All apps (plus auto-grouping where the system category is available). Grouping is predictable and stable between launches. | ☐ |
| FR-006 | D-pad navigation & focus handling | P0 | Yes | Entire UI is operable with Up/Down/Left/Right/OK/Back/Home only. Focus is **always visible**, movement is predictable, and the last focused item is restored when returning to a screen. No focus traps/dead-ends. (Validated on the physical remote — see TEST-PLAN.) | ☐ |
| FR-007 | Simple settings | P0 | Yes | A settings screen reachable by remote lets the user change at least one real preference (e.g. layout/sort or favorites management) and open the system "set default Home" flow. | ☐ |
| FR-008 | Persistence of user configuration | P0 | Yes | Favorites, ordering, and settings survive app restart **and** TV reboot (local storage; no cloud/account). | ☐ |
| FR-009 | Reliable startup (incl. after reboot) | P0 | Yes | NexaTV cold-starts to an interactive, focused Home within the NFR-001 budget; after a TV reboot it comes up as Home (when set as default) without crash/ANR; if app discovery is slow, UI appears first and populates progressively. | ☐ |
| FR-010+ | Post-MVP: app reordering, recents, Continue Watching, clock, weather, backgrounds, OLED screensaver, media/NAS/Jellyfin/Plex/Kodi, Home Assistant, notifications, input shortcuts, animations | P1–P2 | No | Specified separately when scheduled (§14 / MILESTONES M5+). | ☐ |

## 8. Non-functional requirements (NFR)

> Numeric targets are **initial budgets** to confirm/adjust against a baseline measured on the real TCL
> (`UNKNOWN` until the validation spike runs — ADR-0002 Follow-ups). Measure on-device, never an emulator.

| ID | Requirement | Target / measure | Status |
|----|-------------|------------------|--------|
| NFR-001 | Fast startup | Cold start to interactive, focused Home **≤ 2.0 s** on the TCL (p50) / ≤ 3.0 s (p95); measured via Macrobenchmark + Baseline Profiles. Budget confirmed after baseline. | ☐ |
| NFR-002 | Low memory footprint | Idle resident set small enough to avoid being killed under pressure; **target ≤ ~150 MB** idle PSS on this ~2.34 GiB device (confirm vs. baseline). No leaks across navigation. | ☐ |
| NFR-003 | Responsive during discovery | No UI-thread blocking: discovery/icon work off-main; interaction stays responsive (no ANR; dropped-frame budget met) while the app list loads. | ☐ |
| NFR-004 | Offline-first core | FR-001..FR-009 work fully with **no network**; no feature on the MVP path requires internet or an account. | ☐ |
| NFR-005 | Reliable after reboot | After reboot (as default Home), NexaTV reaches interactive Home with no crash/ANR across repeated reboots; persisted state intact. | ☐ |
| NFR-006 | Privacy-friendly / ad-free | **Zero** advertising; **no** analytics/telemetry by default; no account required; no personal data leaves the device. | ☐ |
| NFR-007 | Reversible install / safe fallback | Installed as a normal user app; setting/unsetting as Home is user-reversible; stock launcher remains installed and selectable; uninstalling NexaTV restores the prior Home. Verified before any Home-registration test. | ☐ |

## 9. UX principles

Remote-first; **focus always obvious and movement predictable**; no mobile-first patterns. All major
functionality usable from the remote (Up/Down/Left/Right/OK/Back/Home, long-press where useful). 10-foot
readability at 1080p. Built on first-party TV focus primitives (`focusable`, `FocusRequester`,
`FocusGroup`, `onPreviewKeyEvent`) per ADR-0002.

## 10. Constraints

- `FACT` — 32-bit `armeabi-v7a` only; ~2.34 GiB RAM; 1080p UI; SDK 34 (§5). Favor lean implementation.
- `FACT` — License: Apache-2.0; greenfield; GPL launchers are clean-room references only (no code reuse).
- `FACT` — Device safety (`initial.md` §16): read-only first; never disable the stock launcher, uninstall
  system apps, or modify system partitions; first install must be reversible.
- `UNKNOWN` — Exact firmware behavior for default-Home selection, reboot-as-Home, and crash recovery on this
  TCL build; must be verified on-device before relying on it.

## 11. Assumptions

- `ASSUMPTION` — `RoleManager ROLE_HOME` / the system default-app picker is available and functional on this
  TCL build (to verify).
- `ASSUMPTION` — `tv-material` focus behaves correctly on this SDK-34 unit (to confirm in the ADR-0002 spike).
- `ASSUMPTION` — The owner's C#→Kotlin transfer keeps the single-stack learning curve a schedule risk, not a
  blocker.

## 12. Risks

- HOME-app behavior differs across firmware (default selection, reboot, crash-recovery) → mitigate with
  on-device verification and never disabling the stock launcher.
- Launcher crash → device shows FallbackHome / stuck Home → keep install reversible (NFR-007); test recovery.
- Performance on weak 32-bit hardware → enforce NFR-001/002 with Baseline Profiles + Macrobenchmark on-device.
- Compose-TV focus edge-cases (restoration, dialog trapping) → build an explicit, tested focus framework early.

## 13. MVP

**MVP = FR-001..FR-009** (maps 1:1 to `initial.md` §13) under NFR-001..NFR-007. Keep it small; everything
in FR-010+ is evaluated and scheduled separately. MVP delivery aligns with milestones **M2–M5**.

## 14. Future roadmap

Post-MVP capabilities (FR-010+) are sequenced in [MILESTONES.md](../../project/MILESTONES.md): polished TV
UX (M5), configuration/customization (M6), media/NAS integrations (M7), advanced integrations — Home
Assistant, weather, notifications, screensaver (M8).

## Follow-ups

- Document concrete, reproducible stock-launcher pain points from real use (turns §1 `ASSUMPTION` → `FACT`).
- Run the ADR-0002 validation spike to set real NFR-001/002 baselines, then confirm/adjust the budgets here.
- Verify ROLE_HOME / reboot-as-Home / crash-recovery behavior on the TCL (device-safety task).
