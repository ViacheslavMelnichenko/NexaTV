> **Source:** Phase 0 research, 2026-10-20 — produced by parallel research agents working from the
> actual repositories. Claims are labelled FACT / INFERENCE / ASSUMPTION / UNKNOWN with source refs.
> Pending human review. Do not reuse any code until the license is confirmed (see LICENSE_ANALYSIS.md).

# Yukuza Launcher — Phase 0 Research (NexaTV Discovery)

## Identification

| Field | Value |
|---|---|
| Project | Yukuza Launcher |
| Repo URL | https://github.com/urunkarpm/yukuza-launcher (FACT) |
| Default branch | `master` (FACT) |
| Observed | last code push 2026-06-30 (`pushed_at`); metadata updated 2026-10-12; snapshot observed 2026-10-20; 86 commits; exact HEAD hash UNKNOWN |
| Maintainer | `urunkarpm` (single owner) (FACT) |
| Stars / Forks / Open issues | 10 / 2 / 1 (FACT) |
| Created | 2026-03-18 (FACT) |
| Archived | No (FACT) |
| Maintenance status | Early-stage, low-activity single-maintainer hobby/portfolio project; sporadic maintenance (INFERENCE) |
| Description | "A cinematic Android TV launcher built with Jetpack Compose — aurora background, smart app ordering, live weather & AQI, Now Playing widget" (FACT) |

Note: The project was built using a "superpowers" design-driven workflow (plan + spec markdown under `docs/superpowers/`), suggesting AI-assisted/solo development (INFERENCE).

---

## A. License

- **FACT:** `README.md` states "MIT License — Copyright (c) 2025 Yukuza".
- **FACT:** There is **no `LICENSE` file** anywhere in the repo tree, and the GitHub API `license` field is `null` (GitHub's license detector finds no formal license).
- **INFERENCE:** The MIT grant is declared only as a README line. MIT itself is permissive — allows commercial use, forking, redistribution, no source-disclosure/copyleft, requires preserving the copyright + license notice — and is fully compatible with a new independent MIT/Apache-2.0 project.
- **ASSUMPTION/RISK:** Because no standalone `LICENSE` file exists, the grant is legally weaker/ambiguous than a normal MIT repo. For safe reuse you would want the maintainer to add a proper `LICENSE` file (or confirm intent in writing). Copyright year (2025) predates repo creation (2026-03-18) — minor inconsistency.
- **Attribution:** MIT requires retaining the copyright notice and license text in copies/substantial portions (FACT, from MIT terms).
- **Compatibility with new MIT/Apache project:** Compatible *if* the MIT grant is honored; treat the missing LICENSE file as a due-diligence blocker (INFERENCE).

## B. Technology

- **FACT:** Kotlin 2.0, Jetpack Compose (Compose for TV).
- **FACT:** DI = Hilt; persistence = Room (+ DAOs/entities) and DataStore (preferences); background = WorkManager; networking = Retrofit + Moshi; images = Coil; color = Palette; Kotlinx Immutable Collections; Baseline Profiles.
- **FACT:** minSdk 21; single-activity + Compose Navigation.
- **FACT (APIs):** `LauncherApps`, `PackageManager`, `MediaSessionManager`, `NotificationListenerService`, `WorkManager`, geocoding/Open-Meteo REST.
- Evidence: README tech stack, `gradle/libs.versions.toml`, package tree, design spec.

## C. Architecture

- **FACT:** Clean/layered under `app/src/main/java/com/yukuza/launcher`:
  - `data/` — `db/` (Room `LauncherDatabase`, DAOs: AppOrder, AppLaunchCount, AppColorCache, WeatherCache), `entity/`, `preferences/UserPreferencesManager`, `remote/` (OpenMeteoApi, AirQualityApi, GeocodingApi, GithubReleasesApi) + `dto/`, `repository/` (App, Media, Network, Update, Weather), `service/MediaNotificationListenerService`, `worker/` (PackageChangeSync, PalettePreWarm, WeatherSync).
  - `di/` — Hilt modules (App, CoroutineDispatchers, Database, Network, Repository, Worker).
  - `domain/` — `model/` + `usecase/` (GetApps, GetVisibleApps, ReorderApps, IncrementLaunchCount, GetWeather, GetAqi, GetNetworkSpeed, GetMediaSession, CheckUpdate).
  - `navigation/LauncherNavGraph`; `ui/` (components, overlays, screens+ViewModels, theme); `util/Logger`.
- **FACT (UI arch):** MVVM-style — ViewModels expose `StateFlow<ImmutableList<T>>`; `derivedStateOf`; `@Immutable`/`@Stable`. Single-activity; two nav destinations (HomeScreen, AppListScreen/drawer).
- **FACT (platform abstraction):** repositories wrap platform services (e.g., MediaRepository exposes `Flow<MediaMetadata?>` over MediaSessionManager for testability).

## D. TV-specific

- **FACT (HOME/launcher registration):** `MainActivity` intent-filter = `MAIN` + `CATEGORY_HOME` + `CATEGORY_DEFAULT` + `CATEGORY_LEANBACK_LAUNCHER`. `launchMode=singleTask`, `exported=true`, `screenOrientation=landscape`. uses-feature `android.software.leanback` required=true, `touchscreen` required=false.
  - **NOTE (FACT):** `CATEGORY_LAUNCHER` and a `banner` attribute are **not** declared (banner is the conventional TV launcher icon).
- **FACT (app discovery):** `LauncherApps.getActivityList()` + `PackageManager.getApplicationIcon()`; shortcuts via `LauncherApps.getShortcuts()`; `PackageChangeBroadcastReceiver` for PACKAGE_ADDED/REMOVED; `PackageChangeSyncWorker`. Queries on Dispatchers.IO.
- **FACT (launching):** standard intent resolution; Assistant button uses ordered intent chain with toast fallback.
- **FACT (D-pad / focus / remote):** D-pad navigable rows (`AppRow` LazyRow); focus animations (greyscale→color spring, lift, glow/white ring); drawer up-from-top returns focus to search bar; natural reading-order focus traversal.
- **FACT (input shortcuts):** Quick Settings overlay with HDMI-CEC Input Source + Game Mode — **hidden on standard non-privileged builds** (requires privileged permission).
- **FACT (uninstall/app info):** Edit Mode / AppContextMenu offers Uninstall/Remove; `REQUEST_INSTALL_PACKAGES` declared.
- **FACT (system settings):** `WRITE_SETTINGS` (volume/brightness/night mode), TV Settings pinned first.
- **FACT (restrictions handled):** blur degrades below API 31; GMS-dependent features (location/voice) have fallbacks (LocationManager / manual city).
- **FACT (limitation):** cannot replace system launcher on Fire TV in v1 (sideload-only).
- **INFERENCE:** `RECEIVE_BOOT_COMPLETED` permission is declared but **no `BOOT_COMPLETED` receiver is registered** in the manifest — boot-time sync restart may not actually fire.

## E. UX

- **FACT:** Cinematic 10-foot UI — animated aurora background (5 GPU-composited Canvas blobs, no video), monochrome icons springing to color on focus with lift + dominant-color glow + white focus ring.
- **FACT:** Top-bar widgets (Clock, Weather, AQI, Screen Timer, Network speed); Now Playing media widget (spinning album art + progress); overlays (Quick Settings, City Picker with geocoding D-pad search, App Context Menu/Shortcuts, Input Source, Update Dialog).
- **FACT:** App ordering by launch frequency + manual reorder/pin; ambient screensaver (clock over aurora).
- **FACT:** Localization — English + Spanish (`values-es`), RTL support, `locales_config.xml`.
- **FACT (non-goals):** no categories/folders.
- **INFERENCE:** No documented accessibility (TalkBack/content descriptions) beyond default Compose behavior.

## F. Quality

- **FACT (CI):** `.github/workflows/android-ci.yml` on push/PR to `main`+`develop`; JDK 17 Temurin; runs `./gradlew build`, `test`, `lint`; uploads debug APK. Does **not** run instrumented tests or benchmarks.
- **FACT (tests):** Unit tests (`app/src/test`): repository tests (App, Network, Update, Weather), ViewModel tests (Home, AppList), GetWeather use case, NightModeSchedule, AppInfo; test util `MainDispatcherRule`. Instrumented/Compose UI tests (`app/src/androidTest`): LauncherDatabase, AppIcon, AssistantButton, AuroraBackground, GlassCard, HomeScreen, NowPlayingWidget, QuickSettingsOverlay.
- **FACT (benchmarks):** `macrobenchmark` module with `StartupBenchmark.kt`; Baseline Profiles in app.
- **FACT (process):** Design-driven — `docs/superpowers/plans/*` and `specs/*`. No tagged releases observed.
- **INFERENCE (maturity/maintenance):** Early v1, solo maintainer, 10 stars / 2 forks / 1 open issue / 86 commits; last code push 2026-06-30 — low adoption and low ongoing activity.

## G. Limitations

- **FACT (out of scope v1):** custom wallpapers, multi-user profiles, app folders/grouping/categories, notification badges, kids mode/app lock, icon pack support, cloud/preference sync.
- **FACT:** HDMI-CEC/Input Source/Game Mode hidden without privileged permission; no Fire TV HOME replacement; blur degraded below API 31; GMS features need fallbacks.
- **FACT:** No `LICENSE` file despite README MIT claim.
- **FACT:** Screensaver dims text but does not pixel-shift → not fully OLED burn-in safe.
- **INFERENCE:** No BOOT_COMPLETED receiver registered → questionable reboot reliability of background sync.
- **INFERENCE:** Not fully offline — weather/AQI/update-check require network (Room caches last weather only).
- **FACT/INFERENCE:** No media-server (Jellyfin/Plex/Kodi), NAS, or Home Assistant integrations; no Continue Watching; no published performance/resource benchmark numbers.

---

### Relevance to NexaTV (INFERENCE)
Yukuza is a modern Compose-for-TV reference for: leanback HOME registration, `LauncherApps`-based discovery, D-pad focus patterns, media-session Now Playing, and a clean Hilt/Room/DataStore architecture with Baseline Profiles. Caveats for reuse: missing `LICENSE` file (legal blocker until resolved), no CATEGORY_LAUNCHER/banner, declared-but-unregistered boot receiver, no categories/custom backgrounds, and no media-server/NAS/Home-Assistant integrations that NexaTV may need to build from scratch.

### Sources (FACT basis)
- Repo root + README: https://github.com/urunkarpm/yukuza-launcher ; https://raw.githubusercontent.com/urunkarpm/yukuza-launcher/master/README.md
- GitHub API metadata + file tree: https://api.github.com/repos/urunkarpm/yukuza-launcher ; .../git/trees/master?recursive=1
- Manifest: https://raw.githubusercontent.com/urunkarpm/yukuza-launcher/master/app/src/main/AndroidManifest.xml
- CI: https://raw.githubusercontent.com/urunkarpm/yukuza-launcher/master/.github/workflows/android-ci.yml
- Design spec: docs/superpowers/specs/2026-03-18-yukuza-launcher-design.md
- Screensaver source: app/src/main/java/com/yukuza/launcher/ui/components/AmbientScreensaver.kt
