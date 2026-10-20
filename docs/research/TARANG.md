> **Source:** Phase 0 research, 2026-10-20 — produced by parallel research agents working from the
> actual repositories. Claims are labelled FACT / INFERENCE / ASSUMPTION / UNKNOWN with source refs.
> Pending human review. Do not reuse any code until the license is confirmed (see LICENSE_ANALYSIS.md).

# Tarang Launcher — Phase 0 Research (NexaTV)

> Every claim is labelled **FACT** (verified in repo), **INFERENCE** (reasoned from evidence), **ASSUMPTION** (plausible, unverified), or **UNKNOWN**.

## Identification

| Field | Value |
|---|---|
| Repo URL | https://github.com/mohitkyadav/tarang-launcher **(FACT)** |
| Description | "Launcher for Android TV" **(FACT)** |
| Commit observed | `fc1d24979eaa5acbf9a18ef18d39b207c945906c` — tag `v0.3.16`, "Bump version to 0.3.16 (versionCode 25)", authored 2026-09-07 **(FACT)** |
| Metadata observed | 2026-10-20; repo `pushed_at` 2026-09-07, `updated_at` 2026-10-14 **(FACT)** |
| Maintainer | `mohitkyadav` (110 commits); `kvaishak` (9 commits) **(FACT)** |
| Popularity | 4 stars, 3 forks, 0 open / 2 closed issues, ~119 commits **(FACT)** |
| Maturity | Pre-1.0 (v0.3.16); created 2026-06-28 **(FACT)**; personal/hobby single-device project **(INFERENCE)** |
| Maintenance | Active but young; ~6 weeks since last code push at observation **(FACT)** |

Note: "Tarang" also matches an unrelated Fire-TV *remote* app (`1aishwaryasharma/tarang-public`) and an OTT content app ("Tarang Plus"). The launcher researched here is `mohitkyadav/tarang-launcher`. **(FACT)**

---

## A. License

- **FACT:** There is **no license**. The GitHub license endpoint returns `404 Not Found`, there is no `LICENSE`/`COPYING` file in the tree, and no SPDX or copyright header appears in any `.kt`/`.kts`/`.md`/`.toml` file.
- **FACT:** The only legal-flavoured text is a self-update design note in `app/build.gradle.kts` about signing keys — not a license.
- **INFERENCE:** With no license, default copyright applies ("all rights reserved"). Nobody has been granted rights to use, copy, modify, fork-and-redistribute, or create derivative works.
  - Commercial use: **not permitted** without explicit permission.
  - Fork / redistribution: GitHub ToS permits viewing and forking *within GitHub*, but grants no redistribution or modification rights beyond that.
  - Source-disclosure / attribution / copyleft: none specified (there is no license to impose or waive terms).
  - Compatibility with a new independent MIT/Apache project: **Not compatible as-is.** You cannot lawfully copy code or assets from this repo into a new MIT/Apache project. You *can* independently reimplement ideas/APIs, or ask the maintainer to add a permissive license.
- **UNKNOWN:** Whether the maintainer intends it to be open-source; the absence may be an oversight. Confirming would require contacting the author.

---

## B. Technology

- **FACT:** **Kotlin 2.0.21**, **Jetpack Compose for TV** (`androidx.tv:tv-material 1.0.0`), Compose BOM 2024.12.01 (`gradle/libs.versions.toml`, `app/build.gradle.kts`).
- **FACT:** UI architecture is **MVVM** with `StateFlow`; `LauncherViewModel` exposes `LauncherUiState`; state combined with `combine`/`debounce`/`stateIn`.
- **FACT:** **Manual DI** via `di/AppContainer.kt` (lazy singletons held by `TarangApp`); comment says "Hilt is the upgrade path."
- **FACT:** **Persistence** = Jetpack **DataStore (Preferences)** — two stores: `tarang_settings` (`SettingsStore`) and `tarang_favorites` (`FavoritesStore`, newline-joined to preserve order). Plus app-list cache and update store.
- **FACT:** **Networking** = raw `java.net.HttpURLConnection` + `org.json` (no Retrofit/OkHttp/Ktor). Used for Open-Meteo weather, ipapi.co geolocation (`ui/Weather.kt`), and GitHub-Releases self-update (`data/UpdateChecker/ApkDownloader/UpdateInstaller`).
- **FACT:** **Android APIs**: `PackageManager` (discovery/launch), `MediaSessionManager` (now-playing), `TvProvider` content URIs via `ContentResolver` (`preview_program`, `watch_next_program`), `RoleManager` (ROLE_HOME), `AccessibilityService`, `NotificationListenerService`, `FileProvider`, DownloadManager, `AGSL RuntimeShader` (A13+), `androidx.palette`.
- **FACT:** **Platform channels/plugins**: none — this is native Android, not Flutter/React Native.
- **FACT:** Build: Gradle KTS, AGP 8.7.3, JVM 17, **minSdk 28 / target+compile 35**, Baseline Profile toolchain (`androidx.benchmark` macro + `profileinstaller`).

---

## C. Architecture

- **FACT:** Two Gradle modules: `:app` and `:baselineprofile` (`settings.gradle.kts`, project name "Tarang Launcher").
- **FACT:** ~7,800 lines of Kotlin. Package layering under `com.tarang.launcher`:
  - `data/` — `AppRepository` (discovery/launch), `FavoritesStore`, `SettingsStore`, `IconLoader`, `AppListCache`, `TvArtwork`, `TvContentProbe`, `SignatureVerifier`, `ApkDownloader`, `UpdateChecker/Installer/Store`.
  - `di/` — `AppContainer` manual graph.
  - `home/` — `HomeActivity`, `HomeSetup`, `HomeRedirectService` (accessibility).
  - `media/` — `TarangNotificationListenerService` (no-op, media-session access), `UiSounds`.
  - `ui/` — Compose screens/components (`LauncherScreen`, `LauncherContent`, `AppRow`, `AppCard`, `AppContextMenu`, `SettingsScreen`, `FrameArt`, `Weather`, `Clock`, `NowPlaying`, `FrostedGlass`, wallpapers, etc.).
  - `viewmodel/` — `LauncherViewModel`.
- **FACT:** UI is 100% Jetpack Compose for TV; no Fragments/XML layouts (only drawable/values resources).
- **INFERENCE:** Thin platform abstraction — data classes wrap framework calls directly; no cross-platform or plugin layer; comments reference an internal "plan §" spec, suggesting a spec-then-build (AI-assisted) workflow.

---

## D. TV-specific

- **FACT (`AndroidManifest.xml`):** `HomeActivity` declares **both** `LEANBACK_LAUNCHER` and `HOME`+`DEFAULT` intent filters; `singleTask`, forced landscape. `uses-feature leanback/touchscreen required=false` so it also runs on phone/emulator.
- **FACT:** App discovery (`AppRepository.loadApps`) queries `CATEGORY_LEANBACK_LAUNCHER` first (TV apps) then `CATEGORY_LAUNCHER` (sideloaded phone apps), dedups by package, hides itself, sorts alphabetically. Requires `QUERY_ALL_PACKAGES`. Live refresh via a `RECEIVER_NOT_EXPORTED` package-change broadcast flow.
- **FACT:** Launching uses `getLeanbackLaunchIntentForPackage` with `getLaunchIntentForPackage` fallback, `FLAG_ACTIVITY_NEW_TASK`, optional scale-up `ActivityOptions`.
- **FACT:** App management: **App info** (`ACTION_APPLICATION_DETAILS_SETTINGS`) and **Uninstall** (`ACTION_DELETE`) deep-links; hide-from-grid in settings.
- **FACT:** **D-pad/remote**: D-pad-first; dock "move mode" reorders via LEFT/RIGHT, UP removes; long-press opens context menu; Frame Art pages photos with LEFT/RIGHT. Navigation sounds optional.
- **FACT:** Reads other apps' home-screen rows via **`READ_TV_LISTINGS`** (`TvArtwork`, `TvContentProbe`) — poster/preview artwork and Watch Next. Code explicitly frames this as an experiment to learn whether a sideloaded launcher is *allowed* to read these rows on real/Google-TV devices (provider rejects WHERE for unprivileged callers, so it scans all rows).
- **FACT:** **TV restrictions handling**: On Google TV the system blocks a third-party app from holding the Home role, so `HomeRedirectService` (accessibility) bounces back to Tarang when the stock launcher surfaces. `HomeSetup` uses `RoleManager.ROLE_HOME` as the authoritative default-home check and deep-links the user to the right settings.
- **FACT:** **System settings shortcuts**: top-bar quick links to Wi-Fi / Android settings; accessibility settings for setup.
- **FACT/INFERENCE:** No `BOOT_COMPLETED` receiver observed — surviving a reboot depends on the OS keeping the Home role / enabled accessibility service.

---

## E. UX

- **FACT (README + `ui/`):** "A home that disappears" — at rest only wallpaper + frosted dock; press **DOWN** to slide up the full app grid (tvOS-style).
- **FACT:** **Frame Art** mode: folder slideshow / single photo / current wallpaper, full-bleed, optional floating clock (position/size/date), weather, cross-fades, Ken-Burns "living painting" drift, night-dim, shuffle. Auto-starts after a configurable idle timeout (doubles as a screensaver "dream").
- **FACT:** **Wallpapers**: gradient presets, user photo, per-favorite app artwork on hover, or Frame Art. Rendered still to save GPU.
- **FACT:** **Animations**: three "personalities" — Default (scale/fly-apart), Glide (springs, no blur), Depth (z-axis dive); plus a **Reduce Motion** toggle.
- **FACT:** **Liquid Glass** AGSL refraction + frosted blur, A13+, **off by default** to protect weak TV GPUs, with a flat-tint fallback.
- **FACT:** **Theme** Light/Dark/Auto (time-of-day). **Density** 4–7 columns (code `MIN_COLUMNS=4`, `MAX_COLUMNS=7`; README says "3 to 7").
- **FACT:** **Focus**: scale springs on focus (`AppCard`), focus retained on the lifted tile during reorder.
- **FACT:** **App management**: long-press menu = favorite/unfavorite, hide, app info, uninstall; hidden apps restorable from Settings.
- **FACT:** **Status bar**: clock+date, Wi-Fi indicator, optional weather + now-playing chip.
- **INFERENCE/UNKNOWN:** No explicit accessibility (TalkBack/screen-reader) support found; content descriptions not audited in depth.

---

## F. Quality

- **FACT:** **No tests** — no `test/` or `androidTest/` source sets; the only test-framework code is `:baselineprofile` (macrobenchmark profile generator).
- **FACT:** **No CI** — no `.github/` workflows.
- **FACT:** **Release process is manual** (`CLAUDE.md`, `README-signing.md`): bump `versionCode`/`versionName`, generate the baseline profile on a Pixel AVD (never on the Chromecast — `pm clear` would wipe DataStore), build + sign a release APK with a stable keystore, publish via `gh release create`. Self-update checks the downloaded APK signature matches (`SignatureVerifier`) and pulls from GitHub Releases.
- **FACT:** **Performance**: ships a baseline profile (`app/src/release/generated/baselineProfiles/`) applied via `profileinstaller`; `CLAUDE.md` documents forcing `cmd package compile -m speed` so sideloads don't "crawl."
- **FACT:** 24+ releases (latest v0.3.16, 2026-09-07); ~119 commits; 2 contributors; 0 open issues.
- **INFERENCE:** Code quality appears deliberate and well-commented (rationale comments, permission notes), but is **unverified by any automated test or benchmark number in the repo**; AI-assisted development (project `CLAUDE.md`, ASD-STE100 writing rules, "plan §" references).

---

## G. Limitations

- **FACT:** **No license** → legally unusable/forkable for a derivative project without permission (see §A).
- **FACT:** No automated tests, no CI; single-maintainer bus factor.
- **FACT:** **Single-device focus** — `CLAUDE.md` targets one Chromecast with Google TV ("sabrina", Android 14, armeabi-v7a). Behavior on other TVs/form factors is **UNKNOWN/untested**.
- **FACT:** No app **categories**, no **recent-apps** row, no dedicated **Continue Watching** carousel (Watch Next data only feeds the screensaver). Grid is a flat alphabetical list.
- **FACT:** No media-server (**Jellyfin/Plex/Kodi**), **NAS**, or **Home Assistant** integration; no **HDMI/input-source** shortcuts; no notification display (the notification listener is a no-op used only to read media sessions).
- **FACT:** The `READ_TV_LISTINGS` content-carousel reading is explicitly an experiment that may be **blocked or empty** on Google TV (server-side rows).
- **FACT:** Network required for weather, auto-location, artwork fetch, and self-update (core launching works offline).
- **INFERENCE:** Screensaver has night-dim but **no explicit OLED pixel-shift / burn-in** mitigation.
- **FACT:** `REQUEST_INSTALL_PACKAGES` + fetching/installing APKs from GitHub Releases is a **trust/security surface** (mitigated by signature-match check, but still self-updating from the network).

---

### Source refs
- Repo tree & files: `README.md`, `CLAUDE.md`, `README-signing.md`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `settings.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/tarang/launcher/**` (AppRepository.kt, AppInfo.kt, SettingsStore.kt, FavoritesStore.kt, TvArtwork.kt, TvContentProbe.kt, di/AppContainer.kt, home/HomeSetup.kt, ui/NowPlaying.kt, ui/Weather.kt, viewmodel/LauncherViewModel.kt).
- GitHub REST: `repos/mohitkyadav/tarang-launcher` (metadata, `license: null`), `/license` (404), `/releases`, `/tags`, `/contributors`, `/issues`.
- Observed via git clone at commit `fc1d249` on 2026-10-20.
