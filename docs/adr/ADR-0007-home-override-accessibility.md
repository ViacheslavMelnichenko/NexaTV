# ADR-0007: Becoming HOME via an accessibility override (Google TV firmware)

## Status

**Accepted** — 2026-10-20. Supersedes the *become-Home mechanism* of
[ADR-0006](./ADR-0006-home-role-registration.md) (the `RoleManager` role request) on firmware that
blocks third-party HOME replacement. The rest of ADR-0006 — the pre-Home prompt (FR-015), the
reversibility/device-safety stance, and "no boot receiver" (FR-009) — remains in force. Accepted under the
owner's standing delegation on non-blocking choices, and on the owner's explicit direction (2026-10-20) to
make the override the primary mechanism after the prototype was validated on the real TCL.

## Context

ADR-0006 assumed `ROLE_HOME` was "present and functional" on the TCL and that granting it would make
NexaTV the effective HOME. On-device validation (2026-10-20, recorded in
`docs/technical/ANDROID-TV.md` §3b) refuted that assumption for this Google TV firmware.

- **FACT (on-device, TCL Google TV, Android 14):** granting `ROLE_HOME` through the official
  `RequestRoleActivity` dialog updates the role-holder *record*, but the **effective** HOME — what the
  resolver returns and what the physical HOME button opens — stays the stock
  `com.google.android.apps.tv.launcherx`. Confirmed immediately and after a full reboot.
- **FACT:** `RoleManager.isRoleAvailable(ROLE_HOME)` returns **false** on this firmware, and the
  role-holder record disagrees with `PackageManager.resolveActivity(CATEGORY_HOME)`. The role API is not a
  reliable source of truth here; the resolver is.
- **FACT:** `cmd package set-home-activity …/nexatv` returns `Success` but has **no effect** on the
  effective HOME. Programmatic third-party HOME assignment is blocked.
- **FACT:** this is the documented Google TV (vs. AOSP Android TV) restriction — Google TV pins its own
  launcher to the HOME button.
- **CONSTRAINT (`initial.md` §16):** the stock launcher must never be disabled and the TV must stay
  recoverable. Disabling stock (`pm disable-user`) *would* free the HOME button but is **forbidden** by
  device-safety, so it is off the table.

The question ADR-0007 answers: **given HOME replacement is blocked, how does NexaTV still open on HOME
without disabling the stock launcher?**

## Decision

1. **Become HOME via an accessibility override** (the technique Projectivy uses). A bound
   `AccessibilityService` (`NexaOverrideService`) listens for `TYPE_WINDOW_STATE_CHANGED` from the stock
   launcher package and immediately raises `MainActivity` over it. It does **not** intercept the HOME key
   (the system consumes it first); it reacts to the stock launcher coming to the foreground, which is what
   HOME triggers.
2. **Enable only through the owner-consented Settings flow.** The service is declared with
   `BIND_ACCESSIBILITY_SERVICE` and enabled by the owner in Settings → Accessibility. **FACT (on-device):**
   this firmware binds the service only when enabled via that UI — enabling it with `adb settings put
   secure enabled_accessibility_services …` is ignored (the service shows as *Enabled* but
   `Bound services:{}`), and the TCL `AppBoot`/`AUTO_START` manager additionally rejects the bind until the
   consented enable. So the override is always a real, user-granted, reversible state.
3. **Detect "are we HOME" as role-or-override.** `HomeRoleRepository.isEffectivelyHome()` returns true when
   either the resolver names NexaTV (true default Home, where it works) **or** `NexaOverrideService` is in
   `AccessibilityManager.getEnabledAccessibilityServiceList(...)`. The pre-Home prompt and the app grid gate
   on this combined signal, not on `isRoleHeld`.
4. **Prompt CTA targets the working path.** Because the role request is non-functional here, the pre-Home
   prompt's action opens `Settings.ACTION_ACCESSIBILITY_SETTINGS` (where the owner enables the override).
   Where `ROLE_HOME` is genuinely available, the first-party role request is still preferred.
5. **Reversibility & safety unchanged.** The stock launcher is never disabled; the override is turned off
   any time in Settings → Accessibility, restoring stock HOME. No system partition or default is mutated by
   the app.

## Alternatives

| Option (recommended **bold**) | Reversible? | Verdict |
|---|---|---|
| **Accessibility override (`NexaOverrideService`)** | Yes — owner enables/disables in Settings | **Chosen.** Validated on the TCL: HOME → NexaTV, stable (no loop), Back stays on NexaTV, grid populates. Stock launcher never disabled |
| `ROLE_HOME` role request (ADR-0006) | Yes | **Superseded here.** Grants the role record but not the effective HOME on this Google TV firmware; kept as the path where the role actually works |
| `cmd package set-home-activity` / set-preferred | — | **Rejected.** Returns Success but no effect; mutates system defaults; off-limits by device-safety |
| Disable stock launcher (`pm disable-user`) | Partially | **Rejected.** Would work but is explicitly forbidden (`initial.md` §16 — never disable the stock launcher) |
| Accept "secondary launcher" (open NexaTV manually) | Yes | **Fallback only.** No HOME-button integration; poor UX; keep if a future firmware breaks the override |

## Consequences

**Positive**
- NexaTV opens on the HOME button on firmware that otherwise blocks it, **without** disabling the stock
  launcher — stays within device-safety and least-privilege.
- The override is a consented, reversible accessibility toggle the owner controls; no system mutation.
- `isEffectivelyHome()` unifies both worlds (real role where it works, override where it doesn't), so the UI
  (prompt vs. grid) is correct on both.

**Negative / costs (honest)**
- **One manual setup step with weight:** the owner must enable an *accessibility* service, and the system
  shows a broad accessibility-consent warning. `NexaOverrideService` is deliberately minimal
  (`canRetrieveWindowContent=false`, only `typeWindowStateChanged`) and reads no screen content, but the
  scary consent dialog is unavoidable.
- **Firmware-fragile (INFERENCE):** a 2025 Google TV update reportedly broke this override on some firmware;
  it works on this TCL today but could regress on an OTA. The secondary-launcher fallback is the mitigation.
- **Brief visual transition:** HOME momentarily shows the stock launcher before NexaTV is raised. Observed as
  a quick transition on this device; a longer flash is possible on slower hardware.
- **Back behavior:** from NexaTV, Back re-triggers the stock launcher, which the override re-catches — so the
  user stays on NexaTV (desirable for a Home), but it spawns a fresh task instance rather than reusing the
  `singleTask` root. Minor; revisit if it causes task-stack growth.

## Rationale

The platform's safe, reversible mechanism (ADR-0006's role request) simply does not change the effective
HOME on this Google TV firmware — proven on-device, not assumed. The only within-safety way to still honor
the HOME button is to react to the stock launcher's appearance and raise NexaTV over it, which is exactly an
accessibility service's legitimate capability and is fully owner-consented and reversible. Disabling the
stock launcher would be simpler but is forbidden by the project's device-safety stance, so it is rejected
outright. Keeping `isEffectivelyHome()` as "role **or** override" means the same codebase does the right
thing on both Google TV (override) and any device where `ROLE_HOME` genuinely works (role).

## Follow-ups

- **Setup UX:** guide the owner from the pre-Home prompt to the exact Accessibility toggle (deep-link is not
  reliable on TV; today it opens the Accessibility list). Consider in-app instructions / a short walkthrough.
- **Resilience:** detect when the override is enabled but not firing (future-firmware regression) and fall
  back to clear secondary-launcher instructions rather than a dead HOME.
- **Back-stack hygiene:** confirm the fresh-task-on-Back behavior does not accumulate tasks over long use;
  tighten `launchMode`/flags if needed.
- **Validation record:** on-device results (bind-only-via-UI, HOME→NexaTV, grid populates, Back stays,
  stable no-loop) are logged in `docs/technical/ANDROID-TV.md` §3b/§3c (2026-10-20).
