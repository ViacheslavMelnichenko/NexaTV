# Design — NexaTV release pipeline (sideload)

**Date:** 2026-10-21
**Status:** Approved (brainstorm) — ready for an implementation plan
**Implements:** [ADR-0003](../../adr/ADR-0003-cicd-signing-release.md) (CI/CD, signing, release & update/rollback).
Builds nothing new in the architecture — it **implements the already-accepted ADR-0003 pipeline**, which
was designed but not yet built (`release.yml`, `signingConfig`, the signing/release docs are all missing).

## Goal

Have a real, signed **release version** of NexaTV and a documented way to deliver updates to the TV.
Distribution is **sideload via `adb`** (owner decision, 2026-10-21) — explicitly **not** Google Play:

- **FACT.** The HOME mechanism is an accessibility override (ADR-0007). Google Play disallows using
  `AccessibilityService` for non-accessibility purposes, so a launcher that captures HOME via accessibility
  would very likely be rejected/removed. Play also adds a developer account, review, a yearly target-API
  treadmill, and a privacy-policy obligation — all cost, no benefit, for a single owner-operated TV.
- Sideload needs zero infrastructure and no extra/sensitive permission, matching the lean/private posture.

An in-app OTA self-updater remains **deferred** (ADR-0003 D7 Follow-ups), to be reconsidered post-MVP only
if manual sideload becomes a real pain point.

## Non-goals

- No Google Play / Play internal track; no in-app or OTA self-updater (deferred).
- No `REQUEST_INSTALL_PACKAGES` permission.
- No bit-for-bit reproducible builds (ADR-0003 D8 non-goal); pinned, CI-built-from-tag is the target.
- No change to the HOME mechanism (ADR-0007) or the app's permission posture.

## Components

Five units, each independently reviewable.

### A. Gradle: signing config + version-from-tag (`app/build.gradle.kts`)

- **Signing.** Add a `release` `signingConfig` that reads the keystore from **environment variables**:
  `NEXATV_KEYSTORE_FILE`, `NEXATV_KEYSTORE_PASSWORD`, `NEXATV_KEY_ALIAS`, `NEXATV_KEY_PASSWORD`.
  The config is applied to the `release` build type **only when `NEXATV_KEYSTORE_FILE` is set and the file
  exists**; otherwise `assembleRelease` produces an **unsigned** APK (local/dev convenience). No key, path,
  or password is ever committed (Hard Rule 4). APK Signature Scheme v2+v3 on, v1 off (ADR-0003 D3).
- **Version.** `versionName`/`versionCode` are taken from Gradle properties `-PversionName=` / `-PversionCode=`
  when provided; otherwise they fall back to the current in-file defaults (`0.1.0` / `100`) so local and CI
  non-release builds keep working. The release workflow derives and passes the real values from the git tag
  (ADR-0003 D5). The in-file default is bumped to match the latest release so dev builds aren't "behind".

### B. Release workflow (`.github/workflows/release.yml`)

Trigger: push of an annotated tag matching `v*.*.*`. Runs in a protected GitHub **Environment `release`**
so signing secrets are exposed only here; never runs on fork PRs (ADR-0003 D2/D4).

Steps:
1. Checkout, JDK 17 (temurin), Gradle cache.
2. **Derive version from the tag:** `versionName` = tag without the leading `v`; `versionCode` =
   `MAJOR*10000 + MINOR*100 + PATCH` (ADR-0003 D5).
3. **Monotonic guard:** query the latest prior GitHub Release; **fail** if the new `versionCode` ≤ the
   previous one.
4. **Signing fail-fast:** if the signing secrets are absent, **fail the job** — a "release" is never
   published unsigned (ADR-0003 D4).
5. Base64-decode the keystore secret to a temp file; export the signing env vars from A.
6. `assembleRelease` (R8/minify + resource shrink) → **signed** release APK, renamed `nexatv-<version>.apk`.
7. **Asserts on the release APK:** ABI (no 64-bit `.so`; only `lib/armeabi-v7a/` if any native lib),
   and a **size budget** ≤ **20 MB** (adjustable; tighter than the 30 MB debug budget).
8. `apksigner verify --print-certs` → record the APK **SHA-256** and the signing-cert **fingerprint**.
9. Extract the `CHANGELOG.md` section for this version as the release notes.
10. Create the **GitHub Release**: the APK asset plus SHA-256, cert fingerprint, exact git SHA, and the
    changelog notes. The published APK **is** this CI artifact — never a local rebuild (ADR-0003 D6).

### C. Owner-run signing procedure (`docs/technical/RELEASE-SIGNING.md`)

Documentation only — **no secret values in Git**. Covers:
- The exact **offline** `keytool` command to generate the keystore: one key, RSA 4096, certificate validity
  ≥ 30 years, one alias.
- Base64-encoding the keystore and adding the GitHub **encrypted secrets** scoped to the `release`
  Environment: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- **Two offline backups** (password manager **and** an encrypted offline medium) — the "lost key = fatal,
  app can never be updated in place again" warning (ADR-0003 D3/D4). Rotation/backup procedure.
- One-time GitHub setup: branch protection on `main` (PR-only, ≥1 required check, linear history) and the
  protected `release` Environment.

### D. Release checklist (`docs/testing/RELEASE-CHECKLIST.md`)

The manual pre-release gate the cloud can't run (ADR-0003 D7/D9), as an ordered checklist:
- **Before tagging:** move `CHANGELOG.md` `[Unreleased]` → `[x.y.z] - DATE`; CI green on `main`.
- **After CI builds the signed release:** `adb install -r nexatv-x.y.z.apk` on the real TCL; smoke test —
  launch, set-as-HOME (accessibility re-point), reboot survival, cold-start sanity; verify icons/weather
  render on the **minified** build (R8 can differ from debug — see Risks).
- **Performance:** record cold-start/PSS if available. The Macrobenchmark device-install gap is still open
  (the connected task installs only the test APK, not the measured app), so perf numbers are **recorded,
  not a hard block, for v0.1.0** — honestly noted rather than pretended-gated.
- **Rollback:** the previous release APK stays on GitHub; `adb install -r` the previous one; FallbackHome /
  stock-launcher recovery path documented.

### E. TV update story (documented in D and `docs/technical/ADB.md`)

- Normal update: `adb install -r nexatv-x.y.z.apk` — same certificate → **in-place**, user data preserved.
- **One-time migration wrinkle:** the release `applicationId` is `com.nexatv.launcher` (no `.debug`), while
  the build currently acting as HOME on the TV is the **debug** package (`com.nexatv.launcher.debug`). The
  first release is therefore a **separate install**, not an in-place update of the debug app. The migration:
  install the release APK, re-point the accessibility-HOME override to the release package, optionally remove
  the debug build. Documented explicitly so the first switchover isn't surprising.

## Data flow

```
git tag vX.Y.Z  ──push──▶  release.yml (Environment: release)
   │                           │ derive version, guard monotonic, fail-fast if no secrets
   │                           │ decode keystore ▶ assembleRelease (R8, signed)
   │                           │ assert ABI + size ▶ apksigner verify (SHA-256 + cert fp)
   │                           │ extract CHANGELOG section
   ▼                           ▼
GitHub Release  ◀── APK + SHA-256 + cert fingerprint + git SHA + notes
   │
   ▼  (owner, manual, per RELEASE-CHECKLIST)
adb install -r nexatv-X.Y.Z.apk  ──▶  TCL  (in-place; first time = migrate off .debug)
```

## Risks & honest consequences

- **R8/minify divergence.** The release build enables R8 + resource shrink; debug does not. A prior fix had
  to keep protobuf-lite classes so R8 wouldn't corrupt DataStore. The first signed release can behave
  differently from the debug build the owner has run so far → the on-device smoke test in D is **mandatory**,
  not optional, and proguard-keep rules must be verified against the release build.
- **Separate package from the debug build on the TV** → a one-time migration (E), not a silent update.
- **Lost signing key = fatal** (no Play safety net) → two offline backups are non-negotiable (C).
- **Perf not gated in CI** (no 32-bit device) → relies on the disciplined manual step; the benchmark
  device-install gap means v0.1.0 ships with recorded-not-blocked perf numbers, stated plainly.
- **Unsigned until secrets exist** → `release.yml` fails fast rather than publishing unsigned; the owner adds
  the keystore secrets when ready, then the first `v0.1.0` tag signs and publishes.

## Testing

- `release.yml` is validated by cutting a **pre-release tag** (e.g. `v0.0.1-rc`) once secrets exist, checking
  the asserts/guards fire and the GitHub Release is well-formed, before the real `v0.1.0`.
- The version-derivation and monotonic-guard logic are small shell steps; kept simple enough to read, with
  the guard proven by a deliberately-stale `versionCode` trial on the rc tag.
- Gradle signing is exercised locally with a throwaway keystore via the env vars (not committed) to confirm
  `assembleRelease` signs when present and builds unsigned when absent.

## Out of scope / deferred

- In-app / OTA self-updater and `REQUEST_INSTALL_PACKAGES` (ADR-0003 Follow-ups) — post-MVP, only if needed.
- Any Google Play track. Closing the benchmark device-install gap (separate workstream).
