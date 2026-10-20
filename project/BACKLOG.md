# NexaTV — Backlog

> Hierarchy: **EPIC → Feature → Task.** Keep it lean — no artificial tasks (`initial.md` §23).
> Priority: P0 (now) · P1 (next) · P2 (later). Status: ☐ Todo · ◐ Doing · ☑ Done · ⏸ Blocked.

---

## EPIC: Phase 0 — Discovery  _(M0)_

### Feature: ADB connectivity & environment
- ☑ **P0** Verify/install ADB on the Windows dev host → standalone Google platform-tools (v37.0.1) at `C:\Users\ViacheslavM\platform-tools` (not on PATH; invoked by full path, no system change). 2026-10-20.
- ☑ **P0** Wireless ADB connection to the TCL established — TV shows as `device` in `adb devices -l` (model `Smart_TV_Pro`, codename `G08`). 2026-10-20.
  - *Note:* pairing code/IP/port are runtime-only — never committed.
- ☑ **P0** Android build toolchain obtained **without full Android Studio** — corporate policy blocks only the Studio installer CDN (`edgedl.me.gvt1.com`, 403), while the SDK repo (`dl.google.com`) is reachable. Installed via command-line `sdkmanager` into `%LOCALAPPDATA%\Android\Sdk`: cmdline-tools 22.0, platform-tools 37.0.1, `platforms;android-34`, `build-tools;34.0.0`. JDK 17.0.19 already on PATH (satisfies ADR-0003). 2026-10-20.

### Feature: Safe device audit (read-only)
- ◐ **P0** Run read-only audit; record in `docs/technical/ANDROID-TV.md`. Identity, ABI, **SoC (MT9689)/GPU (Mali-G52)**, display/refresh (60 Hz fixed), HOME environment and TV inputs (7) captured 2026-10-20. *Remaining:* full `pm list packages`; §3 launcher-model behaviors (reboot/crash-recovery) need active verification.

### Feature: Open-source launcher research
- ☐ **P1** Research FLauncher, LTvLauncher(+Extended), Yukuza, Tarang, TV Launcher, LauncherCompose.
  - *Acceptance:* each per-project doc filled from the real repo; claims labelled FACT/INFERENCE/ASSUMPTION/UNKNOWN with source refs.
- ☐ **P1** Build the feature matrix (`docs/research/FEATURE_MATRIX.md`). *Depends on:* per-project research.
- ☐ **P1** License analysis (`docs/research/LICENSE_ANALYSIS.md`). *Depends on:* per-project research.
- ☐ **P1** Architecture comparison (`docs/research/ARCHITECTURE_COMPARISON.md`). *Depends on:* per-project research.

### Feature: Architectural decision
- ☑ **P1** Decide & finalize ADR-0001 (Build vs. Fork) → **greenfield, permissive license, clean-room references** (2026-10-20).
- ☑ **P0** Draft & decide ADR-0002: Flutter vs. Kotlin/Compose-for-TV → **Accepted: Kotlin + Jetpack Compose-for-TV** (direct Android APIs, no Flutter), 2026-10-20.
- ☑ **P0** Validation spike (gate) — **PASS** 2026-10-20 (`docs/technical/SPIKE-ADR-0002.md`). Compose-for-TV builds (AGP 8.7.3 / Gradle 8.9 / Kotlin 1.9.24 / tv-material 1.0.0), APK has no native (`primaryCpuAbi=null`) so installs/runs on `armeabi-v7a` TCL, D-pad focus owner-confirmed on screen, cold start ~833 ms (`am start -W`, ceiling). Spike code throwaway/git-ignored.
  - ☑ **P1** Precise baselines via **Macrobenchmark** — 2026-10-20. NFR-001 cold-start (`COLD`, `CompilationMode.None`, 10 iters): **min 937.9 / median 984.4 / max 1346.8 ms** (budget ≤2.0 s p50 → **PASS**). NFR-002 idle PSS (`dumpsys meminfo`): **≈48.6 MB**. Device: TCL G08, 4×1.39 GHz, 2.5 GB. See `docs/technical/SPIKE-ADR-0002.md`.
    - ☐ **P2** NFR-017 glass fps/PSS gate — still open; needs the real glass UI (M1+), not the spike.
- ☑ **P1** Pick exact permissive license (MIT vs Apache-2.0) and add `LICENSE` file → **Apache-2.0** chosen; `LICENSE` + `NOTICE` added (2026-10-20).

### Feature: Android launcher-model understanding
- ☐ **P2** Document HOME intent / multiple-HOME / reboot / crash-recovery behavior, verified on the TCL.

---

## EPIC: Phase 1 — Foundation  _(M1 — complete 2026-10-20)_
- ☑ **P1** Create the code scaffold for the chosen stack (2026-10-20). Root Gradle + version catalog (`gradle/libs.versions.toml`) + wrapper 8.9; `:app` module `com.nexatv.launcher` (SDK 34, `versionCode` 100 / `versionName` 0.1.0, NFR-011). Manifest: HOME + LEANBACK_LAUNCHER (FR-001), no INTERNET + `allowBackup=false` + data-extraction rules (NFR-021). Compose-for-TV design tokens (color/type/spacing, body ≥18sp — NFR-016), en/uk strings (NFR-020), placeholder Home. **Builds** (`assembleDebug`, JDK 17 / SDK 34). `.gitattributes` enforces LF for CI (ADR-0003).
  - ☑ **P1** CI pipeline — `.github/workflows/ci.yml` wiring the ADR-0003 D1 gate (2026-10-20): `assembleDebug` + Android Lint + `testDebugUnitTest` + **detekt** (config `config/detekt/detekt.yml`) + **ABI assert** (armeabi-v7a only) + **APK-size budget** + **gitleaks** secret scan, on PR/push to `main`. All Gradle steps verified green locally.
    - ☐ **P1** `release.yml` (tag-driven signed release, ADR-0003 D2/D6) — **blocked** on the owner-generated **offline keystore** (D3/D4) + GH `release` Environment secrets + branch protection. Owner-dependent; not a step the agent can do alone.

---

## EPIC: Phase 1 — Foundation (cont.)
- ☑ **P0** Author PRD v1 — MVP locked to **FR-001..FR-009** under NFR-001..NFR-007 (`docs/product/PRD.md`, 2026-10-20, Draft v1 for review).
- ☑ **P0** PRD critics review + author **PRD v2** — folded in MUST+MVP-blocking and SHOULD(doc-now) deltas from the 6-lens expert panel: new sections §15 Delivery/CI-CD, §16 Design System, §17 Accessibility, §18 Permissions, §19 Testability/Observability/DoD; added **FR-011..FR-017** and **NFR-008..NFR-023** (2026-10-20, Draft v2 for review).
- ☑ **P1** Record the PRD-v2 follow-up ADRs (Hard Rule 6):
  - ☑ **ADR-0003** — CI/CD, signing, release & update/rollback strategy (GitHub Actions, tag-driven signed releases, single long-lived key + offline backup, manual sideload, reproducibility = non-goal) — covers NFR-008..015. **Accepted** 2026-10-20.
  - ☑ **ADR-0004** — App enumeration without `QUERY_ALL_PACKAGES` (`LauncherApps.getActivityList` via `ROLE_HOME`) — covers FR-015/016. **Accepted** 2026-10-20.
  - ☑ **ADR-0005** — "Glass"/translucency technique (fake-glass default; real-time blur gated on-device) — covers NFR-017. **Accepted** (measurement-gated) 2026-10-20.
- ☑ **P1** Create the FR×Test traceability matrix stub (`docs/testing/TRACEABILITY.md`) + refresh TEST-PLAN.md for the Kotlin stack. 2026-10-20.

## EPIC: Design System  _(foundational — precedes launcher screens)_
> Owner directive (2026-10-20): the Design System is its **own step**, done **before** building
> launcher screens so every screen consumes shared tokens/components. Implements PRD §16 / NFR-016.
- ☑ **P1** **Design System step** (2026-10-20). Promoted the M1 token seeds into a documented, reusable
  system: semantic `NexaColorScheme` roles behind `NexaTheme` (CompositionLocal), the type scale
  (body ≥18sp — NFR-016), spacing/radius grid, and a core-component catalog — `SectionHeader` + focusable
  `AppTile` (D-pad focus ring, no ripple) with `@Preview`s. Debug-only **Design System gallery**
  (`src/debug`, separate TV entry) renders every token + component. Documented in
  `docs/design/DESIGN-SYSTEM.md`. `assembleDebug` + detekt green locally.
  - *Theme-agnostic by construction:* all colors flow from the active `NexaColorScheme` (no literals in
    screens), so the later theme-switch step is a drop-in.

## EPIC: Theming — switchable themes, persisted  _(later step; depends on Design System + Persistence ADR)_
> Owner directive (2026-10-20): provide **easy theme switching** whose selection **survives restart**.
- ☐ **P2** **Theme catalog** — ≥2 built-in themes (e.g. the pastel dark default + at least one alternative),
  each a complete token set behind `NexaTheme`. *Depends on:* Design System step.
- ☐ **P2** **Theme persistence** — persist the selected theme and re-apply on cold start. *Depends on:* the
  **Persistence ADR** (DataStore vs. alternatives — currently an open/planned ADR) and the Configuration ADR.
- ☐ **P2** **Theme switcher UI** — a D-pad-friendly control (in Settings) to change theme live; selection
  takes effect immediately and is restored after reboot. *Depends on:* the two items above.

## EPIC: Phase 2 — Minimal Launcher  _(M2 — complete 2026-10-20)_
M2 goal (per MILESTONES) is deliberately narrow: **Launcher Activity, D-pad focus, app-list render +
launch**. Favorites/categories/settings/persistence are **M4/M6**, HOME registration is **M3** — kept out
of M2 for phase discipline. Covers **FR-002, FR-003, FR-006, FR-011, FR-012, FR-014** (partial); full
MVP (FR-001..009) completes across M3/M4.
- ☑ **P0** App discovery via `LauncherApps.getActivityList(null, user)` across profiles (ADR-0004) — no
  `QUERY_ALL_PACKAGES`; dedup + sort; icons rasterized off the UI thread (NFR-003). `data/AppEntry` +
  `data/InstalledAppsRepository`. 2026-10-20.
- ☑ **P0** `HomeViewModel` + `HomeUiState` (Loading / Empty / Apps); first frame before discovery finishes
  (FR-009/FR-014). 2026-10-20.
- ☑ **P0** D-pad-navigable app grid (`ui/home/HomeScreen`) on the canonical `AppTile` (icon + monogram
  fallback, FR-013), initial focus on first tile (FR-006/FR-011), safe-area insets (FR-012), launch on OK
  with an in-UI, focus-preserving, auto-dismiss error (FR-003/FR-014). `MainActivity` renders it. 2026-10-20.
- ☑ **P0** `assembleDebug` + `lintDebug` + `detekt` green locally. 2026-10-20.
- ☑ **P0** **On-device verification** on the real TCL (2026-10-20): first reversible install
  (`com.nexatv.launcher.debug`, HOME untouched) — no crash; grid renders real icons; D-pad moves one cell
  per press (FR-006); OK launches the *focused* app (FR-003); focus ring visible (FR-011); `HOME` returns
  to the stock launcher (reversible). Added `Modifier.focusRestorer` so focus returns to the first tile
  after coming back from a launched app. Details in `docs/technical/ANDROID-TV.md` §3a.
  *Note:* pre-`ROLE_HOME`, the grid may be partial (ADR-0004 / FR-015) — expected until M3.
- ☐ **P1** Live app add/remove via `LauncherApps.Callback` (FR-002 ≤2 s) — **deferred to M4** (discovery
  robustness); M2 loads once on start.
