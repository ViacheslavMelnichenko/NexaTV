# NexaTV — Backlog

> Hierarchy: **EPIC → Feature → Task.** Keep it lean — no artificial tasks.
> Status: ☑ Done · ⏸ Deferred (owner paused — not on the current agenda).
> Every item is one of the two; there are no not-started or in-progress markers.

---

## EPIC: Phase 0 — Discovery  _(M0 — Done 2026-10-20)_

### Feature: ADB connectivity & environment
- ☑ Verify/install ADB on the Windows dev host → standalone Google platform-tools (v37.0.1), invoked by full path (not on PATH, no system change). 2026-10-20.
- ☑ Wireless ADB connection to the TCL established — TV shows as `device` in `adb devices -l` (model `Smart_TV_Pro`, codename `G08`). 2026-10-20. *(Pairing code/IP/port are runtime-only — never committed.)*
- ☑ Android build toolchain obtained **without full Android Studio** — corporate policy blocks only the Studio installer CDN (`edgedl.me.gvt1.com`, 403); the SDK repo (`dl.google.com`) is reachable. Installed via command-line `sdkmanager` into `%LOCALAPPDATA%\Android\Sdk`: cmdline-tools 22.0, platform-tools 37.0.1, `platforms;android-34`, `build-tools;34.0.0`. JDK 17 already on PATH. 2026-10-20.

### Feature: Safe device audit (read-only)
- ☑ Read-only audit recorded in `docs/technical/ANDROID-TV.md`: identity, ABI, SoC (MT9689)/GPU (Mali-G52), display/refresh (60 Hz fixed), HOME environment and TV inputs. 2026-10-20.

### Feature: Open-source launcher research
- ☑ Researched FLauncher, LTvLauncher(+Extended), Yukuza, Tarang, TV Launcher, LauncherCompose from their real repos; built the feature matrix, license analysis, and architecture comparison; claims labelled FACT/INFERENCE/ASSUMPTION/UNKNOWN with source refs. 2026-10-20. *(Research docs have since been removed from the tree; conclusions are folded into ADR-0001.)*

### Feature: Architectural decision
- ☑ ADR-0001 (Build vs. Fork) → **greenfield, permissive license, clean-room references** (2026-10-20).
- ☑ ADR-0002: Flutter vs. Kotlin/Compose-for-TV → **Accepted: Kotlin + Jetpack Compose-for-TV** (direct Android APIs, no Flutter), 2026-10-20.
- ☑ Validation spike (gate) — **PASS** 2026-10-20. Compose-for-TV builds (AGP 8.7.3 / Gradle 8.9 / Kotlin 1.9.24 / tv-material 1.0.0); APK has no native code (`primaryCpuAbi=null`) so it installs/runs on the `armeabi-v7a` TCL; D-pad focus owner-confirmed on screen; spike code was throwaway/git-ignored.
- ☑ Precise baselines via **Macrobenchmark** — 2026-10-20. NFR-001 cold-start (`COLD`, `CompilationMode.None`, 10 iters): min 937.9 / median 984.4 / max 1346.8 ms (budget ≤2.0 s p50 → PASS). NFR-002 idle PSS (`dumpsys meminfo`): ≈48.6 MB. Device: TCL G08, 4×1.39 GHz, 2.5 GB. See `benchmark/README.md`.
- ☑ Pick exact permissive license (MIT vs Apache-2.0) → **Apache-2.0**; `LICENSE` + `NOTICE` added (2026-10-20).

---

## EPIC: Phase 1 — Foundation  _(M1 — Done 2026-10-20)_
- ☑ Code scaffold for the chosen stack. Root Gradle + version catalog (`gradle/libs.versions.toml`) + wrapper 8.9; `:app` module `com.nexatv.launcher` (SDK 34, `versionCode` 100 / `versionName` 0.1.0, NFR-011). Manifest: HOME + LEANBACK_LAUNCHER (FR-001), no INTERNET + `allowBackup=false` + data-extraction rules (NFR-021). Compose-for-TV design tokens (color/type/spacing, body ≥18sp — NFR-016), en/uk strings (NFR-020), placeholder Home. Builds (`assembleDebug`, JDK 17 / SDK 34). `.gitattributes` enforces LF for CI (ADR-0003). 2026-10-20.
- ☑ **CI pipeline** — `.github/workflows/ci.yml` wiring the ADR-0003 D1 gate (2026-10-20): `assembleDebug` + Android Lint + `testDebugUnitTest` + detekt + ABI assert (armeabi-v7a only) + APK-size budget + gitleaks secret scan, on PR/push to `main`. Verified green.
- ☑ **Release pipeline** — `.github/workflows/release.yml` (tag-driven signed release, ADR-0003 D2/D6). **Done and validated live end-to-end** with the real signing key (throwaway `v0.0.1-rc1`, since cleaned up). No public release is cut yet — see the owner action at the bottom.

### Feature: Product & test docs
- ☑ PRD v1 — MVP locked to FR-001..FR-009 under NFR-001..NFR-007. 2026-10-20.
- ☑ PRD critics review + **PRD v2** — folded in the 6-lens expert-panel deltas: new §15 Delivery/CI-CD, §16 Design System, §17 Accessibility, §18 Permissions, §19 Testability/Observability/DoD; added FR-011..FR-017 and NFR-008..NFR-023. 2026-10-20.
- ☑ PRD-v2 follow-up ADRs (Hard Rule 6):
  - ☑ **ADR-0003** — CI/CD, signing, release & update/rollback strategy — covers NFR-008..015. Accepted 2026-10-20.
  - ☑ **ADR-0004** — App enumeration without `QUERY_ALL_PACKAGES` (`LauncherApps.getActivityList`) — covers FR-015/016. Accepted 2026-10-20.
  - ☑ **ADR-0005** — "Glass"/translucency technique (fake-glass default; real-time blur gated on-device) — covers NFR-017. Accepted (measurement-gated) 2026-10-20.
- ☑ FR×Test traceability matrix + TEST-PLAN refresh for the Kotlin stack. 2026-10-20.

---

## EPIC: Design System  _(foundational — precedes launcher screens)_
- ☑ **Design System step** (2026-10-20). Semantic `NexaColorScheme` roles behind `NexaTheme` (CompositionLocal), the type scale (body ≥18sp — NFR-016), spacing/radius grid, and a core-component catalog — `SectionHeader` + focusable `AppTile` (D-pad focus ring, no ripple) with `@Preview`s. Debug-only Design System gallery (`src/debug`) renders every token + component. `assembleDebug` + detekt green. *Theme-agnostic by construction:* screens read colors from the active `NexaColorScheme`, so the theme-switch step is a drop-in.

---

## EPIC: Theming — switchable themes, persisted  _(Done)_
- ☑ **Theme catalog** — 4 built-in dark themes, each a complete token set behind `NexaTheme`.
- ☑ **Tile-size options** — selectable grid tile sizing alongside the theme.
- ☑ **Theme persistence** — the selected theme + tile size persist via Proto DataStore (ADR-0008) and re-apply on cold start.
- ☑ **Theme switcher UI** — a D-pad-friendly control that changes theme live; selection takes effect immediately and is restored after reboot.

---

## EPIC: Phase 2 — Minimal Launcher  _(M2 — Done 2026-10-20)_
Covers FR-002, FR-003, FR-006, FR-011, FR-012, FR-014 (partial); full MVP (FR-001..009) completes across M3/M4.
- ☑ App discovery via `LauncherApps.getActivityList(null, user)` across profiles (ADR-0004) — no `QUERY_ALL_PACKAGES`; dedup + sort; icons rasterized off the UI thread (NFR-003). `data/AppEntry` + `data/InstalledAppsRepository`. 2026-10-20.
- ☑ `HomeViewModel` + `HomeUiState` (Loading / Empty / Apps); first frame before discovery finishes (FR-009/FR-014). 2026-10-20.
- ☑ D-pad-navigable app grid (`ui/home/HomeScreen`) on the canonical `AppTile` (icon + monogram fallback, FR-013), initial focus on first tile (FR-006/FR-011), safe-area insets (FR-012), launch on OK with an in-UI, focus-preserving, auto-dismiss error (FR-003/FR-014). 2026-10-20.
- ☑ `assembleDebug` + `lintDebug` + `detekt` green locally. 2026-10-20.
- ☑ **On-device verification** on the real TCL (2026-10-20): first reversible install (`com.nexatv.launcher.debug`, HOME untouched) — no crash; grid renders real icons; D-pad moves one cell/press (FR-006); OK launches the focused app (FR-003); focus ring visible (FR-011); `HOME` returns to the stock launcher. `Modifier.focusRestorer` returns focus to the first tile after a launched app. Details in `docs/technical/ANDROID-TV.md` §3a.

---

## EPIC: Phase 3 — HOME Integration  _(M3 — Done 2026-10-20)_
On this TCL Google TV the normal `ROLE_HOME` path is blocked by firmware (ADR-0006), so HOME is achieved via a reversible accessibility override (ADR-0007). Covers FR-001 / FR-015 on this device. Stock launcher never disabled (Hard Rule 3).
- ☑ On-device validation of the `ROLE_HOME` path (ADR-0006): the RequestRole dialog fires and the role record updates, **but** the effective HOME stays stock Google TV — confirmed across a full reboot. `docs/technical/ANDROID-TV.md` §3b. 2026-10-20.
- ☑ Reversible **accessibility override** (`override/NexaOverrideService` + `xml/nexa_accessibility_config`): watches `TYPE_WINDOW_STATE_CHANGED` from the stock launcher and raises NexaTV over it. Owner enables it in Settings → Accessibility; reversible by toggling off. 2026-10-20.
- ☑ Firmware-independent HOME/override **detection**: `PackageManager.resolveActivity(HOME)` + enabled-accessibility-service check (`isEffectivelyHome()` = default Home **or** override enabled). Grid gates on it. Unit tests green. 2026-10-20.
- ☑ **On-device verification** (TCL): HOME → NexaTV (repeatable, stable, no loop), Back stays on NexaTV, grid populates in override mode. `docs/technical/ANDROID-TV.md` §3c. 2026-10-20. *(Gotcha: this firmware binds the service only via on-device Settings consent; `adb settings put secure enabled_accessibility_services …` is ignored — TCL `AppBoot` `OP_AUTO_START` block.)*
- ⏸ **Accessibility-consent onboarding** — guide the owner to the Settings → Accessibility toggle from the pre-Home prompt (ADR-0007 follow-up). Deferred.
- ⏸ **Override firmware-regression fallback** — secondary-launcher path if an OTA breaks the override (ADR-0007 follow-up). Deferred.

---

## EPIC: Phase 4 — Application Discovery  _(M4 — core Done)_
M4 goal: robust installed-app discovery, launching, favorites, basic categories. Builds on the M2 grid now that NexaTV is effectively HOME (M3), so enumeration is complete (FR-015/016).
- ☑ Live app list updates via `LauncherApps.Callback` (FR-002 ≤2 s). `InstalledAppsRepository.appChanges()` is a `callbackFlow` (registered while collected, unregistered on cancel — no leak); `HomeViewModel` collects it with `collectLatest { reload() }` so a burst of callbacks coalesces into one load. Unit test green; `assembleDebug` green. 2026-10-20.
- ☑ **Persistence ADR** — **ADR-0008 Accepted** 2026-10-20: Proto DataStore for favorites, categories, theme, and settings (owner's choice over Room / Preferences DataStore).
- ☑ **DataStore foundation** (ADR-0008) 2026-10-20: protobuf-lite toolchain wired; `launcher_state.proto` v1; `FavoritesRepository` (favorites `Flow`, transactional toggle/reorder; `FavoriteKey` = component + profile serial so a favorite survives app updates) + serializer + JVM unit tests. Tests + detekt green.
- ☑ **All-apps screen** — full installed-app list reachable from Home. Done.
- ☑ **Home widgets** — calendar + weather widgets on Home (ADR-0010). Done.
- ☑ **Favorites UI** — pin/unpin/reorder with context menu and D-pad reorder. Home state resolves favorites and exposes pin/unpin/reorder; the "Обране" row renders above the grid with first-tile focus; long-press OK context menu (pin/unpin + system App info) via `combinedClickable`; **Перемістити** D-pad reorder mode (◄ ► move, OK commit, BACK cancel), persisted as one `moveFavorite`. **Done — on-device visual check on the TCL still pending (owner's eyes).**
- ⏸ **Basic categories** — group apps (media / games / system) for faster D-pad reach, persisted via the same DataStore. Deferred (owner paused; not on the current agenda).

---

## Deferred milestones (not on the agenda)
- ⏸ **M5** Production TV UX · ⏸ **M6** Configuration & Customization · ⏸ **M7** Media/NAS Integrations · ⏸ **M8** Advanced Integrations.
- ⏸ **NFR-017** glass fps/PSS gate — needs the real glass UI, deferred with M5+.

---

## Owner action before v0.1.0
The release pipeline is Done and validated live. The single remaining pre-release step is **owner-driven: run an on-device smoke test on the TCL, then tag `v0.1.0`** (no public release cut yet).
