> **Source:** Phase 0 research, 2026-10-20 — produced by parallel research agents working from the
> actual repositories. Claims are labelled FACT / INFERENCE / ASSUMPTION / UNKNOWN with source refs.
> Pending human review. Do not reuse any code until the license is confirmed (see LICENSE_ANALYSIS.md).

# FLauncher — Phase 0 Research (NexaTV)

## Identification

| Field | Value |
|---|---|
| Project | FLauncher |
| Repository (canonical) | https://gitlab.com/flauncher/flauncher — FACT (hint github.com/flauncher/flauncher returns 404; upstream is GitLab) |
| Original URL | gitlab.com/etienn01/flauncher (redirected to flauncher/flauncher) — FACT |
| Maintainer | Étienne Fesser (etienn01) — FACT (LICENSE/source © 2021 Étienne Fesser) |
| Package id | me.efesser.flauncher — FACT (AndroidManifest / Google Play) |
| Observed | master, latest commit 2023-03-03; latest release 0.18.0 (2023-04-21); observed 2026-10-20 — FACT |
| License | GNU GPL v3.0 — FACT (LICENSE file) |
| Maintenance status | Dormant / minimally maintained (no commits since 2023-03-03) — INFERENCE |
| Distribution | Google Play (`me.efesser.flauncher`) + GitLab releases (APK) — FACT |

Notable forks (FACT, from search/repo listings): `zompinc/flauncher` (mouse support, Firebase removed), `CameronJGrant/LtvLauncher` & `leanbitlab-org/LtvLauncher` (OLED screensaver, WiFi widget), `osrosal/flauncher`, `4v3ngR/aLauncher`, `svrooij/flauncher` (mirror).

---

## A. License

- **Type:** GNU General Public License v3.0 — FACT (`/LICENSE`).
- **Commercial use:** Permitted under GPL-3.0 (no fee restriction) — FACT, but any distributed derivative must also be GPL-3.0 — FACT.
- **Fork / redistribution:** Allowed; derivatives must ship under GPL-3.0, carry modification notices + dates, and preserve copyright/license notices — FACT. The ecosystem of public forks confirms this in practice — FACT.
- **Source disclosure:** Strong copyleft — distributing object code (e.g., APK) obligates providing the Corresponding Source — FACT. Includes anti-DRM "Installation Information" for User Products — FACT.
- **Attribution:** Must retain notices and state modifications — FACT.
- **Compatibility with a new MIT/Apache project:** **Not compatible for code reuse.** GPL-3.0 code cannot be incorporated into a permissively (MIT/Apache) licensed independent project without relicensing the whole work under GPL-3.0 — INFERENCE (direct consequence of GPL copyleft). NexaTV may study FLauncher for ideas/architecture but must **not copy GPL code** into an MIT/Apache codebase — INFERENCE.

## B. Technology

- **Language/framework:** Flutter/Dart app + native Kotlin host — FACT (`pubspec.yaml`, `MainActivity.kt`). Dart SDK `>=2.12 <3.0`, Flutter `3.7.5` — FACT.
- **State management:** `provider` + `ChangeNotifier` (AppsService, SettingsService, WallpaperService) — FACT.
- **Persistence:** `drift` (SQLite ORM) with WAL journal mode + versioned migrations (schema v1–v5); `shared_preferences`; `path_provider`; `sqlite3_flutter_libs` — FACT.
- **Platform channels:** method `me.efesser.flauncher/method`, event `me.efesser.flauncher/event` — FACT.
- **Networking/other deps:** `unsplash_client`, `webview_flutter`, `firebase_core/analytics/crashlytics/remote_config`, `image_picker`, `package_info_plus`, `intl` — FACT (`pubspec.yaml`).
- **DI:** No DI framework; manual provider wiring — INFERENCE.

## C. Architecture

- **Layers (INFERENCE from tree + sources):** native Kotlin `MainActivity` → `FLauncherChannel` (Dart channel wrapper) → provider/service layer (`lib/providers/*`) backed by `drift` DB (`lib/database.dart`) → UI (`lib/flauncher.dart`, `lib/widgets/*`, `lib/widgets/settings/*`).
- **Platform abstraction:** Single channel boundary isolates all Android/TV specifics from Dart — FACT (all native ops in MainActivity exposed via one method channel + one event channel).
- **UI architecture:** `Stack` home with `FocusTraversalGroup` using custom `RowByRowTraversalPolicy`; `Consumer<T>` widgets bind to ChangeNotifier services — FACT (`lib/flauncher.dart`, `lib/custom_traversal_policy.dart`).
- **Modules:** single Flutter module/app (no multi-module split) — FACT (tree).

## D. TV-specific

- **HOME intent / launcher registration:** `MainActivity` intent-filter: `MAIN` + `DEFAULT`, `HOME`, `LEANBACK_LAUNCHER`, `LAUNCHER`; requires `android.software.leanback` (required=true), touchscreen optional, `android:banner` set, landscape, singleTask — FACT (AndroidManifest.xml).
- **Permissions/queries:** `REQUEST_DELETE_PACKAGES`; scoped `<queries>` (MAIN, GET_CONTENT) instead of `QUERY_ALL_PACKAGES` — FACT.
- **App discovery:** `getApplications` queries `ACTION_MAIN` + `CATEGORY_LEANBACK_LAUNCHER` (TV) then `CATEGORY_LAUNCHER` (sideloaded, de-duplicated); icons/banners rendered to PNG bytes — FACT.
- **Live updates:** `LauncherApps.Callback` streams PACKAGE_ADDED/CHANGED/REMOVED/PACKAGES_AVAILABLE over the event channel — FACT.
- **Launching:** prefers `getLeanbackLaunchIntentForPackage`, falls back to `getLaunchIntentForPackage` — FACT.
- **System actions:** `openSettings` (`ACTION_SETTINGS`), `openAppInfo` (`ACTION_APPLICATION_DETAILS_SETTINGS`), `uninstallApp` (`ACTION_DELETE`), `isDefaultLauncher` (resolve `CATEGORY_HOME`), `startAmbientMode` (`com.android.systemui/.Somnambulator`) — FACT.
- **D-pad/remote:** `RowByRowTraversalPolicy` guarantees adjacent-row up/down; `lib/actions.dart` for key actions — FACT.
- **Default-launcher caveat:** Not auto-settable on Chromecast with Google TV; README documents Button Mapper (Home remap) or ADB disabling `com.google.android.apps.tv.launcherx` (with a YouTube-button breakage warning) — FACT.

## E. UX

- **Layout:** background (wallpaper image or gradient) + transparent Scaffold; AppBar with `TimeWidget` clock and settings button; categories in `SingleChildScrollView` — FACT (`lib/flauncher.dart`).
- **Categories:** row or grid per `CategoryType`; per-category columns count, row height, sort — FACT (`apps_service.dart`).
- **Navigation/focus:** row-by-row D-pad traversal; visible focus highlight; optional highlight animation (toggle added 2023-03-03); optional navigation sound feedback — FACT.
- **Customization:** wallpaper (local file via GET_CONTENT, Unsplash, gradients); create/rename/delete/reorder categories; add/remove/hide apps; settings panels (applications, categories, category, wallpaper, unsplash, gradient, about) — FACT.
- **App management:** app info + uninstall from UI — FACT.
- **Accessibility:** no documented screen-reader/accessibility features — ASSUMPTION/UNKNOWN.
- **Missing on home:** weather, recent apps — FACT (absent from `flauncher.dart`).

## F. Quality

- **CI:** GitLab pipeline (precheck→analyze→test→build→transform→upload→deploy→promote): commitlint, dart format check, `flutter analyze`, `flutter test --coverage`, debug/release AAB, APK via bundletool, GitLab release + Play Store deploy/promote via fastlane; image `cirrusci/flutter:3.7.5` — FACT (`.gitlab-ci.yml`).
- **Tests:** ~16 `_test.dart` files — providers, database/migrations, channel, home + settings widgets; mockito, network_image_mock — FACT (`test/` tree).
- **Release process:** semantic git tags (latest 0.18.0, 2023-04-21); 17 releases, 42 tags — FACT.
- **Maturity/maintenance:** self-described early stage; latest commit 2023-03-03 — effectively dormant since 2023 as of 2026-10-20 — INFERENCE. Active development has moved to forks — INFERENCE.
- **Benchmarks:** none published — FACT (not in repo).

## G. Limitations

- GPL-3.0 copyleft prevents code reuse in an MIT/Apache NexaTV codebase — INFERENCE.
- Early-stage; mainly tested on Chromecast with Google TV — FACT (README).
- No recent apps, Continue Watching, weather, notifications, media/now-playing, Jellyfin/Plex/Kodi, NAS, or Home Assistant integrations; no HDMI/input-source switching — FACT (absent from repo).
- No built-in OLED-friendly screensaver — only triggers the system ambient mode — FACT (`startAmbientMode`). (OLED screensaver exists in the LtvLauncher fork.)
- Bundles Firebase analytics/crashlytics/remote_config telemetry despite being ad-free — FACT (`pubspec.yaml`); Crashlytics collection disabled by default in manifest — FACT.
- Cannot cleanly self-register as default launcher on Chromecast with Google TV (needs ADB/Button Mapper) — FACT.
- Locked to landscape; no accessibility docs — FACT / UNKNOWN.
- Maintenance stalled since 2023; no performance benchmarks — INFERENCE.

### Takeaways for NexaTV
- FLauncher is an excellent **architectural reference** for a Flutter Android TV launcher (channel boundary, drift persistence, row-by-row D-pad traversal, leanback app discovery) — INFERENCE.
- **Do not copy its GPL-3.0 code** into an MIT/Apache project; reimplement independently — INFERENCE.
- NexaTV-desired features (recents, continue watching, weather, media/NAS/Home Assistant integrations, OLED screensaver, reboot reliability) are **not provided** by upstream FLauncher and would be net-new — FACT/INFERENCE.

### Sources
- https://gitlab.com/flauncher/flauncher (repo, README, LICENSE, pubspec.yaml, .gitlab-ci.yml, AndroidManifest.xml, MainActivity.kt, lib/*, test/*)
- GitLab API: repository tree, commits, releases for `flauncher/flauncher`
- Google Play: `me.efesser.flauncher`
