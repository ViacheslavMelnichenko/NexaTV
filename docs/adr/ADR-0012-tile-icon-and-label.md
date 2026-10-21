# ADR-0012: Tiles revert to a square icon over a label (supersedes the 16:9 banner)

## Status

**Accepted** — 2026-10-21. Supersedes [ADR-0011](./ADR-0011-tile-art-banner-first.md) (uniform 16:9 TV
banners). Restores the original FR-013 tile — a square app icon over a one/two-line label — and resets the
S/M/L tokens from [ADR-0009](./ADR-0009-configuration-architecture.md) back to portrait.

## Context

ADR-0011 made every tile a landscape 16:9 card sourced from each app's `android:banner` (icon-on-card
fallback). It was built and verified on the TCL.

- **FACT (on-device):** the owner reviewed the banner grid on the real TV and rejected it — the wide
  landscape plates dominated the row and read as "wide tiles", not the compact app shelf wanted. The owner's
  instruction: *"зроби плитки меншими іконками, як вони були раніше, Іконка і підпис"* (make the tiles smaller
  icons as they were before — icon and label).
- **FACT:** the earlier square-icon tile (pre-ADR-0011, the FR-013 shape) was the preferred look.
- **FACT:** four LARGE banner tiles plus gaps overflowed the 832 dp usable favorites row, clipping the
  Settings tile off the right edge — a second, concrete problem with the wide shape.
- **CONSTRAINT (Hard Rule 6):** a reversal of an Accepted decision is itself recorded as an ADR; ADR-0011 is
  not edited away, it is superseded.

So the decision is simply to **undo ADR-0011 and return to the square icon + label tile**, and to record that
the banner experiment was tried, measured on the device, and rejected — so it is not re-attempted blind.

## Decision

1. **Revert the tile to a square icon over a label (FR-013).** `AppTile` drops the inner 16:9 art card and
   `ContentScale.Crop`; it draws the app icon `Fit` in a square (`aspectRatio(1f)`) over a one/two-line,
   soft-wrapping label. The focus glow/scale/border are unchanged.
2. **Remove the banner data path entirely.** `AppEntry.banner` and `InstalledAppsRepository.loadBanner`
   (plus the `BANNER_W_PX`/`BANNER_H_PX` constants) are deleted — no banner lookup or raster at enumeration
   time. This is a clean removal, not a dormant field, so there is no dead code to mislead a later reader.
3. **Reset the S/M/L tokens to portrait.** `TileSizes` returns to portrait dimensions (80×100 / 96×120 /
   128×156 dp) so four LARGE favorites fit the 832 dp usable row with room to spare.

## Alternatives

| Decision point | Options (recommended **bold**) | Verdict |
|---|---|---|
| Tile shape | **Square icon + label (revert)** · keep 16:9 banners · masked square icons from the banner | **Chosen (owner, on-device):** the square icon+label is the look the owner confirmed. Banners were rejected on the real TV; masking the banner into a square adds complexity for art the owner didn't want |
| Banner code | **Delete it** · keep `AppEntry.banner` dormant behind a flag | **Chosen:** keeping an unused banner field + loader is dead code that misleads; removal is reversible via git history and this ADR if banners ever return |
| Token shape | **Portrait (icon over label)** · keep landscape tokens | **Chosen:** portrait matches an icon-over-label tile and lets all four favorites fit the row; landscape tokens were sized for 16:9 art that no longer exists |

## Consequences

**Positive**
- Tiles are the compact icon+label the owner wanted; all four favorites fit the row with no clipping.
- One fewer `PackageManager` lookup + raster per app at enumeration — slightly cheaper, less memory.
- No banner assets, no new dependency, no licence exposure (Hard Rule 5 was never breached either way).

**Negative / costs (honest)**
- Branded TV apps no longer show their landscape banner art — they show the (adaptive) launcher icon, which
  for some apps renders on a white plate (the exact complaint ADR-0011 set out to fix). The owner accepts
  this trade for the compact shape; normalising/masking adaptive icons is a possible future follow-up.
- Reversing an Accepted ADR one day later is churn; recorded honestly here so the reasoning survives.

## Rationale

The banner grid was a reasonable hypothesis — Android TV ships branded banner art and using it avoided
bundling icons — but the only test that mattered was the owner's eyes on the real TV, and there it failed:
the wide plates weren't the launcher the owner wanted, and they overflowed the favorites row. The right
response is to revert cleanly to the FR-013 square tile, delete the banner path rather than leave it dormant,
and record the reversal so the experiment isn't silently repeated. The unmasked-adaptive-icon look (white
plate on some apps) is a known, accepted cost; improving it is deferred to an optional icon-normalisation
follow-up, independent of tile shape.

## Follow-ups

- **Code (this change):** revert `AppTile`, delete `AppEntry.banner` and `InstalledAppsRepository.loadBanner`,
  reset the `TileSizes` tokens to portrait, drop the `banner =` call sites in `HomeScreen`/`AllAppsScreen`.
- **Later (optional):** normalise/mask adaptive launcher icons so icon-only tiles look clean on apps whose
  icon has a white background layer — the original ADR-0011 motivation, solvable without banners.
