> **Source:** Phase 0 research, 2026-10-20 — produced by parallel research agents working from the
> actual repositories. Claims are labelled FACT / INFERENCE / ASSUMPTION / UNKNOWN with source refs.
> Pending human review. Do not reuse any code until the license is confirmed (see LICENSE_ANALYSIS.md).

# NexaTV Architecture Comparison

> Scope: synthesis from the provided research corpus only. Each claim is labeled **FACT** (stated in corpus as observed), **INFERENCE** (reasoned from corpus evidence), **ASSUMPTION** (plausible but unverified), or **UNKNOWN** (no evidence). Observation date for all projects: 2026-10-20.

---

## 1. Per-Project Architecture Summary

| Project | UI stack | State mgmt | Native / Android bridge | Persistence | Modularity | Strengths | Risks |
|---|---|---|---|---|---|---|---|
| **FLauncher** (gitlab.com/flauncher) | Flutter/Dart (Flutter 3.7.5), Flutter embedding v2 **(FACT)** | `provider` + `ChangeNotifier` services; no formal DI (manual wiring) **(FACT)** | Native Kotlin `MainActivity` over one method channel + one event channel, wrapped by `FLauncherChannel` **(FACT)** | `drift` (SQLite, WAL, versioned migrations v1–v5) + `shared_preferences` **(FACT)** | Single Flutter module, layered (channel / providers / UI); clean one-channel platform boundary **(INFERENCE)** | Clean platform abstraction; mature CI + ~16 test files; custom D-pad row traversal **(FACT)**; ad-free **(FACT)** | **GPL-3.0 blocks code reuse in MIT/Apache NexaTV (INFERENCE)**; dormant since 2023 **(FACT)**; bundles Firebase telemetry **(FACT)**; thin feature set **(FACT)** |
| **LTvLauncher / LTvLauncher Extended** (github.com/LeanBitLab, niposch fork) | Flutter/Dart (FLauncher-derived) **(FACT)** | `provider` / `ChangeNotifier` services (many) **(FACT)** | Single MethodChannel + EventChannels; native Kotlin/Java for PackageManager, TvInputManager, NotificationListener, DreamService, accessibility, usage stats **(FACT)** | `drift` (SQLite) + `shared_preferences` **(FACT)** | Single Flutter module, richly split (`models/providers/widgets/l10n`) **(FACT)** | Most feature-complete of the Flutter set (Continue Watching, weather, TV inputs, OLED screensaver, notifications); base very active (~797 commits, ~392 stars) **(FACT)** | **GPL-3.0 copyleft blocks permissive reuse (FACT/INFERENCE)**; CI lacks test/analyze gate **(FACT)**; Extended is single-maintainer, low adoption **(FACT)**; fixed row layout **(INFERENCE)** |
| **Yukuza Launcher** (github.com/urunkarpm) | Kotlin 2.0 + Jetpack Compose for TV; single-activity Compose Navigation **(FACT)** | `StateFlow<ImmutableList>` + `derivedStateOf`, `@Immutable/@Stable`; Hilt DI **(FACT)** | Repositories wrap `LauncherApps`, `PackageManager`, `MediaSessionManager`, `NotificationListenerService`; WorkManager sync **(FACT)** | Room + DataStore **(FACT)** | Clean/layered (`data/di/domain/navigation/ui`) with use cases **(FACT)** | Modern Compose-for-TV clean architecture; Baseline Profiles + macrobenchmark; CI runs build/test/lint; **MIT (permissive) (FACT)** | **No LICENSE file despite README MIT claim (FACT)** → reuse grant ambiguous **(ASSUMPTION/RISK)**; early-stage, ~3.5 mo stale, 10 stars **(INFERENCE)**; no categories/Continue Watching **(FACT)**; CI skips instrumented/benchmark **(FACT)** |
| **Tarang Launcher** (github.com/mohitkyadav) | Kotlin 2.0.21 + Jetpack Compose for TV (tv-material 1.0.0); 100% Compose UI **(FACT)** | MVVM + `StateFlow`; manual DI (`AppContainer`, Hilt as upgrade path) **(FACT)** | Thin data-class wrappers over PackageManager/ContentResolver/MediaSessionManager; accessibility `HomeRedirectService`; notification listener (media only) **(FACT)** | Jetpack DataStore (Preferences) **(FACT)** | Single module (:app) + :baselineprofile; layered packages; ~7,800 LOC **(FACT)** | Baseline Profiles; AGSL effects gated to A13+; targets weak TV GPUs; minSdk 28 **(FACT)**; active, 24+ releases **(FACT)** | **No LICENSE = all-rights-reserved, NOT reusable (FACT)**; no tests, no CI **(FACT)**; verified on ~1 device (Chromecast GTV) **(FACT)**; bus factor 1 **(INFERENCE)**; raw `HttpURLConnection` networking **(FACT)** |
| **nielsvanvelzen/tv-launcher** (primary) | Kotlin + Compose-for-TV (tv-material 1.1.0); navigation3 **(FACT)** | MVVM, per-tab ViewModels **(FACT)** | `Resolver` classes over PackageManager / TvInputManager / TvProvider; `PackageChangeReceiver` **(FACT)** | SQLDelight 2.4 **(FACT)** | Single module (:app), layered data/ui; Koin DI **(FACT)** | Clean resolver-based platform abstraction; HDMI/TV input switching; channels/Watch-Next; active (496 stars) **(FACT)** | **GPL-3.0 blocks reuse in MIT/Apache NexaTV (FACT/INFERENCE)**; no tests, no release binary **(FACT)**; early/experimental; no categories/wallpaper **(FACT)** |
| **karuhun-developer/LauncherCompose** (secondary) | Kotlin + Compose (Now-in-Android template) **(FACT)** | MVVM; Dagger Hilt DI **(FACT)** | `ApplicationLauncherImpl`, `QUERY_ALL_PACKAGES` **(FACT)** | Room + Proto DataStore **(FACT)** | Full NiA modularization (`core/*`, `feature/*`, build-logic) **(FACT)** | **Apache-2.0 → compatible with MIT/Apache NexaTV, code reusable with attribution (FACT)**; exemplary modular structure; baseline profiles, Spotless, lint module **(FACT)** | Hotel/IPTV guest-facing scope, backend + Firebase dependent → limited as general launcher **(FACT)**; tests mostly stubs, no CI/releases **(FACT)** |

---

## 2. Strategic Options vs. Evidence

### Option A — Fork FLauncher / LTvLauncher (incl. Extended)
- **License blocker (FACT/INFERENCE):** Both are **GPL-3.0**. Forking forces the entire NexaTV derivative to be GPL-3.0 with full corresponding-source disclosure on distribution. If NexaTV must ship under MIT/Apache or remain proprietary, this option is **incompatible**.
- **Upside (FACT):** LTvLauncher is the most feature-complete codebase in the corpus (Continue Watching via Watch Next, weather, TV inputs, OLED screensaver, notifications, backup/restore) and its base is actively maintained (~797 commits). FLauncher offers the cleanest single-channel architecture and best test/CI maturity among the Flutter set.
- **Downside (FACT):** FLauncher is dormant (no commits since 2023) and bundles Firebase telemetry; LTvLauncher's CI does not gate on tests/analyze, and Extended is single-maintainer with minimal traction. Both are Flutter — see device-constraint analysis in §3.
- **Verdict (INFERENCE):** Viable **only if NexaTV adopts GPL-3.0**. Otherwise usable as architecture/UX reference (clean-room) but not for code reuse.

### Option B — Fork another (Yukuza, Tarang, tv-launcher, LauncherCompose)
- **tv-launcher (FACT/INFERENCE):** GPL-3.0 — same copyleft blocker as Option A. Good Compose-for-TV reference (resolver pattern, HDMI inputs), but no tests and no release binary.
- **Yukuza (FACT/ASSUMPTION):** README claims MIT but **no LICENSE file exists and GitHub's license field is null**, so the grant is legally ambiguous. Modern Compose-for-TV clean architecture with Baseline Profiles makes it the most attractive *permissive* Kotlin candidate — **contingent on obtaining a real LICENSE file or written confirmation**. Early-stage (10 stars, ~3.5 mo stale), no categories, no Continue Watching.
- **Tarang (FACT):** **No license = all rights reserved** — cannot be lawfully forked into a derivative. Reference only. Also verified on essentially one device.
- **LauncherCompose (FACT):** **Apache-2.0 → fully compatible** and reusable with attribution, and its NiA modularization is the strongest in the corpus. But its scope is hotel/IPTV, backend- and Firebase-dependent, not a general consumer launcher — so reuse value is largely structural/patterns rather than feature code.
- **Verdict (INFERENCE):** The only cleanly license-compatible fork bases are **LauncherCompose (Apache-2.0)** — but wrong domain — and **Yukuza (MIT, if the license is cured)**. Both are immature for a product launcher.

### Option C — Greenfield Flutter
- **Evidence for (INFERENCE):** FLauncher/LTvLauncher prove Flutter + a single method/event channel is a clean, workable pattern for an Android TV launcher, with mature testing shown in FLauncher's CI.
- **Evidence against (FACT/INFERENCE):** No performance or resource benchmarks are published for *any* Flutter project in the corpus ("Fast startup" / "Low resource usage" are **Unknown** or **Partial** for both Flutter projects). The device constraints (§3) are a material risk for a Flutter engine on 32-bit ARM with ~2.3 GiB RAM.
- **Verdict (INFERENCE):** Feasible and gives full license freedom, but carries the largest build cost and unmeasured runtime risk on the target hardware.

### Option D — Hybrid (clean-room reference + greenfield, reusing only Apache/MIT assets)
- **Evidence (FACT):** LauncherCompose (Apache-2.0) can contribute *actual code/modularization patterns* with attribution; all GPL projects (FLauncher, LTvLauncher, tv-launcher) can inform **architecture and UX by clean-room study only**; Yukuza/Tarang offer modern Compose-for-TV patterns to emulate.
- **Verdict (INFERENCE):** Lowest legal risk while still leveraging the corpus. Build NexaTV greenfield (Kotlin/Compose or Flutter), import permissive code where it fits, and treat GPL projects strictly as design references.

---

## 3. Flutter vs. Kotlin/Compose — against the TCL device constraint

> **Target device (given):** 32-bit ARM only — **armeabi-v7a**; 4 cores; **~2.3 GiB RAM**; UI at **1080p**.

| Dimension | Flutter/Dart (FLauncher, LTvLauncher) | Kotlin + Compose-for-TV (Yukuza, Tarang, tv-launcher, LauncherCompose) |
|---|---|---|
| **32-bit armeabi-v7a support** | Flutter can build armeabi-v7a, but ships its own engine (.so) per-ABI, enlarging the APK and memory footprint. **(INFERENCE)** No corpus project documents 32-bit-only testing. **(UNKNOWN)** | Native Android target; armeabi-v7a is a standard ABI. Tarang explicitly notes the Chromecast GTV reference device is **armeabi-v7a (FACT)**. Lower runtime overhead vs. a bundled engine. **(INFERENCE)** |
| **RAM headroom (~2.3 GiB)** | Dart VM + Flutter engine add baseline memory on top of the app; no benchmarks in corpus. **(UNKNOWN)** | Smaller baseline (no second runtime); Yukuza/Tarang use immutable collections + Baseline Profiles to cut recomposition/allocations. **(FACT, as techniques; resource numbers UNKNOWN)** |
| **CPU (4 cores)** | Dart is single-isolate by default; UI work on one thread unless isolates used. **(INFERENCE)** | Coroutines/Flow spread work across cores naturally. **(INFERENCE)** |
| **1080p GPU rendering** | Flutter renders via Skia/Impeller; capable but unbenchmarked here. **(UNKNOWN)** | Tarang gates AGSL "liquid glass" to A13+ and defaults blur off to protect weak TV GPUs **(FACT)** — direct evidence of tuning for low-end TV GPUs. |
| **Startup performance** | No baseline profiles or startup numbers for either Flutter project. **(UNKNOWN)** | Yukuza & Tarang ship **Baseline Profiles + macrobenchmark StartupBenchmark (FACT)**; tv-launcher has none **(UNKNOWN for it)**. No *published numbers* for any. |
| **License freedom at the toolchain level** | Both Flutter candidates are **GPL-3.0 (FACT)**. | Mixed: Yukuza MIT (ambiguous), Tarang none, tv-launcher GPL-3.0, LauncherCompose Apache-2.0. **(FACT)** |
| **Maturity of TV focus/D-pad** | Custom `RowByRowTraversalPolicy` proven in production **(FACT)**. | Compose-for-TV (tv-material) provides focus/D-pad primitives; used across all four **(FACT)**. |

**Net (INFERENCE):** For a constrained 32-bit, ~2.3 GiB TCL target, **Kotlin + Compose-for-TV has the stronger evidence base for low-end tuning** (explicit armeabi-v7a reference device, GPU gating, Baseline Profiles). Flutter is architecturally proven for TV launchers but has **zero resource/startup evidence on this class of hardware (UNKNOWN)**, which is the dominant open risk.

---

## 4. Preliminary (Non-Binding) Recommendation

The evidence supports a **directional** lean but **not** a final, binding decision.

**Directional lean (INFERENCE):** Pursue a **Hybrid / greenfield-in-Kotlin-Compose-for-TV path (combines Options C and D)**:
- **Toolchain:** Kotlin + Compose-for-TV, because it has the only direct evidence of tuning for the exact constraint class (Tarang's armeabi-v7a device + GPU gating; Yukuza/Tarang Baseline Profiles), whereas no Flutter project in the corpus has any resource/startup evidence on this hardware. **(INFERENCE)**
- **License strategy:** Treat all **GPL-3.0** projects (FLauncher, LTvLauncher/Extended, tv-launcher) as **clean-room architecture/UX references only** — do **not** reuse code unless NexaTV itself ships GPL-3.0 **(FACT/INFERENCE)**. Reuse structural patterns/code only from **Apache-2.0 LauncherCompose** (with attribution), and from **Yukuza only if its MIT license is cured** **(FACT/ASSUMPTION)**.
- **Architecture to emulate (INFERENCE):** The resolver/repository-over-platform pattern (tv-launcher, Yukuza) + NiA-style modularization (LauncherCompose) + single method/event-channel discipline (FLauncher) — whichever maps to the chosen toolchain.
- **Feature baseline to target (FACT):** LTvLauncher demonstrates the richest viable feature set (Continue Watching via Watch Next, weather, TV inputs, OLED screensaver, notifications) and is the best functional reference even though its code cannot be reused.

### What is still needed before this can become binding
- **Hardware benchmarks (UNKNOWN):** No project publishes startup time, RAM, or frame-timing numbers on 32-bit armeabi-v7a / ~2.3 GiB. Require a prototype spike measuring both a Flutter and a Compose-for-TV shell on the actual TCL unit before committing.
- **License resolution for Yukuza (ASSUMPTION/RISK):** Confirm an actual MIT LICENSE file or obtain written grant; otherwise drop it as a reuse candidate.
- **NexaTV's own license posture (UNKNOWN in corpus):** Whether NexaTV may ship GPL-3.0 determines if Options A/B-via-tv-launcher reopen. If GPL-3.0 is acceptable, **forking LTvLauncher (Flutter) becomes the fastest feature-complete path** — this materially changes the recommendation.
- **Target feature scope and default-launcher story (FACT gaps):** Several projects cannot replace the system launcher on Google/Fire TV without accessibility/RoleManager/ADB workarounds; NexaTV must confirm which mechanism the TCL firmware allows.
- **Reboot reliability (Partial/Unknown across all):** No project has tested reboot reliability on the target; needs validation.

---

### Relevant source pointers (from corpus)
- FLauncher: `https://gitlab.com/flauncher/flauncher` — GPL-3.0, Flutter, dormant.
- LTvLauncher / Extended: `https://github.com/LeanBitLab/LtvLauncher`, `https://github.com/niposch/LTvLauncher-Extended` — GPL-3.0, Flutter, most feature-complete.
- Yukuza: `https://github.com/urunkarpm/yukuza-launcher` — MIT (no LICENSE file), Kotlin/Compose-for-TV.
- Tarang: `https://github.com/mohitkyadav/tarang-launcher` — no license, Kotlin/Compose-for-TV, armeabi-v7a reference device.
- tv-launcher: `https://github.com/nielsvanvelzen/tv-launcher` — GPL-3.0, Kotlin/Compose-for-TV.
- LauncherCompose: `https://github.com/karuhun-developer/LauncherCompose` — **Apache-2.0 (reusable)**, Kotlin/Compose, hotel/IPTV scope.
