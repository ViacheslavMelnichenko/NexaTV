# Release signing — owner setup

> **Audience:** the repository owner, done **once**. This is the manual, offline half of the release
> pipeline ([ADR-0003](../adr/ADR-0003-cicd-signing-release.md)). The automated half is
> [`.github/workflows/release.yml`](../../.github/workflows/release.yml).
>
> **No secret values ever go in Git** (Hard Rule 4) — not the keystore, not a password, not the base64.

## 1. Why this matters

NexaTV is sideloaded, so there is **no Google Play key management and no recovery path**. The signing
certificate *is* the app's identity:

- **Same cert → in-place updates** that preserve user data (`adb install -r`).
- **Lost or changed cert → fatal.** Android refuses to update an app with a different signer. The only
  way forward is uninstall + reinstall, which wipes the app's data, and every future "update" becomes a
  fresh install. There is no Play Console "reset upload key" safety net here.

Treat the keystore like the master key it is: generate it once, back it up twice, never commit it.

## 2. Generate the keystore (offline)

Run on a trusted machine (not in CI). Pick strong, **distinct** store and key passwords and record them
in a password manager as you go.

```bash
keytool -genkeypair -v \
  -keystore nexatv-release.jks \
  -alias nexatv \
  -keyalg RSA -keysize 4096 \
  -validity 10950 \
  -storetype PKCS12
```

- `-validity 10950` ≈ **30 years** — the cert must outlive the app; an expired cert blocks updates.
- `-storetype PKCS12` — the modern default; one key, one alias (`nexatv`).
- Answer the distinguished-name prompts (name/org/locale) however you like; they are cosmetic.

Verify what you produced:

```bash
keytool -list -v -keystore nexatv-release.jks
```

## 3. Add the GitHub secrets (protected `release` Environment)

Repo → **Settings → Environments → `release`** → *Add secret*. Four secrets, matching
`release.yml`:

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | base64 of the `.jks` file (command below) |
| `KEYSTORE_PASSWORD` | the store password from step 2 |
| `KEY_ALIAS` | `nexatv` |
| `KEY_PASSWORD` | the key password from step 2 |

Encode the keystore to a single line:

```bash
# Linux / Git Bash
base64 -w0 nexatv-release.jks            # or: base64 nexatv-release.jks | tr -d '\n'
```

Paste the resulting string as `KEYSTORE_BASE64`. The workflow decodes it to a temp file and feeds the
four `NEXATV_*` env vars that `app/build.gradle.kts` reads.

## 4. Protect the Environment and `main`

- **`release` Environment:** require the owner as a reviewer (or restrict deployment branches to
  tags/`main`) so a tag push can't expose the signing secrets from an untrusted branch.
- **`main` branch protection:** PR-only (no direct pushes), **≥1 required status check** (the `CI`
  workflow), and **linear history**. Releases are cut from protected `main`.

## 5. Back up the keystore — twice, offline

Non-negotiable (see §1):

1. The password manager entry (store + key passwords **and** the base64 or the `.jks` itself).
2. An **encrypted offline medium** (e.g. an encrypted USB drive kept physically separate).

Never email it, never put it in the repo, never leave it in a synced cloud folder unencrypted.

## 6. Rotation (avoid if possible)

A new certificate changes the app's signer identity → existing installs **cannot** be updated in place;
the owner must uninstall and reinstall (data loss). There is no in-place migration for sideloaded apps.
Only rotate if the key is compromised, and accept the one-time reinstall cost when you do.

## 7. Verify a published release

Every GitHub Release records the APK SHA-256 and the signing-cert SHA-256 fingerprint. To confirm an APK
before trusting it on the TV:

```bash
apksigner verify --print-certs nexatv-X.Y.Z.apk      # cert SHA-256 must match the Release notes
sha256sum nexatv-X.Y.Z.apk                            # APK SHA-256 must match the Release notes
```

Then follow [`docs/testing/RELEASE-CHECKLIST.md`](../testing/RELEASE-CHECKLIST.md) for the on-device gate.
