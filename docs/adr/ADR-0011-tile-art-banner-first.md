# ADR-0011: Uniform 16:9 tile art — TV banner first, icon on a card otherwise

## Status

**Accepted** — 2026-10-20. Refines the FR-013 tile ("square art over a label") into a landscape 16:9 card.
Builds on [ADR-0004](./ADR-0004-app-enumeration-visibility.md) (the `<queries>` launcher-intent visibility
that also exposes banners) and resizes the S/M/L tokens from [ADR-0009](./ADR-0009-configuration-architecture.md).

## Context

The owner flagged that app tiles looked poor — e.g. YouTube rendered as a red play button on a **white
square** rather than its recognisable branded art.

- **FACT:** tiles drew a single square `ImageBitmap` from `LauncherActivityInfo.getBadgedIcon()` /
  `ResolveInfo.loadIcon()`, scaled to fit. For an app whose launcher icon is an `AdaptiveIconDrawable`
  with a white background layer (YouTube), an unmasked square render shows that white plate.
- **FACT:** Android TV apps declare `android:banner` — a **16:9 branded landscape image** — which Google
  TV uses in its rows. It is reachable via `PackageManager.getActivityBanner()` /
  `getApplicationBanner()`, already visible to NexaTV through the ADR-0004 `<queries>`.
- **CONSTRAINT (Hard Rule 5):** bundling third-party brand icons (icon packs) carries trademark/licensing
  risk and is blocked without a cleared licence.
- The owner chose, from an explicit options prompt (2026-10-20), a **single uniform 16:9 grid** over
  keeping square icons or mixing banner/icon shapes.

So the decision is **how the tile sources and frames its art** so branded apps look right without new
dependencies, bundled assets, or an incoherent mix of tile shapes.

## Decision

1. **The tile art area is a uniform 16:9 card.** Every tile — favorites row and the All-apps grid — draws
   the same landscape card, so banners line up as one tidy row (TV-native, like Google TV shelves).
2. **Banner first, icon-on-card fallback.** `InstalledAppsRepository` loads the TV banner
   (`getActivityBanner()` → `getApplicationBanner()` fallback) alongside the icon, rasterised off the UI
   thread to a bounded 320×180 (NFR-002/NFR-003). The tile prefers the banner (filled, `ContentScale.Crop`);
   with no banner it centres the square icon on the card; an art-less app still falls back to the monogram.
3. **No new dependency, no bundled brand art.** Only platform `PackageManager` APIs; nothing is shipped in
   the APK and no icon-pack/third-party trademark asset is copied (Hard Rule 5).
4. **`AppEntry` gains an optional `banner: ImageBitmap?`** (defaulted null, so the pure `orderInstalledApps`
   seam and its tests are unchanged). The S/M/L tile tokens become landscape (width drives the 16:9 art,
   height leaves room for the art plus one label line); the label collapses to a single ellipsised line so
   every tile keeps an identical art/label split.

## Alternatives

| Decision point | Options (recommended **bold**) | Verdict |
|---|---|---|
| Tile shape | **Uniform 16:9 banners** · masked square icons (no banners) · hybrid banner-or-square | **Chosen (owner):** banners are what the owner asked for ("nice red YouTube button"); a uniform grid stays coherent. Masked squares never show the banner; hybrid mixes shapes/sizes and reads as inconsistent |
| Banner source | **`PackageManager.getActivityBanner`/`getApplicationBanner`** · bundled brand-icon pack · remote icon CDN | **Chosen:** platform API, already visible via ADR-0004, zero assets shipped. Icon packs are a trademark/licence risk (Hard Rule 5); a CDN would add network + a dependency |
| Banner fit | **`Crop` (fill, centred)** · `Fit` (letterbox with bars) | **Chosen:** banners are 16:9 and the logo is centred, so filling the card looks clean; letterboxing would leave bars when the art box isn't exactly 16:9 |
| Icon fallback | **Square icon centred on the card** · stretch icon to 16:9 | **Chosen:** centring preserves the icon's aspect; stretching would distort square icons |

## Consequences

**Positive**
- Branded TV apps (YouTube, Netflix, Prime, …) show their real banner; the grid reads as a coherent
  TV-native row.
- No new dependency, no shipped brand assets, no licence exposure; banner raster stays bounded (NFR-002).
- Additive data change — `orderInstalledApps` and its unit tests are untouched.

**Negative / costs (honest)**
- **Loading cost:** one extra `PackageManager` banner lookup + raster per app at enumeration time. Bounded
  (320×180, off the UI thread) but not free; acceptable because enumeration is already off the cold-start
  render path.
- **Fallback apps look plainer:** side-loaded phone apps with no `android:banner` show an icon on a card
  rather than full-bleed art — a deliberate, consistent fallback, not a regression.
- **`Crop` can trim a banner's extreme top/bottom** if a tile's art box deviates from 16:9; mitigated by
  sizing the S/M/L tokens close to 16:9. Final proportions are owner-verified on the TV.
- Larger tiles mean fewer columns per row in All-apps; intended, given the richer art.

## Rationale

The owner wanted recognisable, branded tiles, and Android TV already ships exactly that art as the banner —
so the right move is to *use the platform's own banner* rather than bundle icons (licence risk) or mask the
square icon (which still wouldn't surface the banner). A single uniform 16:9 grid keeps the result coherent
instead of mixing shapes. The change is additive (`AppEntry.banner` defaults null), costs no new dependency
or shipped asset, and degrades gracefully to an icon-on-card then a monogram. Exact tile proportions are a
visual call to confirm on the real TV.

## Follow-ups

- **Repository + tile** (`feat`, this milestone): load the banner in `InstalledAppsRepository`, add
  `AppEntry.banner`, render the 16:9 card in `AppTile`, widen the S/M/L tokens.
- **On-device check:** verify banner crop/letterbox and tile proportions on the TCL; tune the S/M/L dp if
  banners crop or the grid packs too few/many columns.
- **Later (optional):** mask/normalise adaptive icons for the fallback path so icon-only apps look as clean
  as the banners; a user toggle for banner-vs-icon if ever wanted.
