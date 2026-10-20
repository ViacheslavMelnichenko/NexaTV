# ADR-0003: CI/CD, signing, release & update/rollback strategy

## Status

**Accepted** — 2026-10-20. The recommended option on every decision point was adopted under the owner's
standing delegation to proceed on non-blocking choices; the three operationally-adjustable points (release
trigger, key-backup medium, MVP update mechanism) are noted below and may be revisited by a superseding ADR
if practice warrants. (Implements PRD v2 §15 / NFR-008..NFR-015, the owner's explicit "mandatory GitHub
CI/CD with understandable releases" emphasis. Supersedes nothing.)

## Context

NexaTV must be **built, tested, versioned, signed and released through a reproducible GitHub pipeline**,
with a safe update/rollback story (PRD v2 G6, §15). This ADR fixes *how*. Every claim below is weighed
against hard constraints already established:

- **FACT (distribution):** NexaTV is **not** distributed via Google Play. It is sideloaded (`adb`). There is
  therefore **no Google Play App Signing** — no managed key, no re-signing safety net, no Play-side rollback.
  The signing key is the **sole root of trust**: lose it and NexaTV can **never** be updated in place again
  (a fresh key = different package identity → uninstall/reinstall, losing user data). This single fact drives
  D3/D4 below.
- **FACT (in-place updates):** Android requires that an update be signed by the **same certificate** as the
  installed app. One long-lived certificate for all releases is mandatory for `adb install -r` to work.
- **FACT (minSdk 34):** APK Signature Scheme **v2+v3** cover minSdk 34; the legacy v1 (JAR) signature is not
  required and is disabled.
- **FACT (ABI):** target is `armeabi-v7a` only; a pure Kotlin/Compose app ships **no** `.so` (ADR-0002), but
  a transitive dependency could pull a native library — CI must *assert* the shipped APK has no 64-bit `.so`
  and (if any native lib appears) only `lib/armeabi-v7a`.
- **FACT (no device in CI):** GitHub-hosted runners have **no** 32-bit ARM device, and device-safety keeps
  the production TCL out of any automated pipeline → **performance NFRs (001/002/003) cannot be gated in
  cloud CI**; they are gated by a **manual pre-release on-device** Macrobenchmark run (NFR-013).
- **FACT (developer):** owner is an expert .NET/C# engineer, strong at Git/CI — the CI surface plays to their
  strength; Android signing/keystore mechanics are the new part.
- **ASSUMPTION:** solo maintainer, low release cadence (not a multi-dev team needing complex gating).

Non-goal (restated from PRD NFR-012): **full bit-for-bit reproducible builds** — impractical on the Android
toolchain for the value; we pursue *pinned, CI-built-from-tag* determinism instead.

## Decision

A set of coherent sub-decisions. Each lists the **chosen** option (the recommended one) and the honest
alternative. Three operationally-adjustable points are flagged **[ADJUSTABLE]** — accepted as the
recommended default, revisitable without re-opening the architecture.

**D1 — CI provider & PR gate (NFR-008).** **GitHub Actions** (owner-mandated). On every PR and push to
`main`, on `ubuntu-latest` (JDK 17, Android SDK 34): `assembleDebug` + Android Lint + `ktlint`/`detekt` +
JVM unit tests + **gitleaks** secret-scan + **ABI assert** (no 64-bit `.so`) + APK-size budget. Green is a
**required status check**. Target ≤10 min with Gradle caching.

**D2 — Branch & release model (NFR-009). [ADJUSTABLE: release trigger]** `main` is protected: no direct push,
PR-only, ≥1 required check, linear history. **Releases are triggered by pushing an annotated tag
`vX.Y.Z`** *(recommended)*. The release/publish job **never runs on fork PRs** and is scoped to a protected
GitHub **Environment** (`release`) so signing secrets are only exposed to the tag workflow.

**D3 — Signing key & certificate (NFR-010).** A **single, self-managed keystore** created **offline** by the
owner, holding one key with certificate **validity ≥ 30 years**; APK Signature Scheme **v2+v3** (v1 off).
One certificate for **all** releases → in-place updates. Debug/CI non-release builds use a **separate
throwaway debug key** — the release key is never used for debug jobs and never leaves the `release`
environment. (No alternative exists for custody: without Play App Signing, self-management is the only path;
the only real choice is *where the backup lives* — D4.)

**D4 — Secret storage & key backup (NFR-010). [ADJUSTABLE: backup medium]** The keystore (base64) + its
passwords live **only** in **GitHub Actions encrypted secrets**, scoped to the protected `release`
Environment — **plus** an **offline, encrypted owner-held backup** *(recommended: in the owner's password
manager AND on an offline encrypted medium, two copies)*. Secrets are **never** in Git, Gradle files, or
logs (CI masks them; `gitleaks` guards commits). The release job **fails fast** if the secrets are absent.
Key backup/rotation procedure is documented in the repo (process doc, not the secret itself).

**D5 — Versioning (NFR-011).** `versionName` = SemVer `MAJOR.MINOR.PATCH`. `versionCode` = deterministic
**`MAJOR*10000 + MINOR*100 + PATCH`** (strictly increasing). CI **rejects** a release whose `versionCode`
is ≤ the previous release's. Version source of truth is the Git tag (`vX.Y.Z` → derives both).

**D6 — Release artifact & provenance (NFR-011).** The tag workflow builds the **signed release APK** and
creates a **GitHub Release** containing: the APK, its **SHA-256**, the signing-cert **fingerprint**
(`apksigner verify --print-certs`), the exact **git SHA**, and notes from `CHANGELOG.md`
("Keep a Changelog"). The published APK **is** the CI-built artifact — never a local rebuild.

**D7 — Update & rollback (NFR-015). [ADJUSTABLE: update mechanism]** MVP update = **manual `adb install -r`**
of a signed APK with a higher `versionCode`, data preserved *(recommended for MVP)*. **No**
`REQUEST_INSTALL_PACKAGES`, **no** in-app/OTA updater at MVP (deferred — see Follow-ups). Each GitHub
Release keeps the **previous stable APK** downloadable for **one-step rollback**. Documented crash-loop
recovery: fall back to FallbackHome/stock launcher as Home, then `adb install` the previous stable APK.
Before promoting any release: a **manual on-device cold-start smoke test** on the real TCL (launch, reboot,
set-as-Home) as a checklist item.

**D8 — Determinism (NFR-012).** Pin the toolchain: Gradle wrapper, a **version catalog**, AGP/compileSdk 34,
JDK 17; a release is the CI artifact from a tagged commit. **Full bit-for-bit reproducibility = non-goal.**

**D9 — Performance gate (NFR-013).** NFR-001/002/003 are verified by a **manual pre-release Macrobenchmark
on the real TCL** (≥10 cold iterations, Baseline Profiles); p50/p95 start and idle PSS recorded in the
release notes; a release is **blocked** if cold-start p95 > 3.0 s or idle PSS > ~150 MB. CI runs only cheap
proxies (APK size, Baseline-Profile freshness, ABI assert).

## Alternatives

Weighed against: no-Play-Store distribution · lost-key = fatal · solo maintainer · CI has no 32-bit device.

| Decision | Options (recommended **bold**) | Why the recommendation |
|---|---|---|
| **CI provider (D1)** | **GitHub Actions** · GitLab CI · self-hosted runner | Owner mandated GitHub; hosted runners cover build/lint/test for free; self-hosted adds ops burden with no 32-bit device benefit |
| **Release trigger (D2)** | **Git tag `vX.Y.Z`** · manual `workflow_dispatch` · release-on-every-merge | Tag-driven makes a release an explicit, auditable, versioned act tied to a commit; manual dispatch is less traceable; per-merge over-produces releases for a solo, low-cadence project |
| **Key custody (D3)** | **Self-managed single keystore** · (Play App Signing — *unavailable*, no Play distribution) | No Play Store → self-management is the only path; one cert forever is required for in-place updates |
| **Secret + backup (D4)** | **GH encrypted secrets + offline owner backup (two copies)** · GH secrets only · external vault/OIDC | GH secrets are free and CI-native; the offline backup is the insurance against the fatal lost-key case; an external vault is overkill for a solo repo |
| **Update delivery (D7)** | **Manual `adb` sideload (MVP)** · self-hosted OTA/update server now · `REQUEST_INSTALL_PACKAGES` in-app updater now | Sideload needs zero infrastructure and no extra permissions; an updater/OTA is real surface + a sensitive permission + a server — deferred until the launcher itself is proven |
| **Reproducibility (D8)** | **Pinned + CI-from-tag** · full bit-for-bit repro | Bit-for-bit is high-effort/low-value on Android for a solo project; pinned CI-from-tag gives the provenance that actually matters |

## Consequences

**Positive**
- Every release is an **auditable, signed, versioned** artifact built by CI from a tagged commit, with
  verifiable integrity (SHA-256 + cert fingerprint) — "understandable releases" as the owner asked.
- The fatal **lost-key** risk is mitigated by a redundant offline backup; in-place updates are guaranteed by
  the single-cert rule.
- Zero release infrastructure to run (no server, no OTA) and **no extra/sensitive permissions** for MVP —
  reinforces the privacy posture (NFR-006/021).
- Secret exposure is minimized: release secrets live behind a protected Environment, never touch PR/fork runs.
- Plays directly to the owner's CI/Git strength; the only new learning is Android keystore mechanics.

**Negative / costs (honest)**
- **Performance cannot be gated in CI** (no 32-bit device) → relies on a disciplined **manual** on-device
  step (D9/NFR-013); a skipped manual run can let a perf regression ship. *Mitigation:* make it a hard,
  checklist-enforced release gate recorded in the release notes.
- **Manual sideload updates** are less convenient than OTA and require physical/`adb` access for every update
  (acceptable for a single owner-operated TV; revisited post-MVP).
- **Self-managed signing** puts full key-custody responsibility on the owner (no Play safety net).
- A **mis-set `versionCode`** or lost tag discipline can block updates. *Mitigation:* CI rejects
  non-increasing `versionCode`; version derives from the tag.

**Mitigations (summary)**
- CI asserts: increasing `versionCode`, no secrets committed (gitleaks), no 64-bit `.so`, APK-size budget.
- Release secrets scoped to a protected `release` Environment; release job fails fast if absent.
- Previous-stable APK retained per release for one-step rollback; documented FallbackHome recovery.
- Keystore generated offline, backed up in two places, documented rotation/backup procedure.

## Rationale

The dominant fact is that **there is no Play Store and therefore no managed signing** — which turns key
custody from a routine concern into the single most consequential decision in the pipeline, and makes an
offline key backup non-negotiable. Given that, the cheapest correct design is: let **GitHub Actions** (which
the owner mandated and is strong at) do all the free work — build, lint, test, secret-scan, ABI-assert — and
make a **release an explicit tagged act** that produces a signed, provenance-stamped artifact. Everything the
cloud *cannot* do on a 32-bit TV (measure real performance, prove the Home survives reboots) is pushed to a
**disciplined manual on-device gate**, honestly acknowledged rather than pretended-automated. Updates stay
**manual sideload** for MVP because an OTA/updater is real infrastructure plus a sensitive permission that
contradicts the lean/private posture, and it should not be built until the launcher it updates is proven.
Reproducibility is scoped to *pinned, CI-built-from-tag* because bit-for-bit repro is high-effort, low-value
here. The design is deliberately **boring and recoverable** — which is exactly what a production Home device
needs.

## Follow-ups

*(Implementation lands in M1 — Foundation. Secrets/keys are runtime data: generated and stored securely,
**never committed**, per Hard Rule 4.)*

- Author the GitHub Actions workflows: `ci.yml` (PR/main gate — D1) and `release.yml` (tag-driven — D2/D6).
- Configure branch protection on `main` and the protected **`release`** Environment.
- Generate the release keystore **offline**; store base64 + passwords in GH encrypted secrets **and** two
  offline backups; write the key backup/rotation procedure (`docs/technical/RELEASE-SIGNING.md`) — no secret
  values in Git.
- Add `CHANGELOG.md` ("Keep a Changelog") and wire release notes + SHA-256 + cert fingerprint + git SHA.
- Implement the Gradle `versionCode`/`versionName`-from-tag logic and the non-increasing-version CI check.
- Add CI asserts: `gitleaks`, ABI (no 64-bit `.so`), APK-size budget, Baseline-Profile freshness.
- Document the manual pre-release gate (on-device Macrobenchmark + cold-start smoke + reboot) as a release
  checklist (`docs/testing/RELEASE-CHECKLIST.md`).
- **[Deferred / potential future ADR]** in-app/OTA self-update (would introduce `REQUEST_INSTALL_PACKAGES`
  and a distribution endpoint) — only if sideload becomes a real pain point post-MVP.
- Accepted under owner delegation (2026-10-20); the **[ADJUSTABLE]** points may be revisited via a
  superseding ADR if practice warrants.
