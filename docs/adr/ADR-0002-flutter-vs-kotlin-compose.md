# ADR-0002: Flutter vs. Kotlin/Compose-for-TV

## Status

**Accepted** — 2026-10-20. (Decided by the project owner; technology selection deferred from ADR-0001 §Follow-ups.)

## Context

ADR-0001 (Accepted) fixed two things that frame this decision:

- **FACT:** NexaTV is a **greenfield** build under a **permissive license (MIT/Apache)**. GPL-3.0
  launchers (FLauncher, LTvLauncher, `tv-launcher`) are **clean-room references only** — no code is
  copied. So "reuse FLauncher's Flutter code" is **not** a benefit available to either option here.
- **FACT:** the UI technology choice was explicitly deferred to this ADR and is orthogonal to build-vs-fork.

The decision must serve NexaTV's stated priorities (`initial.md` §§20–21, §34), ranked for this product:
**(1)** D-pad/remote-first UX with *always-obvious, predictable focus*; **(2)** low memory; **(3)** fast
startup; **(4)** reliable after reboot; **(5)** offline-first core; **(6)** low/no telemetry; **(7)**
maintainable, intentional architecture.

### The audited device (the thing every claim is weighed against)

From the read-only device audit (`docs/technical/ANDROID-TV.md`, all FACT via ADB 2026-10-20):

| Property | Value | Why it matters here |
|---|---|---|
| CPU ABI | **armeabi-v7a only** (abilist `armeabi-v7a, armeabi`; **no arm64-v8a**) | Any shipped native `.so` **must** be 32-bit; an arm64-only artifact will not install |
| Cores / RAM | **4 cores / ~2.34 GiB total** | A launcher is **always-resident**; resident footprint drives kill-probability under memory pressure |
| OS / SDK | Android TV OS 14 / **SDK 34** | Current enough for `RoleManager ROLE_HOME`, `LauncherApps`, Compose-for-TV stable libs |
| UI render | **1080p** (4K panel, UI override) | Modest pixel budget; not a GPU stress case, but the old 32-bit GPU is an UNKNOWN for any renderer |
| Device class | Leanback TV (54 `LEANBACK_LAUNCHER` apps) | D-pad is the *only* realistic input; focus correctness is existential, not cosmetic |
| Recovery | `com.android.tv.settings/.system.FallbackHome` exists | A misbehaving HOME is recoverable — but reinforces that the resident launcher must be dependable |

### The invariant both options share (this is the crux)

**FACT:** every capability on the must-build-native list is an **Android** API that is written in
**Kotlin/Java regardless of the UI framework**: app discovery (`LEANBACK_LAUNCHER` + `LAUNCHER` via
`PackageManager`/`LauncherApps`), launching, live `ACTION_PACKAGE_ADDED/REMOVED` updates, HOME /
default-launcher registration (`RoleManager ROLE_HOME`), and optional `TvInputManager`/channels. Neither
option avoids learning the Android platform. The question is therefore **not** "Dart vs. Kotlin for the
hard part" — the hard part is Kotlin either way. The question is: **does wrapping that native core in a
Flutter presentation layer (adding a second language, a second toolchain, and a serialization bridge) buy
more than it costs on *this* device, for *these* priorities?**

### Developer profile (FACT)

Expert .NET/C# engineer; strong architecture / Git / CI. **First** serious Android/ADB project. Flutter is
already installed; the Kotlin/Android toolchain is not yet set up. Kotlin maps cleanly from C#
(coroutines ≈ `async/await`, `Flow` ≈ Rx/`IObservable`, Compose's declarative model ≈ Blazor/MAUI);
Dart maps cleanly too. The transfer advantage is **roughly symmetric**, so "already installed / gentler
on-ramp" is a weak tiebreaker, not a decisive one.

## Decision

**Build NexaTV as a single-stack Kotlin + Jetpack Compose-for-TV application** (`androidx.tv:tv-material`,
`compose-foundation`), calling Android TV APIs directly — **no Flutter, no platform-channel bridge**.

This is a **firm decision, not a Conditional one**: the architectural reasons below do not depend on the
unmeasured device benchmarks. The unknowns (exact RSS, GPU behavior) affect *absolute* numbers, not the
*relative* ranking — Flutter's bundled-engine overhead is structurally higher than Compose's DEX-on-ART
footprint whatever the exact figures turn out to be. The decision is nonetheless gated by a cheap
**on-device validation spike** (see Follow-ups) that *confirms* assumptions rather than *chooses between
stacks*; it would only reopen the decision in the unlikely event Compose-for-TV focus proves fundamentally
broken on this SDK-34 unit.

## Alternatives

Weighed against license · the audited 32-bit device · the ranked priorities. The full head-to-head:

| Dimension (device-weighted) | Flutter (Dart UI + Kotlin channels) | **Kotlin + Compose-for-TV** | Edge |
|---|---|---|---|
| **32-bit ABI fit** | Ships `armeabi-v7a` as a default release ABI in 3.35+ (**CONFIRMED** — the ABI change dropped x86, *not* 32-bit ARM). Bundles a native engine `.so`; must pin + audit every plugin for 32-bit artifacts | Pure Kotlin/DEX-on-ART ships **no native `.so`** → ABI-agnostic; Aug-2026 64-bit Play rule is additive and only bites apps bundling `.so` (**INFERENCE**, follows from ART/DEX) | **Compose** (neither is a dealbreaker, but Compose removes the whole 32-bit question) |
| **Resident memory (~2.34 GiB, always-on)** | Fixed engine overhead (Dart VM + GC heap + renderer); Flutter apps consistently use **materially more RAM** than native — direction well-supported; exact multiplier **UNKNOWN** on this unit | Modest baseline premium over the View system (~10 MB-class, **INFERENCE** from phone benchmarks); no second runtime | **Compose** (this is priority #2 and Flutter's *own* stated central risk) |
| **Cold start / first frame** | ~150–500 ms engine/VM init **before** app logic (**CONFIRMED directionally**; larger on weak HW) | Baseline Profiles (bundled in Compose + tv libs, Android 7+) AOT the critical path from first launch | **Compose** (priority #3) |
| **Reliable after reboot** | Heavier resident footprint → higher kill-probability → cold start on HOME press | Leaner footprint → more likely to stay warm | **Compose** (priority #4) |
| **D-pad / predictable focus (priority #1)** | Not an officially supported Flutter target; focus rests on the community `dpad` package (v3.x, active) + custom `RawKeyboard` code (**CONFIRMED** community-driven) | `androidx.tv:tv-material` **stable 1.1.0 (May 2026)**, Google's documented path (leanback deprecated); first-class `focusable`/`FocusRequester`/`FocusGroup`/`onPreviewKeyEvent` + leanback-correct defaults | **Compose** (the single most important priority sits on officially-supported primitives) |
| **Native TV API access** | Every call crosses a `MethodChannel`/`EventChannel` contract to design, version, test, debug | Direct Kotlin calls, one debugger, no serialization boundary | **Compose** (maintainability / intentional architecture) |
| **UI iteration speed** | **Hot reload** + single declarative tree + total pixel control — genuinely best-in-class | Compose previews + recomposition; good, not hot-reload-fast | **Flutter** (the real Flutter advantage — but serves *velocity*, a secondary concern) |
| **Toolchain surface (solo, first Android project)** | **Two** languages + **two** build systems (Flutter *and* Gradle/AGP) + a channel bridge | **One** language, one toolchain, native Studio profiling/Macrobenchmark | **Compose** |
| **License-safe references (ADR-0001)** | FLauncher (GPL, **dormant since 2023**) = behavioral spec only | `tv-launcher` + Yukuza = modern Kotlin+Compose-TV architectures to study clean-room | **Compose** (references match the chosen architecture directly) |
| **Low/no telemetry** | Greenfield → achievable | Greenfield, zero default analytics deps | Tie (slight Compose) |
| **New-tech risk** | Adds Dart+Flutter *on top of* unavoidable Kotlin | New to Android **and** Kotlin **and** Compose at once (3 new things) | **Flutter** (its one honest advantage on learning *breadth*) |

**Verdict:** Compose-for-TV wins or ties every priority-weighted row except UI-iteration speed and
learning-breadth. Flutter's genuine strengths (hot reload, Dart on-ramp) serve **velocity**, which is not
among NexaTV's top priorities; Compose's strengths land squarely on priorities #1–#4.

## Consequences

**Positive**
- Smallest resident footprint and fastest cold start of the two → directly serves low-memory, fast-startup,
  and reliable-after-reboot on a 2.34 GiB always-on device.
- The #1 priority (predictable focus) is built on **officially supported, stable** TV primitives, not a
  community shim on an unsupported target.
- One language, one toolchain, one debugger, zero serialization bridge → minimal architectural surface,
  matching `initial.md` §34 ("intentional, understandable").
- Direct native access means the unavoidable Kotlin TV work is *the whole app*, not a layer behind a channel.
- License-safe reference designs (`tv-launcher`, Yukuza) match the chosen architecture one-to-one.

**Negative / costs (honest)**
- **Steepest learning curve breadth:** the developer meets Android + Kotlin + Compose simultaneously. This
  is a **schedule/quality risk, not a technical blocker** (ASSUMPTION: partially offset by strong C#/CI
  transfer). Flutter would have let the developer defer deep Kotlin to a thinner layer — but *only* the
  *UI*, since the hard native layer is Kotlin regardless.
- **Compose recomposition is a different mental model**; naive non-lazy layouts / unstable params can cause
  jank and memory growth — a subtle failure mode on weak hardware (INFERENCE).
- **TV libraries can lag mainline Compose (~6 months)** and production focus edge-cases (restoration,
  dialog focus trapping, last-item memory) are known to surface — must be engineered deliberately
  (INFERENCE from practitioner reports, not official).
- We forgo Flutter's hot-reload iteration speed; focus-visual polish will iterate slightly slower.

**Mitigations**
- Go **100% Compose, no interop View hierarchy** — the memory-heavy mixed-stack failure mode never arises.
- Adopt **Baseline Profiles + Macrobenchmark from day one** (bundled in the tv libs); measure cold-start and
  RSS on the **actual TCL armeabi-v7a unit**, never an emulator.
- Lazy layouts (`LazyColumn/Row`) + stable/immutable state; keep composables shallow.
- Build a small, explicit focus framework early (`FocusRequester`/`FocusGroup`/`onPreviewKeyEvent`) with a
  visible indicator and defined restoration rules; make hardware D-pad traversal a first-class acceptance test.
- Stand up the Kotlin/Gradle toolchain + a reproducible CI build **before** feature code (plays to the
  developer's CI strength); study `tv-launcher`/Yukuza clean-room for patterns.
- Pin stable versions (`tv-material` 1.1.0, `compose-foundation`); keep a thin abstraction over any volatile
  TV API.

## Rationale

The naive argument "Flutter because it's already installed" is explicitly rejected: it is a convenience,
not an engineering reason, and Kotlin's C#→Kotlin transfer (coroutines/Flow/declarative UI) largely
neutralizes the on-ramp gap. The naive counter "native because native" is also rejected — the Flutter
advocate is **correct** that 32-bit is **not** a dealbreaker (CONFIRMED: Flutter still ships `armeabi-v7a`),
so ABI alone does not decide this.

What decides it is the combination of **device** and **priorities**. NexaTV's top four priorities —
predictable focus, low memory, fast startup, reliable-after-reboot — all point the same way on a 2.34 GiB,
always-resident, D-pad-only, officially-Compose-supported device. The one architectural fact both advocates
concede is dispositive: **the hard native layer is Kotlin in both options**, so Flutter does not spare the
developer the difficult part — it adds a second language, a second build system, and a channel boundary
*on top of* it, while its headline benefit (hot-reload UI velocity) serves a concern that is not in
NexaTV's top tier. Choosing Compose-for-TV concentrates the unavoidable Android learning into a single,
coherent, officially-supported stack and builds the most-prioritized subsystem (focus) on first-party
primitives rather than a community package on an unsupported target.

The decision is firm because it rests on structure (toolchain surface, direct access, official focus
support, memory *direction*) that is robust to the open benchmarks — not on numbers we have not yet
measured. Those numbers are confirmation gates, below.

## Follow-ups

- **Validation spike (gate, ~½ day):** flash a trivial Compose-for-TV release APK to the audited TCL and
  confirm (a) it installs/runs on `armeabi-v7a`; (b) `tv-material` focus traverses correctly with the
  physical remote; (c) cold-start and idle RSS are acceptable via Macrobenchmark on-device. Record results;
  reopen this ADR only if focus is fundamentally broken (not expected).
- Set up Kotlin/Gradle/AGP toolchain + CI build before feature code.
- Resolve UNKNOWNs on-device: old 32-bit GPU renderer behavior; refresh rate (`dumpsys display`); TV input
  services.
- Pin library versions and record them; create a thin abstraction over volatile TV APIs.
- On acceptance, mark **Accepted** and proceed to the PRD-scoped MVP (FR-001..FR-009).
