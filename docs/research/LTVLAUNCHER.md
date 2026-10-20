> **Source:** Phase 0 research, 2026-10-20 — produced by parallel research agents working from the
> actual repositories. Claims are labelled FACT / INFERENCE / ASSUMPTION / UNKNOWN with source refs.
> Pending human review. Do not reuse any code until the license is confirmed (see LICENSE_ANALYSIS.md).

# Research: LTvLauncher and LTvLauncher Extended

> Phase 0 discovery for NexaTV. Every claim is labelled **FACT** (verified from the actual repo), **INFERENCE**, **ASSUMPTION**, or **UNKNOWN**. Sources are linked inline.

## Identification

| Field | Value |
|---|---|
| Base repo URL | https://github.com/LeanBitLab/LtvLauncher (org also referenced as `leanbitlab-org`) |
| Extended repo URL | https://github.com/niposch/LTvLauncher-Extended |
| Commit/date observed | Base: latest commit **2026-10-20**, app version **2026.10.03+8112**; Extended: release **v2026.10.01** |
| Maintainer | Base: **LeanBitLab** org; Extended: **niposch** (single maintainer) |
| Upstream chain (FACT) | FLauncher by **etienn01 / Étienne Fesser** (GitLab, original) → **osrosal** FLauncher fork (GitHub) → **LeanBitLab/LtvLauncher** → **niposch/LTvLauncher-Extended** |
| Maintenance status | Base: **actively maintained** (feature batch 2026-10-02, daily badge automation). Extended: **new fork, low traction** (~0 stars/forks at observation). |
| Stars/forks (FACT) | Base ~392 stars / ~68 forks / ~797 commits; Extended ~0/0 |
| Internal package | Base Dart project still named `flauncher`; Extended app id `com.niposch.ltvlauncher.extended` |

---

## A. License

- **FACT** — License is **GNU General Public License v3.0 (GPL-3.0)**, verified from the base repo [`LICENSE`](https://raw.githubusercontent.com/LeanBitLab/LtvLauncher/master/LICENSE) (standard GPLv3 header, "Version 3, 29 June 2007") and GitHub sidebar. README states the same. Extended repo README also declares GPL-3.0.
- **FACT** — Source files carry GPL headers ("redistribute/modify under the terms of the GNU General Public License as published by the Free Software Foundation"), seen in `AndroidManifest.xml` header and channel source.
- **Commercial use** — INFERENCE: GPLv3 permits commercial use/sale.
- **Fork/redistribution** — FACT: redistribution and forking are permitted (the project is itself a 4-level fork chain), but derivatives **must remain GPL-3.0** and distribute complete corresponding source.
- **Source disclosure** — INFERENCE: strong copyleft — distributing binaries or derivatives obligates offering full source under GPL-3.0.
- **Attribution** — FACT: notices must be preserved; repos credit etienn01, osrosal, LeanBitLab, and hamishakl (Extended `ATTRIBUTION.md`).
- **Compatibility with a new MIT/Apache NexaTV** — **INFERENCE (high confidence): INCOMPATIBLE for code reuse.** Copying or forking this GPLv3 code would force NexaTV's derivative to be GPL-3.0; a permissive (MIT/Apache) NexaTV cannot absorb this code and remain permissive. Apache-2.0 code can be pulled *into* a GPLv3 project, not the reverse. To stay MIT/Apache, NexaTV must do a **clean-room reimplementation of ideas, not code**.

## B. Technology

- **FACT** — **Flutter / Dart** app. [`pubspec.yaml`](https://raw.githubusercontent.com/LeanBitLab/LtvLauncher/master/pubspec.yaml): Flutter `3.24.5`, Dart `>=3.4.3`.
- **FACT** — State management + DI: **`provider`** (^6.0.5), service/ChangeNotifier pattern.
- **FACT** — Persistence: **`drift`** (^2.14.1) reactive SQLite ORM + `sqlite3_flutter_libs`, plus **`shared_preferences`** for key-value settings; `path`/`path_provider` for storage locations.
- **FACT** — Other deps: `image_picker`, `intl`, `google_fonts`, `screen_brightness`, `package_info_plus`, `share_plus`, `qr_flutter`, `pool`.
- **FACT** — Native bridge: a single **MethodChannel + EventChannels** in [`flauncher_channel.dart`](https://raw.githubusercontent.com/LeanBitLab/LtvLauncher/master/lib/flauncher_channel.dart). Event streams: apps-changed, network-changed, notifications-changed, weather-changed, watch-next-changed.
- **FACT** — Networking: **no HTTP client declared in base pubspec** (`network_image_mock` is test-only). Extended adds networked weather/Jellyfin/Seerr via its own code.
- **INFERENCE** — Native Android side is Kotlin/Java implementing the channel (PackageManager, TvInputManager, NotificationListenerService, DreamService, AccessibilityService, usage stats).

## C. Architecture

- **FACT** — Single Flutter module. [`lib/`](https://github.com/LeanBitLab/LtvLauncher/tree/master/lib) contains `main.dart`, `flauncher.dart`, `flauncher_app.dart`, `flauncher_channel.dart`, `database.dart`/`database.drift.dart`, `actions.dart`, `custom_traversal_policy.dart`, `app_image_type.dart`, `gradients.dart`, and folders `models/`, `providers/`, `widgets/`, `l10n/`.
- **FACT** — [`lib/providers/`](https://github.com/LeanBitLab/LtvLauncher/tree/master/lib/providers): `apps_service`, `backup_service`, `brightness_service`, `launcher_state`, `network_service`, `notifications_service`, `settings_service`, `tv_inputs_service`, `wallpaper_service`, `watch_next_service`, `weather_service`. Clear service-per-concern layering.
- **FACT** — UI architecture: Flutter widgets with a **custom focus traversal policy** (`custom_traversal_policy.dart`) and an `actions.dart` intent/action layer — idiomatic leanback D-pad handling.
- **FACT** — Platform abstraction: all native capability funneled through one channel class (good single integration seam).
- **INFERENCE** — Data flow: Drift DB + prefs → provider services → widgets; native events pushed via EventChannels keep app list/notifications/weather reactive.

## D. TV-specific (from [`AndroidManifest.xml`](https://raw.githubusercontent.com/LeanBitLab/LtvLauncher/master/android/app/src/main/AndroidManifest.xml) and channel)

- **FACT** — HOME intent + launcher registration: MainActivity `MAIN` intent-filter with categories **HOME, LEANBACK_LAUNCHER, LAUNCHER, DEFAULT**; `singleTask`, landscape. `android.software.leanback` **required**; touchscreen **not required**.
- **FACT** — App discovery: `getApplications` via PackageManager, with Android 11+ `<queries>` (MAIN, GET_CONTENT, and `org.breezyweather`). Icons/banners via `getApplicationIcon`/`getApplicationBanner`.
- **FACT** — Launching: `launchApp`, `launchActivityFromAction`.
- **FACT** — Uninstall/app info: `uninstallApp` (`REQUEST_DELETE_PACKAGES`), `openAppInfo`.
- **FACT** — D-pad/focus/remote: `custom_traversal_policy.dart`, leanback; `playClickSound` audio feedback.
- **FACT** — TV restrictions/inputs: `getTvInputs`/`launchTvInput` (HDMI/AV/component selector); `READ_TV_LISTINGS`, `READ_EPG_DATA`.
- **FACT** — Watch Next / Continue Watching: `getWatchNextPrograms`/`launchWatchNextProgram`/`deleteWatchNextProgram` (`READ_WRITE_WATCH_NEXT_PROGRAMS`).
- **FACT** — System settings: `openSettings`, `openWifiSettings`, `openVpnSettings`, `openDefaultLauncherSettings`, `isDefaultLauncher`.
- **FACT** — Services/receivers: `ClockScreensaverService` (DreamService), `LauncherNotificationListenerService`, `LauncherAccessibilityService` ("Home Button Fix" for Google/Fire TV), `BootReceiver`, `WeatherReceiver`.
- **FACT** — Extra permissions: `PACKAGE_USAGE_STATS`, `SYSTEM_ALERT_WINDOW` (overlays), `WRITE_SETTINGS`, network/WiFi state + `CHANGE_WIFI_STATE`.

## E. UX (from READMEs & release notes)

- **FACT** — Row-based leanback home: Continue Watching rows (recency-sorted, theme-adaptive), customizable categories.
- **FACT** — Theming: card themes (Modern/Default, Premium, Classic, Capsule; Oct-2026 added Glow/Squircle/Minimal), accent colors, dynamic / time-based / pitch-black / OLED wallpapers, custom banners.
- **FACT** — Status bar: clock, weather pill, daily/weekly/monthly data-usage widget, WiFi settings shortcut.
- **FACT** — Notifications: floating overlays + drawer, blocked-apps manager.
- **FACT** — OLED screensaver with 30s clock position shifting (burn-in protection).
- **FACT** — Focus animations; keyboard-free "quick presets" for time/date formats and category names; backup/restore.
- **UNKNOWN** — Accessibility beyond D-pad focus (screen reader, text scaling) not documented.

## F. Quality

- **FACT** — Test suite is **moderate and organized** ([`test/`](https://github.com/LeanBitLab/LtvLauncher/tree/master/test)): `actions_test`, `database_test`, `database_migration_test`, `flauncher_channel_test`, `flauncher_test`, `custom_traversal_policy_test`, perf/benchmark tests (`apps_service_perf_test`, `apps_service_benchmark_v2`, `find_target_category_benchmark`), `network_service_debug_test`, plus `models/`, `providers/`, `widgets/` subdirs and `helpers.dart`/`mocks.dart`/`flutter_test_config.dart`.
- **FACT** — CI [`release.yml`](https://raw.githubusercontent.com/LeanBitLab/LtvLauncher/master/.github/workflows/release.yml): on `v*` tags / manual dispatch — checkout, Java 21, SQLite, Flutter 3.24.5, `pub get`, `build_runner` mocks, drift schema gen, conditional keystore signing, builds universal + arm64-v8a + armeabi-v7a APKs, generates release notes, publishes GitHub release. `badges.yml` auto-updates stat badges.
- **FACT (GAP)** — Release workflow does **not** run `flutter test` or `flutter analyze` — tests exist but are **not enforced in CI**.
- **FACT** — Maturity/activity: base ~797 commits, ~392 stars, actively maintained through 2026-10-20 (also distributed via IzzyOnDroid + AFTVnews Downloader code 7259827). Extended is brand-new with ~0 community traction.

## G. Limitations

- **FACT** — Extended media integration is **Jellyfin + Seerr only** (via [`docs/extensions.md`](https://raw.githubusercontent.com/niposch/LTvLauncher-Extended/master/docs/extensions.md) / `fork_config.json`); **no Plex, Kodi, NAS, or Home Assistant**. Each media target app must be installed to open items.
- **FACT** — No dedicated "recent apps" row distinct from Watch Next; **Favorites** not confirmed (UNKNOWN).
- **FACT (GAP)** — No automated test/analyze gate in CI.
- **INFERENCE** — "Fast startup" / "low resource" are stated goals with some benchmark tests, but not independently measured here (Partial).
- **INFERENCE** — Layout is theme/color-customizable but the underlying row structure is largely fixed.
- **FACT** — Base project internally still named `flauncher`.
- **INFERENCE** — Extended fork is single-maintainer with minimal review surface and adoption.

---

### Net takeaway for NexaTV

- **FACT/INFERENCE** — Both repos are **GPL-3.0**. They are an excellent **reference/feature-benchmark** for an Android TV launcher (mature native channel surface for app discovery, launch, uninstall, TV inputs, Watch Next, notifications, settings, screensaver). But **direct code reuse is incompatible with a permissive MIT/Apache NexaTV** — any copied/forked code forces GPL-3.0. A permissive NexaTV must **clean-room reimplement** these capabilities rather than lift source.
