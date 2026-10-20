# NexaTV — Milestones

> Provisional. Change when research or architecture indicates a better plan (`initial.md` §24).
> Status legend: ☐ Not started · ◐ In progress · ☑ Done.

| ID | Milestone | Goal | Status |
|----|-----------|------|--------|
| **M0** | Discovery & Device Audit | ADB connected, device audited (read-only), ecosystem researched, feature matrix + license + architecture comparison, ADR-0001 decided | ☑ Done (2026-10-20) |
| M1 | Architecture & Project Foundation | Technology/architecture decided (ADRs), code scaffold created | ☑ Done (2026-10-20) — 5 ADRs accepted, Apache-2.0 chosen, Kotlin + Compose-for-TV scaffold builds (`assembleDebug`) |
| M2 | Minimal Launcher | Launcher Activity, D-pad focus, app list render | ◐ In progress (2026-10-20) — code complete (grid renders/launches via `LauncherApps`, D-pad focus, states); on-device verification pending |
| M3 | HOME Integration | Register as HOME (reversibly), verify behavior on real TV | ☐ |
| M4 | Application Discovery | Robust installed-app discovery, launching, favorites, basic categories | ☐ |
| M5 | Production TV UX | Polished focus/navigation/animations, remote-first UX | ☐ |
| M6 | Configuration & Customization | Settings, layout, backgrounds, persistence | ☐ |
| M7 | Media / NAS Integrations | Jellyfin/Plex/Kodi, NAS, Continue Watching | ☐ |
| M8 | Advanced Integrations | Home Assistant, weather, notifications, screensaver | ☐ |

## M0 — exit criteria (current milestone)

- [x] Working ADB connection to the TCL (wireless, paired + connected 2026-10-20)
- [x] Device audit recorded in `docs/technical/ANDROID-TV.md` (identity, CPU/RAM, display, HOME activities, app counts)
- [x] Repository initialized
- [x] Initial documentation structure
- [x] Initial research notes populated (6 launchers researched from real repos; matrix + license + architecture docs written 2026-10-20)
- [x] Initial backlog
- [x] M0 milestone defined
- [x] ADR-0001 (Build vs. Fork) decided → greenfield, permissive license, clean-room references

**M0 complete.** **M1 complete (2026-10-20).** Next milestone **M2 — Minimal Launcher**.

## M1 — exit criteria (met 2026-10-20)

- [x] All architecture ADRs accepted (ADR-0001…0005)
- [x] Permissive license chosen and applied (Apache-2.0; `LICENSE` + `NOTICE` at root)
- [x] Code scaffold created: root Gradle + version catalog + wrapper (8.9), `:app`
      module (`com.nexatv.launcher`, SDK 34, `versionCode` 100 / `0.1.0`)
- [x] Manifest honors FR-001 (HOME + LEANBACK_LAUNCHER) and NFR-021 (no INTERNET,
      `allowBackup=false`, data-extraction rules); design tokens + en/uk strings
- [x] Builds locally (`assembleDebug`, JDK 17 / SDK 34); placeholder Home only

> Per `initial.md` §32: do **not** install NexaTV during M0–M1. First on-device
> install happens in M2/M3 with a verified recovery path.

## M2 — exit criteria (in progress)

- [x] Launcher Activity renders a D-pad-navigable app grid (`ui/home/HomeScreen` on `AppTile`)
- [x] App discovery via `LauncherApps.getActivityList` across profiles (ADR-0004), off the UI thread
- [x] Launch on OK; Loading/Empty states + in-UI launch-error (FR-003/FR-006/FR-011/FR-012/FR-014)
- [x] `assembleDebug` + `lintDebug` + `detekt` green
- [ ] **On-device (owner):** first install with verified recovery path; D-pad navigation + launch confirmed
      on the real TCL remote; FR-011 focus check from ~3 m. (Pre-`ROLE_HOME` the grid may be partial — M3.)
