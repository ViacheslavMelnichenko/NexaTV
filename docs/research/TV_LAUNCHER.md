> **Source:** Phase 0 research, 2026-10-20 — produced by parallel research agents working from the
> actual repositories. Claims are labelled FACT / INFERENCE / ASSUMPTION / UNKNOWN with source refs.
> Pending human review. Do not reuse any code until the license is confirmed (see LICENSE_ANALYSIS.md).

## Phase 0 Research — Native (Kotlin/Compose) TV Launcher Candidates

Two repositories were located and verified against their actual source trees, READMEs, LICENSE files, manifests, build catalogs, and GitHub API metadata. **Primary candidate: `nielsvanvelzen/tv-launcher`** (the genuine general-purpose Android-TV launcher and the native counterpoint to Flutter FLauncher). **Secondary: `karuhun-developer/LauncherCompose`** (a hotel/IPTV guest launcher — strong architecture, narrow purpose).

> Label key: **FACT** = verified in repo · **INFERENCE** = reasoned from evidence · **ASSUMPTION** = plausible/unverified · **UNKNOWN** = not verifiable.

---

### Identification

| Field | nielsvanvelzen/tv-launcher | karuhun-developer/LauncherCompose |
|---|---|---|
| Repo URL | https://github.com/nielsvanvelzen/tv-launcher | https://github.com/karuhun-developer/LauncherCompose |
| Observed commit / date | `412929b`, pushed 2026-10-15 (observed 2026-10-20) | `7c47e53`, pushed 2025-12-07 (observed 2026-10-20) |
| Created | 2021-04-25 | 2025-06-23 |
| Maintainer | Niels van Velzen (@hetisniels; Jellyfin-Android-TV contributor) — single maintainer | karuhun-developer (small team; Telegram community) |
| License | **GPL-3.0** (FACT: LICENSE + API `spdx_id`) | **Apache-2.0** (FACT: LICENSE + README) |
| Stars / Forks / Open issues | 496 / 63 / 9 | 35 / 13 / 0 |
| Maintenance status | Active (commits to Oct 2026, mostly Renovate bumps); self-labeled "early development stage" | Maintained, lower cadence; last commit Dec 2025 |
| Language | Kotlin (FACT) | Kotlin (FACT) |

---

### A. License

- **FACT** — `tv-launcher` is **GPL-3.0** (`LICENSE` = GNU GPL v3; API `license.spdx_id = GPL-3.0`). `LauncherCompose` is **Apache-2.0** (`LICENSE` + README "Distributed under the Apache License 2.0").
- **Commercial use:** Both allow it. GPL-3.0 permits commercial use but with strong copyleft; Apache-2.0 permits it with minimal obligations.
- **Fork / redistribution:** Both allow. GPL-3.0 requires derivative/combined works to be GPL-3.0 with corresponding source offered. Apache-2.0 allows permissive redistribution, including inside proprietary products.
- **Source disclosure:** GPL-3.0 (`tv-launcher`) = **required** on distribution. Apache-2.0 (`LauncherCompose`) = **not required**.
- **Attribution:** GPL-3.0 preserve notices; Apache-2.0 retain NOTICE + state changes.
- **Compatibility with an independent MIT/Apache NexaTV (INFERENCE, critical):** **`tv-launcher` GPL-3.0 is INCOMPATIBLE for code reuse** — importing its code would force NexaTV to become GPL-3.0 and disclose source. Treat it as *reference / clean-room design inspiration only*. **`LauncherCompose` Apache-2.0 is COMPATIBLE** — code reusable with attribution — but its hotel/IPTV scope limits how much is reusable.

### B. Technology

- **`tv-launcher` (FACT, `gradle/libs.versions.toml`):** Kotlin 2.4.21, **Jetpack Compose with Compose-for-TV** (`androidx.tv:tv-material` 1.1.0). DI = **Koin** 4.2. Persistence = **SQLDelight** 2.4 (`.sq` schemas: App, Channel, ChannelProgram, Input) — *not* Room. Images = **Coil** 2.7. Serialization = kotlinx.serialization. Logging = Timber. Navigation = androidx.navigation3. `minSdk 24`, `compile/targetSdk 37`, JDK 21. No Retrofit/heavy networking; only `INTERNET` + `READ_TV_LISTINGS` + boot/storage permissions (manifest).
- **`LauncherCompose` (FACT, README + `settings.gradle.kts`):** Kotlin + Jetpack Compose on the **Now-in-Android** template. DI = **Dagger Hilt**. Networking = **Retrofit**. Persistence = **Room** + **Proto DataStore**. Coil, Firebase (analytics disabled by default), coroutines/Flow. Requires an external backend (`github.com/karuhun-developer/hotel`).

### C. Architecture

- **`tv-launcher` (FACT):** Single `:app` module, cleanly layered. `data/` holds platform **Resolver** classes abstracting Android system services — `AppResolver` (PackageManager), `InputResolver` (TvInputManager), `ChannelResolver` (TvProvider) — plus SQLDelight repositories and `PackageChangeReceiver`. `ui/` holds Compose screens: a `LauncherScreen`, a `Home` tab (favorites + channel/watch-next rows), an `Apps` tab (all-apps grid), and a configurable `Toolbar`. MVVM via per-tab ViewModels. Platform abstraction is thin but real (resolvers → SQLDelight → Compose).
- **`LauncherCompose` (FACT):** Full NiA modularization — `build-logic` convention plugins; `core/*` (common, data, database, network, datastore, domain, designsystem, ui, model) and `feature/*` (home, main-menu, content, screensaver, hotel-profile, restaurant, weather, onboarding) + `sync/work`. Per-feature data/domain/ui split. Architecturally the more mature of the two, but purpose-built for hotels.

### D. TV-specific (FACT unless noted)

- **HOME intent / launcher registration (`tv-launcher`):** `LauncherActivity` intent-filter = `MAIN` + categories `HOME`, `LAUNCHER`, `LEANBACK_LAUNCHER`, `DEFAULT`, `MONKEY`; `singleTask`, `excludeFromRecents`, `stateNotNeeded`. Default-launcher managed via **RoleManager `ROLE_HOME`** (`DefaultLauncherHelper`, API 29+). `LauncherCompose` registers `HOME` + `LEANBACK_LAUNCHER`.
- **App discovery:** `AppResolver.getApplications()` queries `queryIntentActivities` for `LEANBACK_LAUNCHER` + `LAUNCHER`, dedup by package; live-refreshed by `PackageChangeReceiver` (PACKAGE_ADDED/CHANGED/REPLACED/REMOVED + BOOT_COMPLETED). `LauncherCompose` uses `QUERY_ALL_PACKAGES` + `ApplicationLauncherImpl`.
- **Launching:** `getLeanbackLaunchIntentForPackage()` with fallback to `getLaunchIntentForPackage()`; banner via `getActivityBanner` → `getActivityIcon` fallback (`AppExtensions.kt`).
- **D-pad / focus / remote keys:** Compose-for-TV; `Toolbar.kt` uses `focusRestorer`+`FocusRequester`; `util/modifier/autoFocus.kt`.
- **HDMI / TV inputs:** `InputResolver` via `TvInputManager`; `ToolbarInputsButton` + `createSwitchIntent`; needs `READ_TV_LISTINGS`.
- **Channels / Watch-Next:** `ChannelResolver` via `androidx.tvprovider`; models `ChannelType`, `ChannelProgramType`, `ChannelProgramAspectRatio`.
- **Uninstall / app-info / system settings:** `ToolbarSettingsButton` defers to system settings. **UNKNOWN/No** in-app uninstall or app-info action — the only per-app controls are favorite-toggle and reorder (`AppPopup.kt`).

### E. UX (FACT unless noted)

- **`tv-launcher`:** Top toolbar = tabs | spacer | inputs button | settings button | clock. Home tab shows favorite app cards + channel/watch-next rows; Apps tab is an all-apps grid. Favorites toggled/reordered through a focus popup (left/right move + heart). INFERENCE: minimal, early-stage; no categories, no wallpaper.
- **`LauncherCompose`:** Onboarding, screensaver, weather, content/restaurant menus, hotel profile — guest-facing hotel UX, not a general app grid (README screenshots + feature modules).
- **Accessibility:** UNKNOWN for both (no a11y-specific code or tests observed).

### F. Quality

- **`tv-launcher` (FACT):** GitHub Actions `app-build.yaml` builds debug APKs on push/PR; Renovate keeps deps fresh (recent commits are dependency bumps). **No test/androidTest/benchmark directories exist — effectively no automated tests.** No Releases or tags published. 496★/63 forks/9 issues; actively maintained but self-described early-stage.
- **`LauncherCompose` (FACT):** Inherits NiA quality scaffolding — Jacoco, baseline/benchmark profiles, Spotless, dedicated `:lint` module, Gradle Managed Devices, convention plugins; however test files are largely NiA `Example*Test` stubs. **No GitHub Actions workflows present**, no Releases. 35★/13 forks/0 issues; last commit Dec 2025 (INFERENCE: lower/paused cadence).

### G. Limitations

- **`tv-launcher` (FACT):** **No wallpaper/custom background and no search** — both declared unimplementable on Android TV (README "Unplanned features"; no `WallpaperManager` actions). Early-stage: no categories, no recents row, no in-app settings screen, no media/Jellyfin/Plex/weather/Home-Assistant/NAS integration, no released binary, no tests. **INFERENCE: GPL-3.0 is the decisive blocker for reuse in an MIT/Apache NexaTV.**
- **`LauncherCompose` (FACT):** Hotel/IPTV-specific; depends on an external backend + Firebase + `QUERY_ALL_PACKAGES` + cleartext traffic; not a general consumer launcher.
- **Both — UNKNOWN:** real-world TV-reboot reliability and startup/resource benchmarks (unpublished).

---

### Feature matrix (primary = `tv-launcher`; LauncherCompose noted in evidence)

| Feature | Value | Evidence |
|---|---|---|
| App discovery | Yes | `AppResolver.kt` (LEANBACK+LAUNCHER queries) + `PackageChangeReceiver`; LauncherCompose uses QUERY_ALL_PACKAGES |
| Categories | No | Only Home/Apps tabs, flat grid — no grouping |
| Favorites | Yes | README favorites; `App.favoriteOrder`; `AppPopup.kt` heart toggle |
| Reordering | Yes | `home/AppPopup.kt` `onMove(-1/+1)`; reorderable toolbar items |
| D-pad navigation | Yes | Compose-for-TV (`androidx.tv:tv-material`) |
| Focus handling | Yes | `Toolbar.kt` focusRestorer/FocusRequester; `autoFocus.kt` |
| Recent apps | No | No recents row; activity `excludeFromRecents` |
| Continue Watching | Partial | Channels/Watch-Next via `androidx.tvprovider` (`ChannelResolver`) — early |
| Weather | No | Absent in tv-launcher (LauncherCompose has `feature:weather`) |
| Clock | Yes | `ToolbarClock.kt` |
| HDMI / input shortcuts | Yes | `InputResolver` (TvInputManager) + `ToolbarInputsButton` |
| System settings shortcuts | Yes | `ToolbarSettingsButton.kt` |
| Screensaver (OLED-friendly) | No | Absent in tv-launcher (LauncherCompose has `feature:screensaver`; OLED claim unverified) |
| Notifications | No | No notification handling |
| Media playback info | No | No media-session/now-playing code |
| Media integration (Jellyfin/Plex/Kodi) | No | Generic launcher; no app-specific integration |
| NAS integration | No | None in either repo |
| Home Assistant integration | No | None in either repo |
| Custom backgrounds | No | README: wallpapers impossible on Android TV |
| Customizable layout | Partial | Configurable toolbar + favorites reorder; no full layout editor |
| Animations | Partial | INFERENCE: Compose-for-TV default focus animations |
| Configuration / settings | Partial | No in-app settings screen; defers to system settings |
| Offline-first core | Yes | PackageManager discovery + SQLDelight local store; no backend (LauncherCompose needs backend) |
| Fast startup | Unknown | No benchmarks published |
| Low resource usage | Unknown | No benchmarks published |
| Minimal / no advertising | Yes | FOSS GPL-3.0; no ad/analytics libs in version catalog |
| TV reboot reliability | Partial | HOME category + BOOT_COMPLETED receiver present; reliability unverified |

---

### Phase 0 recommendation (INFERENCE)

- **`nielsvanvelzen/tv-launcher`** is the best *native* reference for how a lean Compose-for-TV launcher registers as HOME, discovers/launches apps, switches HDMI inputs, and surfaces TV channels — but **its GPL-3.0 license bars code reuse in an MIT/Apache NexaTV.** Use for architecture/clean-room inspiration only.
- **`karuhun-developer/LauncherCompose`** is **Apache-2.0 (reusable with attribution)** and demonstrates a production-grade modular Compose architecture (Hilt/Room/Retrofit, convention plugins, baseline profiles), but it is hotel/IPTV-specific and backend-dependent, so only its architecture patterns and generic launcher plumbing (`ApplicationLauncherImpl`) are directly reusable.

**Sources:** repo READMEs, `LICENSE` files, `AndroidManifest.xml`, `gradle/libs.versions.toml`, `settings.gradle.kts`, `.github/workflows/app-build.yaml`, Kotlin source under `app/src/main/kotlin/...`, and GitHub REST API metadata (both repos), observed 2026-10-20.
