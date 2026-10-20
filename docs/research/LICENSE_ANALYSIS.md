> **Source:** Phase 0 research, 2026-10-20 — produced by parallel research agents working from the
> actual repositories. Claims are labelled FACT / INFERENCE / ASSUMPTION / UNKNOWN with source refs.
> Pending human review. Do not reuse any code until the license is confirmed (see LICENSE_ANALYSIS.md).

# NexaTV License Analysis

Scope note: This analysis assumes NexaTV is (or intends to be) an independent, permissively licensed project (MIT/Apache-2.0), consistent with the compatibility framing throughout the research corpus. "Compatible with NexaTV?" below is judged against that permissive-target assumption. Where the corpus is explicitly unsure, the value is marked `UNKNOWN`. No license types have been invented; only license identifiers that appear in the corpus are used.

## 1. Summary Table

| Project | License | Commercial use | Fork OK | Source disclosure | Attribution | Compatible with NexaTV? | Source ref |
|---|---|---|---|---|---|---|---|
| FLauncher | GPL-3.0 (GPL-3.0-only) | Permitted (derivatives must stay GPL-3.0) | Yes (derivatives must be GPL-3.0; keep notices + modification dates) | Required (strong copyleft; must provide Corresponding Source, incl. Installation Information) | Required (retain copyright/license notices; state modifications w/ dates) | No (GPL-3.0 code cannot enter a permissive codebase without relicensing the whole work) | `/LICENSE` file in repo (FACT) |
| LTvLauncher + LTvLauncher Extended | GPL-3.0-only (Extended inherits GPL-3.0) | Permitted (GPLv3 allows commercial use/sale) | Yes (itself a fork chain; derivatives must stay GPL-3.0 + ship full source) | Required (strong copyleft) | Required (preserve notices; repos credit etienn01 / osrosal / LeanBitLab / hamishakl via ATTRIBUTION.md) | No (copying/forking forces whole derivative under GPL-3.0; incompatible with permissive NexaTV) | `LICENSE` file in base repo; Extended README (FACT) |
| Yukuza Launcher | MIT (declared in README only; **no LICENSE file**, GitHub API license field = null) | Permitted (no royalty) | Yes (incl. closed-source redistribution) | Not required (permissive, no copyleft) | Required (retain copyright notice + MIT permission text) | Yes, *if* the MIT grant is honored — **RISK**: missing LICENSE file makes the grant ambiguous; obtain a proper LICENSE / written confirmation before reuse | README line "MIT License — Copyright (c) 2025 Yukuza" (FACT); no standalone LICENSE (FACT) |
| Tarang Launcher | None (no license file; all rights reserved by default) | Not permitted without explicit author permission (INFERENCE) | No beyond GitHub ToS view/fork; no redistribution/modification rights granted | Not applicable (no license to impose terms) | None specified (default copyright = author holds all rights) | No (code/assets cannot be lawfully copied into a permissive derivative; must reimplement independently or ask maintainer to add a permissive license) | GitHub `/license` API returns 404; no LICENSE/COPYING; no SPDX headers (FACT) |
| nielsvanvelzen/tv-launcher | GPL-3.0 | Permitted (strong copyleft applies) | Yes (derivative/combined works must be GPL-3.0 + source available) | Required (must offer corresponding source under GPL-3.0) | Required (preserve copyright/license notices) | No (reuse would force NexaTV to GPL-3.0 + source disclosure; reference/clean-room only) | `LICENSE` file + API `license.spdx_id = GPL-3.0` (FACT) |
| karuhun-developer/LauncherCompose | Apache-2.0 | Permitted (minimal obligations; proprietary use OK) | Yes (permissive redistribution, incl. in proprietary products) | Not required | Required (retain NOTICE/attribution; state changes) | Yes (Apache-2.0 code can be reused in an MIT/Apache NexaTV with attribution) — but hotel/IPTV scope limits usefulness | `LICENSE` file + README (FACT) |

## 2. Implications for NexaTV's Own License (Permissive vs. Copyleft Tensions)

The core tension is directional. Apache-2.0 code can flow **into** a permissive (MIT/Apache) NexaTV, and Apache-2.0 can also flow into a GPL-3.0 project — but GPL-3.0 code cannot flow into a permissive project without dragging the entire combined work under GPL-3.0 (with mandatory source disclosure). This asymmetry drives every decision below.

- **Most of the corpus is off-limits for code reuse under a permissive NexaTV.** Four of the six analyzed codebases are GPL-3.0 (FLauncher, LTvLauncher, LTvLauncher Extended, nielsvanvelzen/tv-launcher). These are the most feature-rich and mature references (Continue Watching, TV inputs, notifications, screensavers, weather, Watch Next, etc.), but incorporating any of their code would force NexaTV itself to become GPL-3.0 and to disclose Corresponding Source for any distributed binary. For a permissive NexaTV, these must be treated as **architecture/idea references only (clean-room)** — study patterns, do not copy code, assets, or generated files.

- **If NexaTV is willing to be GPL-3.0, the calculus flips.** Adopting GPL-3.0 unlocks direct reuse/forking of the richest implementations (the FLauncher → osrosal → LeanBitLab → niposch chain, plus nielsvanvelzen/tv-launcher), and Apache-2.0 LauncherCompose code could still be pulled in. The cost is strong copyleft: every distributed derivative must ship full source under GPL-3.0, carry anti-DRM Installation Information (per the FLauncher corpus), and preserve attribution chains. This is the fastest path to feature parity but forecloses any proprietary/closed distribution.

- **Only two sources are safe for permissive reuse**, and both are weak as code donors:
  - *LauncherCompose (Apache-2.0)* is cleanly reusable with attribution (retain NOTICE, state changes), but its scope is hotel/IPTV guest UX with a backend + Firebase dependency — limited direct value for a general consumer launcher.
  - *Yukuza (MIT, as declared)* is the most technically aligned permissive candidate (Kotlin + Compose for TV, clean architecture), **but the MIT grant exists only as a README line** — no LICENSE file and GitHub reports no license. Until a proper LICENSE file or written confirmation is obtained, treat the grant as legally ambiguous and do not reuse code in reliance on it.

- **Tarang is fully closed by default.** With no license, default copyright reserves all rights; it is usable neither under a permissive nor a GPL-3.0 NexaTV. Reuse requires either independent reimplementation or the maintainer adding a license.

- **Attribution hygiene matters even for permissive sources.** Both viable options (Apache-2.0 LauncherCompose, MIT Yukuza) require preserving copyright/attribution notices; Apache-2.0 additionally requires stating changes and retaining any NOTICE file. NexaTV should establish a NOTICE/THIRD-PARTY attribution file early.

- **Recommendation summary (permissive-NexaTV assumption):** Prefer clean-room learning from the GPL-3.0 projects; reuse Apache-2.0 LauncherCompose code where its functionality fits; gate any Yukuza (MIT) reuse behind license clarification; exclude Tarang entirely. If deep code reuse from the GPL-3.0 feature leaders is a hard requirement, that is a decision to license NexaTV itself as GPL-3.0 — and should be made deliberately, not incrementally.

## 3. Reuse Clearance Log

Track each concrete reuse decision here before merging any third-party code or assets.

| Date | Component / file(s) | Source project | Source license | Reuse type (code / asset / clean-room idea) | Target NexaTV license | Compatible? | Attribution added? | Decision (approved / blocked / pending) | Reviewer | Notes |
|---|---|---|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |  |  |  |
|  |  |  |  |  |  |  |  |  |  |  |
|  |  |  |  |  |  |  |  |  |  |  |
|  |  |  |  |  |  |  |  |  |  |  |
