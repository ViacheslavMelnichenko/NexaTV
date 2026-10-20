# NexaTV — Milestones

> Provisional. Change when research or architecture indicates a better plan (`initial.md` §24).
> Status legend: ☐ Not started · ◐ In progress · ☑ Done.

| ID | Milestone | Goal | Status |
|----|-----------|------|--------|
| **M0** | Discovery & Device Audit | ADB connected, device audited (read-only), ecosystem researched, feature matrix + license + architecture comparison, ADR-0001 decided | ◐ In progress |
| M1 | Architecture & Project Foundation | Technology/architecture decided (ADRs), code scaffold created | ☐ |
| M2 | Minimal Launcher | Launcher Activity, D-pad focus, app list render | ☐ |
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
- [ ] ADR-0001 (Build vs. Fork) decided ← **next**

> Per `initial.md` §32: do **not** install NexaTV during M0.
