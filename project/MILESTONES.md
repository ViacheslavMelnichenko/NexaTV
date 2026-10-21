# NexaTV — Milestones

> Status legend: ☑ Done · ⏸ Deferred (owner paused — not on the current agenda).
> Every item below is one of the two. There are no not-started or in-progress markers.

| ID | Milestone | Goal | Status |
|----|-----------|------|--------|
| **M0** | Discovery & Device Audit | ADB connected, device audited (read-only), ecosystem researched, feature matrix + license + architecture comparison, ADR-0001 decided | ☑ Done (2026-10-20) |
| M1 | Architecture & Project Foundation | Technology/architecture decided (ADRs), code scaffold created | ☑ Done (2026-10-20) — 5 ADRs accepted, Apache-2.0 chosen, Kotlin + Compose-for-TV scaffold builds (`assembleDebug`) |
| M2 | Minimal Launcher | Launcher Activity, D-pad focus, app list render + launch | ☑ Done (2026-10-20) — grid renders/launches via `LauncherApps` (ADR-0004), D-pad focus + Loading/Empty/error states; **verified on the real TCL** (reversible install, one-cell D-pad, OK launches focused app, HOME returns to stock) |
| M3 | HOME Integration | Register as HOME (reversibly), verify behavior on real TV | ☑ Done (2026-10-20) — `ROLE_HOME` replacement is blocked by firmware on this Google TV (ADR-0006), so HOME is achieved via a reversible **accessibility override** (ADR-0007); stock launcher never disabled; **verified on the real TCL** |
| M4 | Application Discovery | Robust installed-app discovery, launching, favorites, basic categories | ☑ Done (core) — live add/remove, persistence foundation (ADR-0008), All-apps screen, favorites UI, home widgets. "Basic categories" is ⏸ Deferred (see below). |
| M5 | Production TV UX | Polished focus/navigation/animations, remote-first UX | ⏸ Deferred |
| M6 | Configuration & Customization | Settings, layout, backgrounds, persistence | ⏸ Deferred |
| M7 | Media / NAS Integrations | Jellyfin/Plex/Kodi, NAS, Continue Watching | ⏸ Deferred |
| M8 | Advanced Integrations | Home Assistant, weather, notifications, screensaver | ⏸ Deferred |

## M0 — exit criteria (met 2026-10-20)

- ☑ Working ADB connection to the TCL (wireless, paired + connected 2026-10-20)
- ☑ Device audit recorded in `docs/technical/ANDROID-TV.md` (identity, CPU/RAM, display, HOME activities, app counts)
- ☑ Repository initialized; initial documentation structure
- ☑ Open-source launcher research completed (6 launchers researched from their real repos; feature matrix + license + architecture comparison written 2026-10-20)
- ☑ Initial backlog + M0 milestone defined
- ☑ ADR-0001 (Build vs. Fork) decided → greenfield, permissive license, clean-room references

## M1 — exit criteria (met 2026-10-20)

- ☑ All architecture ADRs accepted (ADR-0001…0005)
- ☑ Permissive license chosen and applied (Apache-2.0; `LICENSE` + `NOTICE` at root)
- ☑ Code scaffold created: root Gradle + version catalog + wrapper (8.9), `:app`
      module (`com.nexatv.launcher`, SDK 34, `versionCode` 100 / `0.1.0`)
- ☑ Manifest honors FR-001 (HOME + LEANBACK_LAUNCHER) and NFR-021 (no INTERNET,
      `allowBackup=false`, data-extraction rules); design tokens + en/uk strings
- ☑ Builds locally (`assembleDebug`, JDK 17 / SDK 34)

## M2 — exit criteria (met 2026-10-20)

- ☑ Launcher Activity renders a D-pad-navigable app grid (`ui/home/HomeScreen` on `AppTile`)
- ☑ App discovery via `LauncherApps.getActivityList` across profiles (ADR-0004), off the UI thread
- ☑ Launch on OK; Loading/Empty states + in-UI launch-error (FR-003/FR-006/FR-011/FR-012/FR-014)
- ☑ `assembleDebug` + `lintDebug` + `detekt` green
- ☑ **On-device (TCL):** first reversible install; D-pad moves one cell/press, OK launches the
      focused app, focus ring visible, `HOME` returns to the stock launcher. See `docs/technical/ANDROID-TV.md` §3a.

## M3 — exit criteria (met 2026-10-20)

- ☑ On-device validation of the `ROLE_HOME` path (ADR-0006): the role-request dialog fires and updates the
      role record, **but** the effective HOME stays stock Google TV — a documented firmware block, confirmed
      across reboot. Recorded in `docs/technical/ANDROID-TV.md` §3b.
- ☑ Within-safety HOME integration via a reversible **accessibility override** (`NexaOverrideService`,
      ADR-0007): owner enables it in Settings → Accessibility; HOME then raises NexaTV over the stock launcher.
      Stock launcher never disabled; reversible by toggling the service off.
- ☑ **On-device (TCL):** HOME → NexaTV (repeatable, stable, no loop), Back stays on NexaTV, and the app grid
      populates in override mode. See `docs/technical/ANDROID-TV.md` §3c.
- ☑ Firmware-independent detection: `PackageManager.resolveActivity(HOME)` + enabled-accessibility-service
      check, not the unreliable `RoleManager` API.
- ☑ Unit tests green for the state machine (prompt vs. grid gating); `assembleDebug` green.

## M4 — status

- ☑ Live app add/remove via `LauncherApps.Callback`; persistence (ADR-0008 + Proto DataStore foundation,
      `FavoritesRepository`); "All apps" screen; home widgets (calendar + weather, ADR-0010); favorites UI.
- ⏸ **Basic categories** — Deferred (owner paused; not on the current agenda).

## Deferred follow-ups (not on the agenda)

- ⏸ ADR-0007 follow-ups: accessibility-consent onboarding; override firmware-regression (OTA) fallback.
- ⏸ NFR-017 glass fps/PSS gate.
- ⏸ Milestones M5–M8.

## Owner action before v0.1.0

The release pipeline (`.github/workflows/release.yml`, ADR-0003) is Done and was validated live end-to-end
with the real signing key (throwaway `v0.0.1-rc1`, since cleaned up). No public release is cut yet. The single
remaining pre-release action is **owner-driven: run an on-device smoke test on the TCL, then tag `v0.1.0`.**
