> **Source:** Phase 0 research refresh, 2026-10-20 — second pass, from the actual repository.
> Claims are labelled FACT / INFERENCE / ASSUMPTION / UNKNOWN with source refs. Pending human review.
> Do not reuse any code until the license is confirmed (see [LICENSE_ANALYSIS.md](./LICENSE_ANALYSIS.md)).

# CleanYourTV — research note

Not in the first research pass; surfaced by the Oct-2026 landscape sweep as the only *new* credible,
**permissively licensed** entry. It is primarily a **debloat toolkit** that also bundles a deliberately
minimal home launcher — so it is a weak *launcher* candidate but a strong reference for our
**device-safety / HOME-swap workflow** and a rare permissive code donor.

## Identity

- **Repo:** `https://github.com/Gathin23/cleanyourtv` — `FACT`.
- **What it is:** ADB-over-Wi-Fi debloat toolkit (no root, one-line restore) **plus** a bundled
  "Apple-TV-style" native home launcher, package `com.gathin.tvhome` (~300 KB). `FACT` (README, per sweep agent).

## License — personally verified (reuse-critical)

- **Code: MIT.** `FACT` — the repo `LICENSE` file is standard MIT, `Copyright (c) 2026 Gathin`, no
  non-commercial / no-resell / extra-attribution clauses. Verified directly from
  `raw.githubusercontent.com/Gathin23/cleanyourtv/main/LICENSE`.
- **Why GitHub shows `NOASSERTION`/"Other":** the repo is **dual-licensed** — the bundled **Outfit**
  typeface is **SIL OFL-1.1**, which trips GitHub's single-license detector. `FACT` (API `license.spdx_id
  = NOASSERTION`; LICENSE file names both). The OFL only binds the *font*; **code reuse is governed by MIT**.
- **Reuse implication:** MIT is a clean permissive donor for a permissive-target NexaTV (retain the MIT
  notice; do **not** copy the Outfit font unless honoring OFL-1.1 separately). `INFERENCE`.

## Tech & TV specifics

- **Launcher stack:** plain **Java + AndroidX RecyclerView** — no Compose, no Leanback, no
  network/background services; intentionally bare. `FACT` (sweep agent, from README). Contrast with our
  Kotlin + Compose-for-TV (ADR-0002) stack.
- **Debloat tooling:** Python 3 (pure-Python ADB client) + PowerShell twins; claims a device matrix of
  ~66 models / 25 brands **including TCL**. `FACT` (README) — directly relevant to our TCL target and the
  "set HOME / survive reboot" problem.
- **D-pad/focus production quality on Android TV 14:** `UNKNOWN` — not verified on device; the launcher is
  minimal and may need focus-UX work.

## Maintenance (as of 2026-10-20)

- **Trending/active:** ~7.5k stars, ~866 forks, 466 commits; +4,442 stars 2026-10-17…19. `FACT` (sweep
  agent + Trendshift). Most-starred TV-launcher-adjacent repo of the month.
- Exact latest-commit timestamp: `UNKNOWN` (not re-verified this pass).

## Value for NexaTV

- **As a launcher:** limited — bare RecyclerView grid, Java, no Compose. Useful only as a *lightweight
  baseline* illustration. `INFERENCE`.
- **As a workflow reference:** high — its no-root ADB debloat, HOME-swap and reboot-survival approach on a
  TCL device maps onto our device-safety constraints (CLAUDE.md rule 3). Study the *approach*; any code
  reuse is MIT-clean with attribution. `INFERENCE`.

## Closed-source benchmarks (study UX only — never copy)

- **Projectivy Launcher** (`com.spocky.projengmenu`, projectivylauncher.com): **closed source**,
  proprietary/freemium. The `spocky/miproja1` repo holds only JSON device configs + issues, **no app
  source, no LICENSE**. Mature D-pad UX — a UX benchmark, **not** a code base. `FACT`.
- **Wolf Launcher** (`com.wolf.firelauncher`): **closed source**, APK-only, a fork of (closed) ATV
  Launcher; last updated ~Sept 2022, effectively abandoned; Fire-OS-oriented. Not reusable. `FACT`/`INFERENCE`.
- **tsynik/LeanbackLauncher**: code public but **no LICENSE** and **derived from decompiled Google
  Leanback Launcher** → reuse legally blocked (CLAUDE.md rule 5); behavioral reference only. `FACT`/`INFERENCE`.

## Sources

- `https://github.com/Gathin23/cleanyourtv` (README + `LICENSE` raw + `/license` API)
- `https://trendshift.io/repositories/310225`
- `https://projectivylauncher.com/`, `https://github.com/spocky/miproja1`
- `https://github.com/tsynik/LeanbackLauncher`
</content>
</invoke>
