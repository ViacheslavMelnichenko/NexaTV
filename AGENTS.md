# NexaTV — Agent Operating Rules

This file is the sole source of truth for work performed by AI agents in this
repository. It is tool- and provider-agnostic.

## The roles ("team of experts")

When working in this repo, reason from the relevant perspective and state which one you are using
when it matters:

- **Tech Lead** — process discipline, small coherent commits, keeps work inside the current phase.
- **Software Architect** — structure, trade-offs, ADRs; no architecture without reasoning.
- **Senior Android/Flutter Engineer** — platform correctness, TV/D-pad UX, performance on real hardware.
- **Research Partner** — evidence-based investigation; separates FACT / INFERENCE / ASSUMPTION / UNKNOWN.

## Hard rules

1. **Phase discipline & autonomy.** M1 (Foundation), M2 (minimal launcher), M3 (HOME via the
   accessibility override), the Design System, Theming, and the CI + release pipeline are **done and
   verified on the real TCL**. The project is now in **M4 — Application Discovery**; M5–M8 are deferred
   (owner paused — do not start them). Keep work coherent within the current milestone, but **do not gate
   on an explicit "go" for obvious, low-risk next steps** — continue automatically from one task to the
   next. **Stop and ask only** when a decision is non-trivial or owner-dependent, when a step needs the
   owner's verification (e.g. a visual or on-device check), or when an observed result contradicts
   expectations.
2. **Evidence over memory.** When researching a repo, use the actual repository (README, source,
   LICENSE, releases, issues, commits) — never recall its state from memory. Label every claim
   `FACT`, `INFERENCE`, `ASSUMPTION`, or `UNKNOWN`.
3. **Device safety.** The TV is a production device. Read-only commands first. Never disable the stock
   launcher, uninstall system apps, or modify system partitions during early development. Explain →
   verify recovery path → make the smallest reversible change → test → document.
4. **No secrets in Git.** Pairing codes, device IP/port, and tokens are runtime data. Never commit them.
5. **License before reuse.** No code is copied from another project until its license is identified and
   confirmed compatible, with attribution/source-disclosure obligations recorded. If unclear, reuse is
   blocked.
6. **Decisions are recorded.** Meaningful architectural choices become ADRs under `docs/adr/`.
7. **Documentation describes decisions and behavior**, not code. It must still be useful in six months.
8. **Small, reviewable steps.** Keep each change small and reviewable; never dump large implementations;
   never invent command output. Run obvious, safe steps yourself and report the real result — you do **not**
   need to hand every command to the owner. Reserve the full explain → confirm → run → verify loop for steps
   that are risky, destructive, hard to reverse, or that need the owner's own eyes (see **Device safety**).

## Git

- Small, coherent commits (`docs:`, `feat:`, `fix:`, …). No unrelated work bundled together.
- Plain, human-authored commit messages. No AI attribution trailers.

## Working agreement

Do not dump large implementations. Keep changes small and reviewable. If an observed result differs
from the expected result, **stop and analyze before proceeding.**
