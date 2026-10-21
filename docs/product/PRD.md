# NexaTV — Product Requirements Document (PRD)

> **Status:** Draft v2 for review — 2026-10-20. v2 folds in the critics-panel gap review (6 expert lenses):
> added a Delivery/CI-CD, a Design-System, an Accessibility, a Permissions, and a
> Testability/Observability/DoD section, plus FR-011..FR-017 and NFR-008..NFR-023.
> Authored after Phase 0 research, ADR-0001 (greenfield, permissive), ADR-0002 (Kotlin + Jetpack
> Compose-for-TV), and the Apache-2.0 license decision.
> Requirements use **stable IDs** (`FR-00x`, `NFR-00x`) that must **not** be renumbered once assigned.
> Evidence labels: `FACT` (verified), `ASSUMPTION` (plausible, unverified), `UNKNOWN` (open).
> Each row is tagged **MVP-blocking** / **doc-now** (decide now, build later) / **build-later**.

## 1. Problem statement

The TCL unit ships with the stock Google TV / TCL launcher as its Home experience. The project owner's
motivation is that it does **not** provide the Home experience they want: it is
content/recommendation-and-ad-oriented rather than an app-first, fast, fully owner-controlled surface.

- `ASSUMPTION` (owner-stated, not yet itemised against this firmware): the stock launcher emphasises
  promoted content/ads and cloud recommendations over a clean, predictable app grid, and offers limited
  layout/favorites control. Concrete, reproducible pain points will be recorded during the device-safety
  work (see Follow-ups) so this section becomes `FACT`.

NexaTV replaces the Home experience with a **polished, fast, D-pad-first, offline-first, ad-free launcher**
the owner fully controls — standing alongside the stock launcher (installed reversibly, never disabling or
removing system apps).

## 2. Vision

A polished, fast, customizable, remote-first Android TV Home experience — offline-first and
privacy-friendly — that is a better daily Home than the stock launcher on the owner's own hardware. Its
look is **minimalist and elegant**: a restrained pastel palette with tasteful translucent ("glass")
surfaces, never at the cost of 10-foot legibility or performance.

## 3. Goals

- G1 — Be a **reliable, always-available Home**: register as a HOME app and start cleanly, including after
  reboot, without destabilising the device.
- G2 — Make launching installed apps **faster and more predictable** than the stock launcher, with
  always-obvious D-pad focus.
- G3 — Give the owner **control**: favorites, basic categories, simple settings, persisted across reboots.
- G4 — Stay **lean** on the audited 32-bit / ~2.34 GiB device (low memory, fast startup).
- G5 — Be **offline-first and ad-free / privacy-friendly** — no mandatory accounts, no default telemetry.
- G6 — Be **deliverable and recoverable**: built, tested, versioned and released through a reproducible
  GitHub pipeline, with signed artifacts and a safe update/rollback story.
- G7 — Have a **coherent, elegant visual language**: a tokenised pastel/translucent design system that stays
  legible and fast on the real hardware.

## 4. Non-goals (MVP)

- Phone/tablet/touch/mouse support — **TV + D-pad only**.
- Mandatory cloud accounts, advertising, or default analytics/telemetry.
- Replacing or disabling the stock launcher, uninstalling system apps, or modifying system partitions.
- Media/NAS/Jellyfin/Plex/Kodi/Home Assistant integrations, Continue Watching, weather, screensaver,
  notifications — all **post-MVP** (§14), evaluated separately.
- `TvContract` / preview channels, "Watch Next", recommendation / sponsored rows.
- A **custom screensaver** — the system DREAM stays active and untouched for MVP.
- Multi-device / non-TCL hardware support (nice-to-have later; not an MVP constraint).
- **Full bit-for-bit reproducible builds** (see NFR-012 — explicitly a non-goal for MVP).
- **In-app / over-the-air self-update** (MVP ships via manual `adb` sideload — see NFR-015).
- `ASSUMPTION` — a **single primary user profile**; secondary/restricted profiles
  (`LauncherApps` user profiles) are out of MVP scope. Channels/profiles revisited post-MVP.

## 5. Target environment

Primary device — TCL Smart TV, Android TV OS 14 (audited specs in
[ANDROID-TV.md](../technical/ANDROID-TV.md)). The requirements below are weighed against these `FACT`s:

| Property | Value | Relevance |
|---|---|---|
| OS / SDK | Android TV OS 14 / SDK 34 | `RoleManager ROLE_HOME`, `LauncherApps`, `tv-material` all available |
| CPU ABI | **armeabi-v7a only** (no arm64) | Pure-Kotlin/Compose app ships no `.so` → ABI-agnostic (ADR-0002) |
| Cores / RAM | 4 cores / ~2.34 GiB | Launcher is always-resident → memory budget matters (NFR-002) |
| Display | 4K panel, **UI at 1080p**, density 320 | Design target is 1080p 10-foot UI |
| SoC / GPU | MediaTek MT9689 (Cortex-A73); **ARM Mali-G52**, GL ES 3.2 | Low-mid GPU at 60 Hz/16.67 ms → real-time blur is a measured risk (NFR-017, ADR-0005) |
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

> FR-001..FR-009 are the original **MVP**. FR-011..FR-016 were added in v2 (design & platform correctness);
> FR-017 is build-later. FR-010 remains the post-MVP catch-all bucket.

| ID | Requirement | Priority | MVP? | Acceptance criteria | Status |
|----|-------------|----------|------|---------------------|--------|
| FR-001 | Register as an Android HOME app | P0 | Yes | App declares a HOME launcher Activity (MAIN + HOME + DEFAULT + LEANBACK_LAUNCHER). User can set NexaTV as default Home via `RoleManager ROLE_HOME` / system picker. Pressing HOME opens NexaTV. **Stock launcher is never disabled; user can switch back at any time.** | ☐ |
| FR-002 | Discover installed applications | P0 | Yes | On launch, NexaTV lists launchable apps via `LauncherApps`/`PackageManager` (LEANBACK_LAUNCHER ∪ LAUNCHER, de-duplicated), with label + icon. A newly installed/removed app **appears/disappears within ≤2 s without a manual refresh** (`ACTION_PACKAGE_*`). Discovery runs off the UI thread. | ☐ |
| FR-003 | Launch an application | P0 | Yes | Selecting an app (OK) starts its leanback/main launch intent; a failed launch shows a non-crashing, focus-preserving in-UI message (FR-014) and keeps focus. | ☐ |
| FR-004 | Favorites | P0 | Yes | User can mark/unmark any app as favorite (e.g. long-press → menu). Favorites appear in a dedicated, first-focused row/section and persist across restart/reboot. | ☐ |
| FR-005 | Basic categories | P0 | Yes | Apps are grouped into at least Favorites + All apps (plus auto-grouping where the system category is available). **Grouping is identical across two cold starts and after one reinstall.** | ☐ |
| FR-006 | D-pad navigation & focus handling | P0 | Yes | Entire UI is operable with Up/Down/Left/Right/OK/Back/Home only. Focus is **always visible** (FR-011), movement is predictable, and the last focused item is restored when returning to a screen. No focus traps/dead-ends. (Validated on the physical remote — see TEST-PLAN & §19.) | ☐ |
| FR-007 | Simple settings | P0 | Yes | A settings screen reachable by remote lets the user change at least one real preference (e.g. layout/sort or favorites management) and open the system "set default Home" flow. | ☐ |
| FR-008 | Persistence of user configuration | P0 | Yes | Favorites, ordering, and settings survive app restart **and** TV reboot (local storage; no cloud/account). | ☐ |
| FR-009 | Reliable startup (incl. after reboot) | P0 | Yes | NexaTV cold-starts to an interactive, focused Home within the NFR-001 budget; after a TV reboot it comes up as Home (when set as default) without crash/ANR; if app discovery is slow, UI appears first and populates progressively. Relies on the system auto-starting Home after boot; **does not declare `RECEIVE_BOOT_COMPLETED`** (avoids low-RAM overhead). | ☐ |
| FR-010+ | Post-MVP catch-all: app reordering, recents, Continue Watching, clock, weather, backgrounds, OLED screensaver, media/NAS/Jellyfin/Plex/Kodi, Home Assistant, notifications, input shortcuts, decorative animation | P1–P2 | No | Specified separately when scheduled (§14 / MILESTONES M5+). | ☐ |
| FR-011 | Measurable, always-obvious focus | P0 | Yes (MVP-blocking) | The focused tile shows **≥2 simultaneous signals** from {scale ≥1.08×; focus ring ≥4 dp at ≥3:1 contrast; elevation/brightness lift}. The indicator is ≥3:1 against **both** the element and the page background. Focus settles ≤150 ms after a D-pad press. Verified on-device from ~3 m on a 55″ panel. (Makes G2/FR-006 measurable.) | ☐ |
| FR-012 | Safe area / overscan | P0 | Yes (MVP-blocking) | All interactive content and focus rings stay within a ≥5% safe area (~48 dp top/bottom, 64 dp left/right @1080p); row content never bleeds into the physical edge. Acceptance: on the real TCL the first and last item of every row and their focus ring are fully visible. | ☐ |
| FR-013 | Canonical app tile & art selection | P1 | Yes (MVP look) | One canonical tile (fixed aspect + shape + inner padding + background token). Deterministic art order: banner → adaptive icon → icon → generated monogram placeholder (label + pastel accent). Labels 1 line with ellipsis. Images downsampled to tile size; memory-bounded cache (supports NFR-002). Acceptance: a mix of ≥10 real apps (banners, icon-only, one art-less) renders uniformly with no crop/blur/empty tiles. | ☐ |
| FR-014 | Empty / loading / error states | P1 | Yes | During progressive population show **token skeleton placeholders** (never a blank/white frame); empty-Favorites state with a hint to Settings; "no apps" state; launch errors shown **in-UI, focus-preserving, auto-dismissing** (not a system toast). Makes FR-003/FR-009 verifiable. | ☐ |
| FR-015 | Enumerate apps **without** `QUERY_ALL_PACKAGES` | P0 | Yes (MVP-blocking) | Enumerate via `LauncherApps.getActivityList` (visibility granted to the `ROLE_HOME` holder); **do not** declare the sensitive/policy-gated `QUERY_ALL_PACKAGES`. Until NexaTV is Home, show an explicit first-run state ("Set NexaTV as default Home to see all apps"), not a partial/empty list. Measurable: with `ROLE_HOME` granted, launchable count = the stock launcher's (delta 0). Decision recorded in an ADR. | ☐ |
| FR-016 | Discovery exclusions | P1 | Yes | Exclude: packages with no launch activity, disabled, suspended, NexaTV itself, and (configurable) the stock launcher; subscribe to `PACKAGE_CHANGED`. Measurable: no visible tile leads into the FR-003 failure path. | ☐ |
| FR-017 | App actions (App info / Uninstall) | P2 | No (build-later) | Long-press menu: "App info" (`ACTION_APPLICATION_DETAILS_SETTINGS`) for all; "Uninstall" (system confirmation) **only** for user-installed non-system apps. Never a silent uninstall; never uninstall/disable system apps — encodes the device-safety rule. | ☐ |

## 8. Non-functional requirements (NFR)

> NFR-001..007 are the original core budgets. **NFR-008..NFR-023 are defined in the themed sections
> §15–§19** (Delivery/CI-CD, Design System, Accessibility, Permissions, Testability) and are the
> authoritative text for those IDs. Numeric targets are **initial budgets** confirmed on the real TCL
> (measure on-device, never an emulator).

| ID | Requirement | Target / measure | Status |
|----|-------------|------------------|--------|
| NFR-001 | Fast startup | Cold start to interactive, focused Home **≤ 2.0 s** on the TCL (p50) / ≤ 3.0 s (p95); measured via Macrobenchmark + Baseline Profiles. Budget confirmed after baseline. | ☐ |
| NFR-002 | Low memory footprint | Idle resident set small enough to avoid being killed under pressure; **target ≤ ~150 MB** idle PSS on this ~2.34 GiB device (confirm vs. baseline). No leaks across navigation. Leak method (build-later): debug `LeakCanary` + `StrictMode`; zero retained leaks after ≥50 package add/remove and ≥100 navigation cycles; idle PSS returns within +10% of baseline (no monotonic growth). | ☐ |
| NFR-003 | Responsive during discovery | No UI-thread blocking: discovery/icon work off-main; interaction stays responsive (no ANR; dropped-frame budget met) while the app list loads. | ☐ |
| NFR-004 | Offline-first core | FR-001..FR-009 work fully with **no network**; no feature on the MVP path requires internet or an account. | ☐ |
| NFR-005 | Reliable after reboot | After reboot (as default Home), NexaTV reaches interactive Home with no crash/ANR across repeated reboots; persisted state intact. | ☐ |
| NFR-006 | Privacy-friendly / ad-free | **Zero** advertising; **no** analytics/telemetry by default; no account required; no personal data leaves the device. Made structurally verifiable by NFR-021 (no `INTERNET` permission). | ☐ |
| NFR-007 | Reversible install / safe fallback | Installed as a normal user app; setting/unsetting as Home is user-reversible; stock launcher remains installed and selectable; uninstalling NexaTV restores the prior Home. Verified before any Home-registration test. | ☐ |

## 9. UX principles

Remote-first; **focus always obvious and movement predictable** (FR-011); no mobile-first patterns. All
major functionality usable from the remote (Up/Down/Left/Right/OK/Back/Home, long-press where useful).
10-foot readability at 1080p. Built on first-party TV focus primitives (`focusable`, `FocusRequester`,
`FocusGroup`, `onPreviewKeyEvent`) per ADR-0002. Visual language is defined by the design system (§16).

## 10. Constraints

- `FACT` — 32-bit `armeabi-v7a` only; ~2.34 GiB RAM; 1080p UI; SDK 34 (§5). Favor lean implementation.
- `FACT` — License: Apache-2.0; greenfield; GPL launchers are clean-room references only (no code reuse).
- `FACT` — Device safety: read-only first; never disable the stock launcher, uninstall
  system apps, or modify system partitions; first install must be reversible.
- `FACT` — Hosted GitHub CI runners have **no** 32-bit `armeabi-v7a` device, and device-safety keeps the
  real TCL out of CI → performance NFRs can only be gated by a manual on-device pre-release step (NFR-013).
- `UNKNOWN` — Exact firmware behavior for default-Home selection, reboot-as-Home, and crash recovery on this
  TCL build; and the exact GPU's tolerance for real-time translucency. Both verified on-device.

## 11. Assumptions

- `FACT` — `RoleManager ROLE_HOME` is present and functional on this TCL build (current holder
  `com.google.android.apps.tv.launcherx`, confirmed via `cmd role` 2026-10-20). `ASSUMPTION` (still open) —
  the user-facing default-Home picker lets us actually *select* NexaTV; verify once an installable APK exists.
- `ASSUMPTION` — `tv-material` focus behaves correctly on this SDK-34 unit (to confirm in the ADR-0002 spike).
- `ASSUMPTION` — The owner's C#→Kotlin transfer keeps the single-stack learning curve a schedule risk, not a
  blocker.

## 12. Risks

- HOME-app behavior differs across firmware (default selection, reboot, crash-recovery) → mitigate with
  on-device verification and never disabling the stock launcher.
- Launcher crash → device shows FallbackHome / stuck Home → keep install reversible (NFR-007); test recovery.
- **Lost signing key** → can never update in place (no Play App Signing safety net) → offline keystore
  backup is mandatory (NFR-010).
- **Bad release bricks the Home experience** → one-step rollback APK kept per release + manual smoke test
  before promotion (NFR-015).
- Performance on weak 32-bit hardware, esp. **real-time translucency/blur** → enforce NFR-001/002/017 with
  Baseline Profiles + Macrobenchmark on-device; prefer "fake glass".
- **Pastel-on-glass illegibility** (low contrast over dynamic backdrops) → NFR-018 contrast floor + scrim.
- Compose-TV focus edge-cases (restoration, dialog trapping) → build an explicit, tested focus framework early.

## 13. MVP

**MVP = FR-001..FR-009** plus the MVP-blocking additions from v2:
FR-011, FR-012, FR-015, FR-016 and NFR-008, NFR-009, NFR-010, NFR-011, NFR-015 (delivery),
NFR-016, NFR-017, NFR-018 (design), NFR-021 (permissions), NFR-023 (observability).
FR-013/FR-014 and NFR-019/020/022 are part of the intended MVP look/quality where cheap. The MVP stays
small in *new launcher code* — most additions are **cheap-now, costly-to-retrofit** decisions (design
tokens, stable signing key, monotonic `versionCode`, `allowBackup=false`, no `INTERNET`, avoiding
`QUERY_ALL_PACKAGES`, a basic CI gate). MVP delivery aligns with milestones **M1–M5**.

## 14. Future roadmap

Post-MVP capabilities (FR-010+, FR-017) are sequenced in [MILESTONES.md](../../project/MILESTONES.md):
polished TV UX (M5), configuration/customization (M6), media/NAS integrations (M7), advanced integrations —
Home Assistant, weather, notifications, screensaver (M8). Costly-but-deferred items (self-hosted perf
runner, SBOM/vuln scan, in-app updater, second test rig) are documented now and built later.

## 15. Delivery, CI/CD & releases

> ⭐ Owner emphasis #1. Honest trade-offs: hosted runners can't gate performance on 32-bit hardware
> (→ NFR-013 manual gate); full reproducibility is impractical on Android (→ NFR-012 non-goal); the signing
> key is the sole root of trust with no Play App Signing fallback (→ NFR-010 offline backup).

| ID | Priority | MVP? | Requirement |
|----|----------|------|-------------|
| NFR-008 | MUST | MVP-blocking (light) | Every PR and push to `main` triggers GitHub Actions: `assembleDebug` + Android Lint + `ktlint`/`detekt` + JVM unit tests on `ubuntu-latest` (JDK 17, SDK 34). Green build is a required status check before merge. Target ≤10 min with Gradle cache. |
| NFR-009 | MUST | MVP-blocking | `main` is protected: no direct push, ≥1 required check, linear history. Pushing tag `vX.Y.Z` triggers the release workflow. The release/publish job **never runs on fork PRs**. |
| NFR-010 | MUST | MVP-blocking | All release APKs signed with a **single long-lived keystore** (valid ≥25–30 y) using APK Signature Scheme v2+v3 (v1 disabled at minSdk 34); one certificate for all releases → in-place updates without uninstall. Keystore (base64) + passwords live **only** in GitHub encrypted secrets + an offline owner backup; never in Git/logs/Gradle files. CI fails if a release build starts without the secrets. Key backup/rotation procedure recorded in an ADR. |
| NFR-011 | MUST | MVP-blocking | `versionName` = SemVer `MAJOR.MINOR.PATCH`; `versionCode` = strictly increasing integer from a deterministic formula (e.g. `MAJOR*10000+MINOR*100+PATCH`); a release with `versionCode` ≤ previous is rejected. Each GitHub Release is created from an annotated tag and contains: the signed APK, notes from `CHANGELOG.md` ("Keep a Changelog"), the APK **SHA-256**, the signing-cert **fingerprint**, and the exact git SHA. The release APK is the byte-for-byte CI artifact. |
| NFR-012 | SHOULD | doc-now | Toolchain pinned (Gradle wrapper, version catalog, AGP/compileSdk 34/JDK 17). A release is the CI artifact from a tagged commit (not a local rebuild). **Full bit-for-bit reproducibility is a NON-GOAL for MVP** (documented trade-off). |
| NFR-013 | SHOULD | doc-now | Because hosted CI has no 32-bit arm device, NFR-001/002/003 are verified by a **manual pre-release Macrobenchmark run on the real TCL** (≥10 cold iterations, Baseline Profiles); p50/p95 start and idle PSS recorded in the release notes. A release is blocked if cold-start p95 > 3.0 s or idle PSS > ~150 MB. CI runs only cheap proxies (APK-size budget, Baseline-Profile freshness check). |
| NFR-014 | SHOULD→COULD | doc-now | PR-CI additionally: secret-scan ("gitleaks") — fail on any committed secret (enforces Hard Rule 4); **assert ABI** (APK contains `lib/armeabi-v7a`, **no** 64-bit `.so`) + APK-size budget; (COULD) SBOM + license-compat scan (Apache-2.0 incompatibility / copyleft) + vuln-scan ("OSV"), dependency version pinning, Dependabot, denylist for analytics/crash/ads/network SDKs (reinforces NFR-006). |
| NFR-015 | MUST | MVP-blocking | Update = in-place `adb install -r` of a signed APK with a higher `versionCode`, data preserved. Documented recovery if an update crash-loops: (a) fall back to FallbackHome/stock launcher as Home, (b) `adb install` the previous stable APK. Each GitHub Release keeps the previous stable APK downloadable (one-step rollback). Before promoting a release — a **manual on-device cold-start smoke test on the real TCL** (launch, reboot, set-as-Home) as a checklist item. ADR: MVP uses manual sideload only (no `REQUEST_INSTALL_PACKAGES`). |

## 16. Design system & visual language

> ⭐ Owner emphasis #2: minimalist, elegant, **pastel + translucent "glass"**. Honest trade-offs:
> real-time blur is the costliest effect on a weak 32-bit GPU (→ NFR-017 prefer "fake glass"); pastel is
> inherently low-contrast and can make focus/text invisible from 3 m (→ NFR-018 contrast floor + scrim).

| ID | Priority | MVP? | Requirement |
|----|----------|------|-------------|
| NFR-016 | MUST | MVP-blocking (cheap now, costly to retrofit) | A single Compose `MaterialTheme` with: (a) a pastel palette of ≥6 named roles (surface / on-surface / accent / focus / success / error); (b) a type scale of 4–5 steps (Display/Title/Body/Label), **body ≥18 sp** @ density 320 for 10-foot; (c) an 8 dp spacing grid and 2–3 radius tokens. Acceptance: every MVP screen consumes **only** tokens (zero hardcoded hex/sp — enforced by a `detekt`/lint rule). |
| NFR-017 | MUST | MVP-blocking (+ADR) | "Glass" is achieved mostly by pre-baked/alpha technique (not per-frame blur). Real-time backdrop blur is allowed on ≤1 small surface and only if scroll/focus holds ≥58 fps (frame ≤16.6 ms, Macrobenchmark on the real TCL) with no NFR-001/002 regression; otherwise a flat fallback. Acceptance: a documented on-device fps+PSS comparison of the chosen technique vs an opaque baseline. |
| NFR-018 | MUST | MVP-blocking | Text and essential icons: **≥4.5:1** (body) / **≥3:1** (large text & focus ring) contrast against the **worst actually-rendered translucent backdrop** (a captured TV frame, not a flat mock). An **opaque scrim** sits under text on translucent panels. Focus is conveyed by more than hue (ring + scale + elevation). Contrast check is part of Definition of Done. |

## 17. Accessibility

| ID | Priority | MVP? | Requirement |
|----|----------|------|-------------|
| NFR-019 | SHOULD | doc-now, partly MVP | The UI stays usable and un-clipped at system font-scale **up to ≥1.3×** and the largest TCL display size; **sp only**, no hardcoded px. Every focusable element has a meaningful `contentDescription`/semantics so Home is navigable with "TalkBack". Verified via accessibility scanner / a manual TalkBack pass in DoD. |
| NFR-020 | SHOULD | doc-now (cheap now) | All launcher-authored strings live in resources (zero UI literals); MVP locales **en + uk**; layout tolerates +30% string length; locale follows the system; avoid absolute left/right (RTL is post-MVP). |

## 18. Permissions & privacy posture

> `FR-002` implicitly tempts the sensitive `QUERY_ALL_PACKAGES`, and default `allowBackup` would leak
> favorites/settings to Google Drive (contradicting NFR-006). These deltas make NFR-006 **structurally
> verifiable** rather than merely declarative.

| ID | Priority | MVP? | Requirement |
|----|----------|------|-------------|
| NFR-021 | MUST | MVP-blocking (cheap) | Minimal permissions, each listed/justified in docs. `android:allowBackup="false"` + `dataExtractionRules` excluding cloud-backup and device-transfer. The merged release manifest has **no** `android.permission.INTERNET` / `ACCESS_NETWORK_STATE`. CI fails on any of these violations (defense-in-depth for NFR-006). |
| NFR-022 | SHOULD | doc-now | Persistent config is documented as owner-local and non-sensitive (no accounts/tokens/PII) → plaintext DataStore/SharedPreferences is acceptable; at-rest encryption is out of scope for MVP (rationale: startup/RAM budget). A future sensitive field → `EncryptedSharedPreferences`. |

(See also FR-015/FR-016/FR-017 for the discovery-visibility and app-action permission behavior.)

## 19. Testability, observability & Definition of Done

> The strongest promises (G1 "always-available Home", "no crash/ANR") are currently unverifiable because
> telemetry is banned with no sanctioned alternative. These deltas make them measurable.

| ID | Priority | MVP? | Requirement |
|----|----------|------|-------------|
| NFR-023 | MUST | MVP-blocking (minimal) | A `qaDebug` build variant writes crash+ANR to a local app-private file, retrievable via `adb pull` / a Settings "export diagnostics" action; release builds have zero network egress (no-`INTERNET` CI check). A global crash-handler logs and recovers instead of silently dying. Reliability acceptance = a scripted **50× reboot soak** on the real TCL with 0 crash / 0 ANR and `dumpsys activity` showing Home bound after each boot. |

**Definition of Done & traceability (process, MVP).** An FR is Done only when it has: (1) an objective
acceptance criterion, (2) ≥1 automated test or a documented manual test, (3) a recorded passing run. Soft
criteria are rewritten measurably (done in §7 for FR-002/FR-005). An FR×Test matrix lives in
`docs/testing/TRACEABILITY.md`.

**D-pad test method (doc-now).** An instrumented test injects `DPAD_*`/`BACK`/`CENTER` across each screen
and asserts a focused node after every event; a scripted path visits every top-level row and returns (no
focus trap). Plus a per-screen manual checklist; Back-from-Home-root and the Home key are separate cases.

**Test matrix & environment (doc-now).** Pin the TCL model + firmware build id; run instrumented tests on
(a) the physical TCL (authoritative) and (b) an `armeabi-v7a`/API-30 emulator if an image exists, else
document it as not-covered with ABI risk. Re-run the reboot soak after every OTA.

## Follow-ups

- Document concrete, reproducible stock-launcher pain points from real use (turns §1 `ASSUMPTION` → `FACT`).
- Run the ADR-0002 validation spike to set real NFR-001/002 baselines, then confirm/adjust the budgets here.
- Verify ROLE_HOME / reboot-as-Home / crash-recovery behavior on the TCL (device-safety task).
- **Record the new ADRs** these deltas imply (Hard Rule 6):
  - CI/CD, signing, release & update/rollback strategy (manual sideload, offline key backup, reproducibility
    non-goal).
  - App enumeration without `QUERY_ALL_PACKAGES` (visibility via `ROLE_HOME`).
  - "Glass"/translucency technique (fake-glass vs measured real-time blur budget).
