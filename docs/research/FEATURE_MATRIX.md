> **Source:** Phase 0 research, 2026-10-20 — produced by parallel research agents working from the
> actual repositories. Claims are labelled FACT / INFERENCE / ASSUMPTION / UNKNOWN with source refs.
> Pending human review. Do not reuse any code until the license is confirmed (see LICENSE_ANALYSIS.md).

## NexaTV Feature Matrix

**Status note:** Values below are transcribed verbatim from each project's `featureMatrix` in the research corpus (observed 2026-10-20). No values were inferred or invented. `LauncherCompose` has no standalone `featureMatrix` in the corpus — its cells are `Unknown` except for the handful of features explicitly stated in the shared `tv-launcher` entry's evidence (App discovery, Weather, Screensaver, Offline-first, NAS, Home Assistant). The `NexaTV (target)` column is intentionally left blank (`—`) pending design decisions.

**Legend:** `Yes` = present · `No` = absent · `Partial` = partially present / limited / unverified sub-claim · `Unknown` = no entry in that project's corpus data · `—` = target column, not yet decided.

| Feature | FLauncher | LTvLauncher | Yukuza | Tarang | TV Launcher | LauncherCompose | NexaTV (target) |
|---|---|---|---|---|---|---|---|
| App discovery | Yes | Yes | Yes | Yes | Yes | Yes | — |
| Categories | Yes | Yes | No | No | No | Unknown | — |
| Favorites | Partial | Unknown | Partial | Yes | Yes | Unknown | — |
| Reordering | Yes | Partial | Yes | Yes | Yes | Unknown | — |
| D-pad navigation | Yes | Yes | Yes | Yes | Yes | Unknown | — |
| Focus handling | Yes | Yes | Yes | Yes | Yes | Unknown | — |
| Recent apps | No | Unknown | No | No | No | Unknown | — |
| Continue Watching | No | Yes | No | Partial | Partial | Unknown | — |
| Weather | No | Yes | Yes | Yes | No | Yes | — |
| Clock | Yes | Yes | Yes | Yes | Yes | Unknown | — |
| HDMI / input shortcuts | No | Yes | Partial | No | Yes | Unknown | — |
| System settings shortcuts | Yes | Yes | Yes | Yes | Yes | Unknown | — |
| Screensaver (OLED-friendly) | Partial | Yes | Partial | Partial | No | Partial | — |
| Notifications | No | Yes | Partial | No | No | Unknown | — |
| Media playback info | No | Unknown | Yes | Yes | No | Unknown | — |
| Media integration (Jellyfin/Plex/Kodi) | No | Partial | No | No | No | Unknown | — |
| NAS integration | No | No | No | No | No | No | — |
| Home Assistant integration | No | No | No | No | No | No | — |
| Custom backgrounds | Yes | Yes | No | Yes | No | Unknown | — |
| Customizable layout | Yes | Partial | Partial | Partial | Partial | Unknown | — |
| Animations | Yes | Yes | Yes | Yes | Partial | Unknown | — |
| Configuration / settings | Yes | Yes | Yes | Yes | Partial | Unknown | — |
| Offline-first core | Partial | Yes | Partial | Partial | Yes | No | — |
| Fast startup | Unknown | Partial | Yes | Yes | Unknown | Unknown | — |
| Low resource usage | Unknown | Partial | Partial | Yes | Unknown | Unknown | — |
| Minimal / no advertising | Yes | Yes | Yes | Yes | Yes | Unknown | — |
| TV reboot reliability | Unknown | Partial | Partial | Partial | Partial | Unknown | — |
