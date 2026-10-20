# ADR-0005: "Glass" / translucency technique

## Status

**Accepted** — 2026-10-20, **with an on-device measurement gate**. The *strategy* (default to pre-baked
"fake glass"; real-time backdrop blur is opt-in and gated) is accepted now because it does not depend on
unmeasured numbers. Whether any real-time blur is ever *enabled* is decided by the on-device budget in
§Follow-ups (same pattern as ADR-0002's validation spike). (Implements PRD v2 §16 / NFR-017, and is bound by
NFR-018 contrast and FR-011 focus.)

## Context

The owner's visual direction is **minimalist, elegant, pastel, with translucent "glass" surfaces** (PRD G7,
§16). Translucency is the single most performance-sensitive aesthetic choice on this hardware.

- **FACT (device):** the GPU is an **older 32-bit class** part (exact model `UNKNOWN`); the launcher is
  **always-resident** on ~2.34 GiB (NFR-002) and must hit cold-start/idle budgets (NFR-001/003).
- **FACT (API):** real-time backdrop blur (`RenderEffect.createBlurEffect`, Compose
  `Modifier.graphicsLayer`/`blur`) requires **API 31+**; the device is **API 34**, so it is *available*.
  Availability ≠ affordability on this GPU.
- **INFERENCE:** per-frame backdrop blur cost scales with blurred **area × radius** and typically needs an
  **offscreen buffer** → raises GPU load *and* memory every frame it is on screen; on a weak 32-bit GPU this
  risks dropped frames during D-pad scroll/focus and regressions against NFR-001/002/003.
- **INFERENCE:** **"fake glass"** — a semi-transparent surface token over a *static* (optionally
  pre-blurred) background layer — costs essentially the same as drawing any translucent surface
  (negligible), and on a mostly-static launcher backdrop is visually close to real blur.
- **FACT (legibility):** pastel + translucency is inherently **low-contrast**; text/focus over a dynamic
  translucent backdrop can fall below the 10-foot legibility floor → bound by NFR-018 (contrast ≥4.5:1 body /
  ≥3:1 large & focus-ring over the *worst real* backdrop, opaque scrim under text) and FR-011 (focus never
  carried by hue/translucency alone).

## Decision

1. **Default technique = "fake glass" (pre-baked / alpha).** Translucent surfaces are built from
   semi-transparent **design tokens** (NFR-016) layered over a **static background** (a flat pastel gradient
   and/or a **pre-blurred static asset**). **No per-frame backdrop blur** of live content in the default UI.
2. **Real-time backdrop blur is opt-in, gated, and OFF by default.** It may be enabled on **at most one
   small, non-scrolling surface** (e.g. a focused detail/overlay panel), behind a **feature flag**, and
   **only if** the on-device measurement gate (§Follow-ups) shows sustained **≥58 fps** (frame ≤16.6 ms)
   during scroll/focus **and** no regression against the NFR-001/002 budgets versus an opaque baseline.
   If it fails the gate, the flag stays off and the fake-glass fallback is used.
3. **Legibility is non-negotiable and independent of technique:** every translucent surface bearing text or
   essential icons satisfies **NFR-018** (contrast floor + opaque scrim); focus satisfies **FR-011**
   (≥2 non-hue signals). Translucency is decoration, never the carrier of information or focus.
4. **Scope limit:** translucency is applied to a small number of surface roles (e.g. top bar / side rail /
   overlay), not to every tile — both for performance and for the "restrained, elegant" aesthetic.

## Alternatives

| Technique (recommended **bold**) | Authenticity | Cost on this GPU | Verdict |
|---|---|---|---|
| Opaque pastel surfaces only | None (no glass) | Cheapest | Fallback baseline; misses the owner's "glass" intent |
| **Fake glass — alpha token over static (pre-blurred) background** | High on a static backdrop | Negligible (≈ normal translucent draw) | **Chosen default.** Gets ~the look with ~none of the risk |
| Real-time backdrop blur (`RenderEffect`) of live content | Highest | High per-frame GPU + offscreen memory; risks NFR-001/002/003 | **Gated, off by default**, ≤1 small non-scrolling surface, only if it passes the on-device budget |

## Consequences

**Positive**
- Delivers the elegant translucent aesthetic with **negligible, predictable** cost in the default path →
  protects NFR-001/002/003 on weak hardware.
- Real-time blur remains **possible** where it adds the most value (a single focal overlay), but only when
  *proven* affordable — no speculative perf debt.
- Contrast/focus guarantees (NFR-018/FR-011) hold regardless of which technique renders a surface.

**Negative / costs (honest)**
- Fake glass over a **changing** backdrop (e.g. video/art behind the panel) looks less authentic than true
  blur — acceptable because the launcher backdrop is mostly static; revisit only if a dynamic backdrop
  becomes a design requirement.
- A **pre-blurred static asset** adds a small APK-size/memory cost (one or few images) — bounded by the
  NFR-014 art/memory discipline and the APK-size budget (NFR-013).
- The real-time-blur gate adds a **measurement step** before that feature can ship (acceptable; it is a flag,
  not core).

## Rationale

On a weak, always-resident 32-bit device, the cheapest way to get ~the entire visual benefit is to exploit
that a launcher backdrop is **mostly static**: a pre-blurred/alpha "fake glass" is visually close to real
blur at a fraction of the per-frame cost, so it is the right default. Real-time blur is not banned — it is
simply made **earn its place** through an on-device budget, confined to one small non-scrolling surface where
it matters most. Legibility is deliberately decoupled from the aesthetic (NFR-018/FR-011) so that no
translucency choice can ever make the 10-foot UI unreadable — the elegant look never costs usability. The
strategy is accepted now because it rests on structure (static backdrop, cost model), not on numbers;
the numbers only decide whether the optional blur flag is ever switched on.

## Follow-ups

- **On-device measurement gate (part of the ADR-0002 spike / NFR-017 acceptance):** on the real TCL, compare
  the chosen fake-glass surface and a candidate real-time-blur surface against an opaque baseline via
  Macrobenchmark — record sustained fps during scroll/focus, idle PSS, and cold-start delta. Enable the
  blur feature flag **only** if it clears §Decision point 2.
- **Capture the worst-case real translucent backdrop** as a frame grab and run the NFR-018 contrast check
  against it (not against a flat mock) before locking the pastel palette tokens (NFR-016).
- Record chosen alpha values, scrim opacity, and (if used) the pre-blur radius/asset in the design-system
  documentation once measured.
