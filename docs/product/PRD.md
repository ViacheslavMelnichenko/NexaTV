# NexaTV — Product Requirements Document (PRD)

> **Status:** ☐ Draft placeholder. To be authored **after** Phase 0 research and ADR-0001. Requirements
> use stable IDs (`FR-00x`, `NFR-00x`) that must not be renumbered once assigned.

## 1. Problem statement
_The stock Google TV / TCL launcher is [to be characterized from real use]. NexaTV aims to…_ **(TBD)**

## 2. Vision
A polished, fast, customizable, remote-first Android TV Home experience — offline-first and privacy-friendly.

## 3. Goals
- _TBD_

## 4. Non-goals
- _TBD_ (e.g., phone/tablet support, mandatory cloud accounts, advertising.)

## 5. Target environment
- Primary device: TCL Smart TV, Android TV OS 14 (see [ANDROID-TV.md](../technical/ANDROID-TV.md) for audited specs).
- Input: physical remote / D-pad (no touch, no mouse).

## 6. Personas (if useful)
- _TBD_

## 7. Functional requirements (FR)
> Derived from `initial.md` §1 capability list and the §13 MVP. Fill acceptance criteria per item.

| ID | Requirement | Priority | MVP? | Acceptance criteria | Status |
|----|-------------|----------|------|---------------------|--------|
| FR-001 | Launcher Activity registered as an Android HOME app | _TBD_ | Yes | _TBD_ | ☐ |
| FR-002 | Discover installed applications | _TBD_ | Yes | _TBD_ | ☐ |
| FR-003 | Launch an application | _TBD_ | Yes | _TBD_ | ☐ |
| FR-004 | Favorites | _TBD_ | Yes | _TBD_ | ☐ |
| FR-005 | Basic categories | _TBD_ | Yes | _TBD_ | ☐ |
| FR-006 | D-pad navigation & focus handling | _TBD_ | Yes | _TBD_ | ☐ |
| FR-007 | Simple settings | _TBD_ | Yes | _TBD_ | ☐ |
| FR-008 | Persistence of user configuration | _TBD_ | Yes | _TBD_ | ☐ |
| FR-009 | Reliable startup (incl. after reboot) | _TBD_ | Yes | _TBD_ | ☐ |
| FR-010+ | _Post-MVP capabilities (Continue Watching, weather, media/NAS/Home Assistant, screensaver, …)_ | _TBD_ | No | _TBD_ | ☐ |

## 8. Non-functional requirements (NFR)

| ID | Requirement | Target / measure | Status |
|----|-------------|------------------|--------|
| NFR-001 | Fast startup | _TBD_ (measure on real TCL hardware) | ☐ |
| NFR-002 | Low memory footprint | _TBD_ | ☐ |
| NFR-003 | Responsive during app discovery (no UI-thread blocking) | _TBD_ | ☐ |
| NFR-004 | Offline-first core functionality | _TBD_ | ☐ |
| NFR-005 | Reliable behavior after TV reboot | _TBD_ | ☐ |
| NFR-006 | Minimal/no advertising, privacy-friendly | _TBD_ | ☐ |
| NFR-007 | Reversible installation / safe fallback to stock launcher | _TBD_ | ☐ |

## 9. UX principles
Remote-first; focus always obvious and movement predictable; no mobile-first patterns; all major
functionality usable from the remote (Up/Down/Left/Right/OK/Back/Home, long-press where useful).

## 10. Constraints
_TBD_ (firmware behavior, licensing from any reused code, hardware limits.)

## 11. Assumptions
_TBD_

## 12. Risks
_TBD_ (HOME-app behavior differences across firmware, launcher-crash recovery, upstream license, performance.)

## 13. MVP
See FR-001..FR-009 above. Keep the MVP small; everything else is evaluated separately.

## 14. Future roadmap
_TBD — aligns with [MILESTONES.md](../../project/MILESTONES.md)._
