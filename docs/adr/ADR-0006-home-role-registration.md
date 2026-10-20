# ADR-0006: HOME registration, pre-Home prompt & reversibility

## Status

**Accepted** — 2026-10-20. (Implements PRD FR-001 and the FR-015 pre-Home prompt; depends on
[ADR-0004](./ADR-0004-app-enumeration-visibility.md), which couples full app visibility to holding
`ROLE_HOME`. Accepted under the owner's standing delegation on non-blocking choices: the mechanism is
forced by platform policy and the project's device-safety stance, so there is no genuine owner trade-off to
defer. The one real UX decision — gate the grid behind the prompt vs. show a partial list — is already
settled by the PRD, "an explicit first-run state … not a partial/empty list.")

## Context

NexaTV must be able to *become* the TV's HOME app (FR-001) and, until it is, must tell the user how —
because app visibility is coupled to the Home role (ADR-0004). Two things are in scope here: **how a user
makes NexaTV the default Home**, and **what NexaTV shows before that happens**. Device safety (`initial.md`
§16) is paramount: the TV is a daily-use production device, so the stock launcher must never be disabled and
the user must be able to switch back at any time.

- **FACT (device audit, `docs/technical/ANDROID-TV.md`):** `ROLE_HOME` is present and functional on the TCL;
  current holder is the stock `com.google.android.apps.tv.launcherx`. The system also exposes
  `com.android.tv.settings/.system.FallbackHome` as a HOME activity (a natural recovery fallback).
- **FACT (SDK 34):** `android.app.role.RoleManager` (API 29+) is available with no version guard. It provides
  `isRoleHeld(ROLE_HOME)` and `createRequestRoleIntent(ROLE_HOME)`, which drive the **system-owned**,
  user-consented, reversible flow for granting a role. The system — not the app — changes the default.
- **FACT:** the HOME launcher Activity is already declared (manifest intent-filter: `MAIN` + `HOME` +
  `DEFAULT` + `LEANBACK_LAUNCHER`), so NexaTV is an *eligible* Home candidate; being *selected* is a separate,
  user-driven step.
- **FACT:** NexaTV declares no `RECEIVE_BOOT_COMPLETED` (FR-009 / low-RAM posture). When set as Home, the
  system auto-starts it after boot via the HOME intent — no boot receiver needed.

## Decision

1. **Become Home via `RoleManager.createRequestRoleIntent(ROLE_HOME)`**, launched with an ActivityResult
   contract. The system shows its own consent dialog / Home picker; NexaTV never writes the default itself.
2. **Detect state via `RoleManager.isRoleHeld(ROLE_HOME)`.** This is the single source of truth for "are we
   Home", decoupled from how many apps enumeration happens to return.
3. **Pre-Home prompt (FR-015):** when NexaTV does **not** hold `ROLE_HOME`, the Home surface is an explicit
   prompt — a title, a one-line explanation, and a focusable primary action that fires the role-request
   intent — **instead of** the app grid. Never a silently partial or empty grid. Once the role is held, the
   grid is shown.
4. **Re-check on return.** After the role-request result and on every `ON_RESUME`, re-read `isRoleHeld` and
   refresh — so setting (or clearing) Home via system Settings is reflected without a restart.
5. **Reversibility & safety (`initial.md` §16).** NexaTV never disables the stock launcher, never clears
   another app's defaults, and never forces itself as Home. Switching back is a normal system action
   (Settings → Apps → Default apps → Home, or re-granting the role to the stock launcher). Recovery if NexaTV
   ever misbehaves as Home: the system falls back to `FallbackHome`/stock; `adb` can reinstall a prior APK.
6. **No boot receiver.** Rely on the system to start Home after reboot (FR-009); do not declare
   `RECEIVE_BOOT_COMPLETED`.

## Alternatives

| Option (recommended **bold**) | Reversible? | Verdict |
|---|---|---|
| **`RoleManager.createRequestRoleIntent(ROLE_HOME)`** | Yes — system-owned, user-consented | **Chosen.** First-party, API-29+ clean, no system mutation by us; the OS changes the default |
| `Settings.ACTION_HOME_SETTINGS` deep-link to the Home picker | Yes | Usable fallback if the role dialog is absent on this firmware, but one extra manual step and less direct; keep as a documented backup |
| Programmatically clear/set preferred Home (legacy `clearPackagePreferredActivities` / set-preferred) | Risky | **Rejected.** Mutates system defaults, fragile across firmware, violates device-safety ("smallest reversible change", never disable stock) |
| Show a partial app grid before Home, no prompt | — | **Rejected.** Contradicts FR-015 / ADR-0004 (a partial list reads as a bug, not a state) |

## Consequences

**Positive**
- The OS owns the default-Home change → structurally safe and reversible; NexaTV holds no privileged power
  over the system launcher. Upholds device-safety and least-privilege (NFR-021).
- One honest, legible first-run step; full visibility (ADR-0004) and being Home are the *same* gate, so the
  prompt both explains and unlocks the app list.
- No boot receiver → no low-RAM/background cost (FR-009).

**Negative / costs (honest)**
- The app grid is unavailable until the user grants the role — intentional per FR-015, but it means the very
  first launch is a prompt, not apps.
- The exact system UI for `createRequestRoleIntent(ROLE_HOME)` on this TCL firmware is **INFERENCE until
  verified on-device** (could be a dialog or the full Home picker); the `ACTION_HOME_SETTINGS` fallback is
  the mitigation if the role dialog is missing.

## Rationale

The platform already provides a safe, reversible, user-consented path to change the Home app — letting the OS
own that change is exactly right for a daily-use production TV, where silently mutating system defaults would
be unacceptable. Gating the grid behind the prompt isn't a limitation we invented; it is the honest surface of
ADR-0004's visibility coupling, and the PRD already chose "explicit prompt, not a partial list." Relying on the
system to start Home after boot keeps the memory posture clean (no boot receiver), consistent with FR-009.

## Follow-ups

- **On-device acceptance (FR-001/FR-015):** confirm on the real TCL that the role-request intent sets NexaTV
  as Home, that pressing HOME then opens NexaTV, that the launchable count equals the stock launcher's
  (delta 0, per ADR-0004), and that switching back to the stock launcher works. Record in
  `docs/technical/ANDROID-TV.md`.
- If the role dialog is absent on this firmware, wire the `Settings.ACTION_HOME_SETTINGS` fallback and note it.
- Reboot behavior (FR-009) and crash-recovery path are verified as part of M3/M14; document the observed
  behavior (this closes the "launcher HOME strategy — verify on-device" item in the ADR index).
