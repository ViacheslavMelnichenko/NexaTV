# Release checklist — manual on-device gate

> The pre-release steps CI **cannot** run: the human + real-TCL half of
> [ADR-0003](../adr/ADR-0003-cicd-signing-release.md) (D7/D9). CI builds, signs, and verifies the APK; this
> checklist is how a release becomes *trusted*. Work top to bottom; stop and analyse on any surprise.
>
> One-time owner setup (keystore, GitHub secrets, branch protection) lives in
> [`../technical/RELEASE-SIGNING.md`](../technical/RELEASE-SIGNING.md) and must be done before the first tag.

## Pre-tag

- [ ] **CHANGELOG moved.** In `CHANGELOG.md`, rename `## [Unreleased]` work to `## [X.Y.Z] - YYYY-MM-DD`
      and open a fresh empty `## [Unreleased]` above it. The release workflow extracts the `[X.Y.Z]`
      section verbatim as the GitHub Release notes.
- [ ] **CI green on `main`.** The `CI` workflow passes on the commit being released.
- [ ] **versionCode will strictly increase.** The new `vX.Y.Z` yields `MAJOR*10000 + MINOR*100 + PATCH`
      greater than the last Release's — the workflow's monotonic guard enforces this, but confirm it so the
      job doesn't fail late.

## Tag & let CI build

- [ ] **Tag and push:**
      ```bash
      git tag -a vX.Y.Z -m "NexaTV X.Y.Z"
      git push origin vX.Y.Z
      ```
- [ ] **Watch `release.yml`.** It derives the version, guards monotonicity, fails fast if signing secrets
      are missing, builds the **signed** APK, asserts ABI + 20 MB size, verifies the signature, and publishes
      the GitHub Release with the APK, its SHA-256, the cert fingerprint, and the commit SHA.
- [ ] **Verify provenance before downloading to the TV** (see RELEASE-SIGNING §7): `apksigner verify
      --print-certs` and `sha256sum` must match the Release notes.

## On-device smoke test (real TCL)

The release build is **minified (R8) + resource-shrunk**; the debug build the owner runs day-to-day is not.
R8 can diverge (a prior fix had to keep protobuf-lite classes so DataStore wasn't corrupted). **This test is
mandatory, not optional.**

- [ ] **Install:** `adb install -r nexatv-X.Y.Z.apk` (first release: see migration note below).
- [ ] **Launch** — the Home grid renders; no crash, no blank/Loading-stuck screen.
- [ ] **Set as HOME** — the accessibility override ([ADR-0007](../adr/ADR-0007-home-override-accessibility.md))
      raises NexaTV when the stock launcher appears.
- [ ] **Reboot survives** — after `adb reboot`, NexaTV still comes up as HOME.
- [ ] **Core UI on the minified build** — app tiles (icons + labels), favorites row, and the weather widget
      (condition glyph, temp, wind) all render correctly on R8 output, not just in debug.
- [ ] **Cold-start sanity** — subjectively snappy; no obvious regression.

## Performance (recorded, not gated — for now)

- [ ] **Record** cold-start / PSS if you can capture them, and note them in the Release or CHANGELOG.
- The Macrobenchmark device-install gap is still open (the connected task installs only the test APK, not the
  measured app), so for **v0.1.0 performance is recorded, not a hard block** — stated plainly rather than
  pretended-gated. Tighten to a gate once the benchmark install gap is closed.

## First-release migration (one-time, v0.1.0 only)

The release `applicationId` is `com.nexatv.launcher`; the build currently acting as HOME on the TV is the
**debug** package `com.nexatv.launcher.debug`. The first release is therefore a **separate install**, not an
in-place update:

- [ ] `adb install -r nexatv-0.1.0.apk` (installs alongside the debug build).
- [ ] Re-point the accessibility-HOME override to the **release** package in Settings → Accessibility.
- [ ] Reboot and confirm the *release* app is HOME.
- [ ] Optionally remove the debug build: `adb uninstall com.nexatv.launcher.debug`.

## Rollback

- [ ] The previous release APK stays on GitHub. To revert: `adb install -r nexatv-<previous>.apk` (same
      signer → in-place).
- [ ] If a bad release crash-loops HOME: trigger the stock launcher (the stock launcher was never disabled —
      ADR-0007), then `adb install -r` the previous stable APK.
