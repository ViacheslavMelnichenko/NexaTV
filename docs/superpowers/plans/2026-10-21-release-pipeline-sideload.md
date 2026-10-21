# Release Pipeline (Sideload) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the already-accepted ADR-0003 release pipeline so NexaTV has a real signed release built by CI from a git tag, delivered by `adb` sideload.

**Architecture:** Gradle gains an env-fed release `signingConfig` and tag-driven version props; a tag-triggered `release.yml` builds/signs/verifies the APK and publishes a GitHub Release with provenance; two docs cover the owner's offline keystore setup and the manual on-device release gate. No Google Play, no OTA, no new permissions.

**Tech Stack:** Gradle Kotlin DSL (AGP 8.7.3), GitHub Actions, `apksigner`, `gh` CLI, bash.

**Spec:** [`docs/superpowers/specs/2026-10-21-release-pipeline-sideload-design.md`](../specs/2026-10-21-release-pipeline-sideload-design.md)

---

## File structure

- Modify `app/build.gradle.kts` — release `signingConfig` from env; `versionName`/`versionCode` from `-P` props with in-file fallback.
- Create `.github/workflows/release.yml` — tag `v*.*.*` → build, sign, assert, verify, publish.
- Create `docs/technical/RELEASE-SIGNING.md` — owner keystore + GitHub secrets/Environment + branch protection.
- Create `docs/testing/RELEASE-CHECKLIST.md` — manual pre-release on-device gate + rollback.
- Modify `docs/technical/ADB.md` — "Updating NexaTV on the TV" section.
- Modify `CHANGELOG.md` — record the release pipeline under `[Unreleased]`.

---

### Task 1: Gradle release signing + version-from-tag

**Files:**
- Modify: `app/build.gradle.kts` (`defaultConfig` version lines ~32-34; `buildTypes.release` ~49-57; add `signingConfigs`)

- [ ] **Step 1: Add version-from-property resolution above `android {}`**

Insert after the `detekt {}` block, before `android {`:

```kotlin
// Release versioning (ADR-0003 D5). CI passes -PversionName/-PversionCode derived from the git tag;
// local/dev and CI non-release builds fall back to the in-file defaults so they always build.
val releaseVersionName = (project.findProperty("versionName") as String?) ?: "0.1.0"
val releaseVersionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 100
```

- [ ] **Step 2: Use the resolved version in `defaultConfig`**

Replace the hardcoded lines:

```kotlin
        // NFR-011: versionCode = MAJOR*10000 + MINOR*100 + PATCH. 0.1.0 -> 100.
        versionCode = 100
        versionName = "0.1.0"
```

with:

```kotlin
        // NFR-011: versionCode = MAJOR*10000 + MINOR*100 + PATCH (0.1.0 -> 100). Overridable from the
        // git tag via -PversionCode/-PversionName in release CI; defaults above keep local builds working.
        versionCode = releaseVersionCode
        versionName = releaseVersionName
```

- [ ] **Step 3: Add an env-fed release `signingConfig` inside `android {}`**

Add, directly after the `defaultConfig { ... }` block closes and before `buildTypes {`:

```kotlin
    // Release signing (ADR-0003 D3/D4). The key is read from environment variables injected by release
    // CI from encrypted secrets — never from Git. When absent (local/dev), the release build is left
    // UNSIGNED rather than failing, and CI fails fast earlier if a *release* is attempted without secrets.
    val keystorePath = System.getenv("NEXATV_KEYSTORE_FILE")
    val hasKeystore = keystorePath != null && file(keystorePath).exists()
    signingConfigs {
        create("release") {
            if (hasKeystore) {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("NEXATV_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("NEXATV_KEY_ALIAS")
                keyPassword = System.getenv("NEXATV_KEY_PASSWORD")
            }
        }
    }
```

- [ ] **Step 4: Wire the signingConfig into the release build type**

In `buildTypes { release { ... } }`, replace the comment line:

```kotlin
            // Signing is wired in CI from secrets (ADR-0003 / NFR-010). No keys in Git.
```

with:

```kotlin
            // Signed only when the keystore env is present (release CI); unsigned locally (ADR-0003 D3/D4).
            signingConfig = if (hasKeystore) signingConfigs.getByName("release") else null
```

- [ ] **Step 5: Verify version override + unsigned release build locally**

Run: `./tools/gradle-8.9/bin/gradle.bat :app:assembleRelease -PversionName=9.9.9 -PversionCode=90909 --no-daemon`
Expected: `BUILD SUCCESSFUL`; an **unsigned** APK at `app/build/outputs/apk/release/app-release-unsigned.apk` (no keystore env → unsigned; validates the R8/minify release build and proguard-keeps too).

- [ ] **Step 6: Commit**

```bash
git add app/build.gradle.kts
git -c user.name="ViacheslavMelnichenko" -c user.email="melnichenkoviatcheslav@gmail.com" \
  commit -m "build(release): env-fed signing config and tag-driven version"
```

---

### Task 2: Release workflow

**Files:**
- Create: `.github/workflows/release.yml`

- [ ] **Step 1: Verify the version-derivation logic in isolation first**

Run (proves the math before baking it into YAML):

```bash
TAG=v0.1.0; NAME="${TAG#v}"; CORE="${NAME%%-*}"; IFS=. read -r MA MI PA <<< "$CORE"; echo "$NAME -> $((MA*10000+MI*100+PA))"
```
Expected: `0.1.0 -> 100`. Also try `TAG=v1.2.3` → `1.2.3 -> 10203`, and `TAG=v0.1.0-rc` → `0.1.0-rc -> 100`.

- [ ] **Step 2: Create `.github/workflows/release.yml`**

```yaml
# NexaTV — tag-driven signed release (ADR-0003 D2/D4/D5/D6). Sideload distribution: the published
# APK is installed with `adb install -r`. Never runs on fork PRs; signing secrets live only in the
# protected `release` Environment. Performance NFRs are a manual on-device gate (docs/testing/RELEASE-CHECKLIST.md).
name: Release

on:
  push:
    tags: ['v*.*.*']

permissions:
  contents: write   # create the GitHub Release

concurrency:
  group: release-${{ github.ref }}
  cancel-in-progress: false

jobs:
  release:
    name: Build · Sign · Publish
    runs-on: ubuntu-latest
    timeout-minutes: 25
    environment: release
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Set up Gradle (with cache)
        uses: gradle/actions/setup-gradle@v4

      - name: Derive version from tag
        id: ver
        run: |
          TAG="${GITHUB_REF_NAME}"
          NAME="${TAG#v}"
          CORE="${NAME%%-*}"
          IFS=. read -r MAJOR MINOR PATCH <<< "$CORE"
          CODE=$(( MAJOR*10000 + MINOR*100 + PATCH ))
          echo "name=$NAME" >> "$GITHUB_OUTPUT"
          echo "code=$CODE" >> "$GITHUB_OUTPUT"
          echo "Derived versionName=$NAME versionCode=$CODE"

      - name: Guard — versionCode must strictly increase
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          PREV_TAG=$(gh release list --limit 1 --json tagName --jq '.[0].tagName' 2>/dev/null || true)
          if [ -z "$PREV_TAG" ]; then echo "No prior release; skipping monotonic check."; exit 0; fi
          PREV_CORE="${PREV_TAG#v}"; PREV_CORE="${PREV_CORE%%-*}"
          IFS=. read -r a b c <<< "$PREV_CORE"
          PREV_CODE=$(( a*10000 + b*100 + c ))
          echo "previous $PREV_TAG ($PREV_CODE) vs new ${{ steps.ver.outputs.code }}"
          if [ "${{ steps.ver.outputs.code }}" -le "$PREV_CODE" ]; then
            echo "::error::versionCode ${{ steps.ver.outputs.code }} <= previous $PREV_CODE"; exit 1
          fi

      - name: Signing fail-fast (never publish unsigned)
        env:
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
        run: |
          if [ -z "$KEYSTORE_BASE64" ]; then
            echo "::error::Signing secrets absent — refusing to publish an unsigned release (ADR-0003 D4)."
            exit 1
          fi

      - name: Decode keystore
        id: keystore
        env:
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
        run: |
          echo "$KEYSTORE_BASE64" | base64 -d > "$RUNNER_TEMP/nexatv-release.jks"
          echo "path=$RUNNER_TEMP/nexatv-release.jks" >> "$GITHUB_OUTPUT"

      - name: Assemble signed release
        env:
          NEXATV_KEYSTORE_FILE: ${{ steps.keystore.outputs.path }}
          NEXATV_KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          NEXATV_KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          NEXATV_KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: |
          ./gradlew :app:assembleRelease \
            -PversionName=${{ steps.ver.outputs.name }} \
            -PversionCode=${{ steps.ver.outputs.code }} \
            --stacktrace

      - name: Locate & name the APK
        id: apk
        run: |
          SRC=app/build/outputs/apk/release/app-release.apk
          test -f "$SRC" || { echo "::error::signed release APK missing at $SRC"; exit 1; }
          OUT="nexatv-${{ steps.ver.outputs.name }}.apk"
          cp "$SRC" "$OUT"
          echo "file=$OUT" >> "$GITHUB_OUTPUT"

      - name: ABI assert (armeabi-v7a only, no 64-bit .so)
        run: |
          BAD=$(unzip -l "${{ steps.apk.outputs.file }}" | awk '{print $4}' | grep '^lib/' | grep -vE '^lib/armeabi-v7a/' || true)
          if [ -n "$BAD" ]; then echo "::error::Forbidden native libs:"; echo "$BAD"; exit 1; fi
          echo "ABI OK."

      - name: Size budget (release <= 20 MB)
        run: |
          SIZE=$(stat -c%s "${{ steps.apk.outputs.file }}"); MAX=$((20*1024*1024))
          echo "Release APK: $SIZE bytes (budget $MAX)."
          [ "$SIZE" -le "$MAX" ] || { echo "::error::release APK exceeds 20 MB budget"; exit 1; }

      - name: Verify signature & capture provenance
        id: prov
        run: |
          BT=$(ls -d "$ANDROID_SDK_ROOT"/build-tools/* | sort -V | tail -1)
          "$BT/apksigner" verify --print-certs "${{ steps.apk.outputs.file }}" | tee certs.txt
          SHA=$(sha256sum "${{ steps.apk.outputs.file }}" | awk '{print $1}')
          FP=$(grep -m1 'SHA-256 digest' certs.txt | awk '{print $NF}')
          echo "sha=$SHA" >> "$GITHUB_OUTPUT"
          echo "fp=$FP" >> "$GITHUB_OUTPUT"

      - name: Extract changelog notes + provenance
        id: notes
        run: |
          VER="${{ steps.ver.outputs.name }}"
          awk -v v="$VER" '
            $0 ~ "^## \\[" v "\\]" {grab=1; next}
            grab && /^## \[/ {exit}
            grab {print}
          ' CHANGELOG.md > notes.md
          if [ ! -s notes.md ]; then echo "NexaTV $VER" > notes.md; fi
          {
            echo ""
            echo "---"
            echo "**APK SHA-256:** \`${{ steps.prov.outputs.sha }}\`"
            echo "**Signing cert SHA-256:** \`${{ steps.prov.outputs.fp }}\`"
            echo "**Commit:** \`${{ github.sha }}\`"
          } >> notes.md

      - name: Publish GitHub Release
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          PRERELEASE=""
          case "${{ steps.ver.outputs.name }}" in *-*) PRERELEASE="--prerelease";; esac
          gh release create "${GITHUB_REF_NAME}" \
            "${{ steps.apk.outputs.file }}" \
            --title "NexaTV ${{ steps.ver.outputs.name }}" \
            --notes-file notes.md \
            $PRERELEASE
```

- [ ] **Step 3: Sanity-check YAML parses**

Run: `./tools/gradle-8.9/bin/gradle.bat help --no-daemon -q >/dev/null 2>&1 || true` (Gradle can't lint YAML; instead eyeball indentation and confirm the file was written). If `python` is available: `python -c "import yaml,sys; yaml.safe_load(open('.github/workflows/release.yml'))" && echo OK`.
Expected: `OK` (or clean visual review if no python).

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/release.yml
git -c user.name="ViacheslavMelnichenko" -c user.email="melnichenkoviatcheslav@gmail.com" \
  commit -m "ci(release): tag-driven signed release workflow (ADR-0003)"
```

---

### Task 3: Owner signing & GitHub setup doc

**Files:**
- Create: `docs/technical/RELEASE-SIGNING.md`

- [ ] **Step 1: Write the doc** with these sections (full content — no placeholders):
  1. **Why this matters** — the key is the sole root of trust; lost key = app can never be updated in place again (ADR-0003 D3).
  2. **Generate the keystore (offline)**:
     ```bash
     keytool -genkeypair -v \
       -keystore nexatv-release.jks \
       -alias nexatv \
       -keyalg RSA -keysize 4096 \
       -validity 10950 \
       -storetype PKCS12
     ```
     (10950 days ≈ 30 years; choose strong, distinct store and key passwords.)
  3. **Add GitHub secrets** (repo → Settings → Environments → `release`): `KEYSTORE_BASE64`
     (`base64 -w0 nexatv-release.jks` on Linux, or `base64 nexatv-release.jks | tr -d '\n'`),
     `KEYSTORE_PASSWORD`, `KEY_ALIAS` (`nexatv`), `KEY_PASSWORD`.
  4. **Protect the `release` Environment** and `main` branch (PR-only, ≥1 required check, linear history).
  5. **Back up the keystore twice** — password manager + an offline encrypted medium. Never commit it.
  6. **Rotation note** — a new cert changes package identity → uninstall/reinstall (data loss); avoid.
  7. **Verify a release** — `apksigner verify --print-certs`; match the fingerprint in the GitHub Release.

- [ ] **Step 2: Commit**

```bash
git add docs/technical/RELEASE-SIGNING.md
git -c user.name="ViacheslavMelnichenko" -c user.email="melnichenkoviatcheslav@gmail.com" \
  commit -m "docs(release): offline keystore + GitHub signing setup procedure"
```

---

### Task 4: Release checklist doc

**Files:**
- Create: `docs/testing/RELEASE-CHECKLIST.md`

- [ ] **Step 1: Write the ordered checklist** (full content):
  - **Pre-tag:** move `CHANGELOG.md` `[Unreleased]` → `[x.y.z] - YYYY-MM-DD`; CI green on `main`; `versionCode` will strictly increase.
  - **Tag:** `git tag -a vX.Y.Z -m "NexaTV X.Y.Z" && git push origin vX.Y.Z` → `release.yml` runs.
  - **On-device gate (real TCL):** download the signed APK from the GitHub Release; `adb install -r nexatv-X.Y.Z.apk`; **smoke test** — launch, set-as-HOME (accessibility), reboot survives, cold-start sanity; confirm icons + weather render on the **minified** build (R8 can differ from debug).
  - **Perf:** record cold-start/PSS if available; the benchmark device-install gap is open, so perf is **recorded, not gated** for now — state plainly.
  - **First-release migration:** the release `applicationId` is `com.nexatv.launcher` (no `.debug`) → a separate install from the debug build on the TV; re-point the accessibility-HOME override to the release package; optionally remove the debug build.
  - **Rollback:** the previous release APK stays on GitHub → `adb install -r` the previous one; if HOME crash-loops, fall back to the stock launcher, then install the previous stable APK.

- [ ] **Step 2: Commit**

```bash
git add docs/testing/RELEASE-CHECKLIST.md
git -c user.name="ViacheslavMelnichenko" -c user.email="melnichenkoviatcheslav@gmail.com" \
  commit -m "docs(release): manual on-device release checklist + rollback"
```

---

### Task 5: TV-update docs + changelog

**Files:**
- Modify: `docs/technical/ADB.md` (append a section before "## Command log")
- Modify: `CHANGELOG.md` (under `[Unreleased]` → `### Added`)

- [ ] **Step 1: Add an "Updating NexaTV on the TV" section to `ADB.md`**

```markdown
## Updating NexaTV on the TV

Updates are sideloaded (ADR-0003 D7). The signed APK comes from the GitHub Release.

| Action | Command | Notes |
|--------|---------|-------|
| Update in place | `adb install -r nexatv-X.Y.Z.apk` | Same signing cert → data preserved |
| First switch off debug | `adb install -r nexatv-X.Y.Z.apk` | Release id `com.nexatv.launcher` is a **separate** package from the `.debug` build; after installing, re-point the accessibility-HOME override to it (then optionally `adb uninstall com.nexatv.launcher.debug`) |
| Roll back | `adb install -r nexatv-<prev>.apk` | Previous release APK is kept on GitHub |

> The release build is **minified (R8)**; always run the on-device smoke test in
> [RELEASE-CHECKLIST.md](../testing/RELEASE-CHECKLIST.md) before trusting a new version.
```

- [ ] **Step 2: Record the pipeline in `CHANGELOG.md`** under `[Unreleased]` → `### Added`:

```markdown
- Release pipeline (ADR-0003): tag-driven `.github/workflows/release.yml` builds, signs (env-fed
  keystore from the protected `release` Environment), asserts ABI + 20 MB size budget, verifies the
  signature, and publishes a GitHub Release with SHA-256 + cert fingerprint + git SHA. Gradle gains an
  env-fed release `signingConfig` and tag-derived `versionName`/`versionCode`. Owner setup in
  `docs/technical/RELEASE-SIGNING.md`; manual on-device gate in `docs/testing/RELEASE-CHECKLIST.md`.
```

- [ ] **Step 3: Commit**

```bash
git add docs/technical/ADB.md CHANGELOG.md
git -c user.name="ViacheslavMelnichenko" -c user.email="melnichenkoviatcheslav@gmail.com" \
  commit -m "docs(release): TV update/rollback steps and changelog entry"
```

---

### Task 6: Full verification

- [ ] **Step 1: Run the device-free CI lane** to confirm nothing regressed

Run: `./tools/gradle-8.9/bin/gradle.bat :app:assembleDebug :app:testDebugUnitTest :app:detekt :app:lintDebug --no-daemon`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Confirm the signed-path builds with a throwaway keystore** (optional but recommended)

```bash
keytool -genkeypair -v -keystore /tmp/throwaway.jks -alias t -keyalg RSA -keysize 2048 \
  -validity 30 -storetype PKCS12 -storepass test1234 -keypass test1234 \
  -dname "CN=throwaway"
NEXATV_KEYSTORE_FILE=/tmp/throwaway.jks NEXATV_KEYSTORE_PASSWORD=test1234 \
  NEXATV_KEY_ALIAS=t NEXATV_KEY_PASSWORD=test1234 \
  ./tools/gradle-8.9/bin/gradle.bat :app:assembleRelease -PversionName=0.1.0 -PversionCode=100 --no-daemon
```
Expected: `BUILD SUCCESSFUL`; a **signed** `app-release.apk` (not `-unsigned`). Delete `/tmp/throwaway.jks` after.

---

## Self-review

- **Spec coverage:** A (Task 1) · B (Task 2) · C (Task 3) · D (Task 4) · E (Task 5); risks (R8, migration, lost key, unsigned-until-secrets) are reflected in Tasks 4/5 docs and the fail-fast step in Task 2. ✓
- **Placeholders:** none — all docs/code shown or fully specified. ✓
- **Type/name consistency:** env vars `NEXATV_KEYSTORE_FILE/PASSWORD`, `NEXATV_KEY_ALIAS/PASSWORD` and secrets `KEYSTORE_BASE64/PASSWORD`, `KEY_ALIAS/PASSWORD` match across Gradle, workflow, and docs; APK name `nexatv-<version>.apk` consistent. ✓
