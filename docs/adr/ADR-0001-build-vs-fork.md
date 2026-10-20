# ADR-0001: Build vs. Fork

## Status

Proposed — **blocked on Phase 0 research.** Do not finalize until the feature matrix, license analysis,
and architecture comparison are evidence-backed.

## Context

NexaTV needs a foundational decision: build the launcher from scratch, fork an existing open-source
Android TV launcher, or take a hybrid path. The choice affects engineering effort, maintainability,
licensing obligations, UX flexibility, long-term independence, and upgrade path. This decision must be
grounded in the Phase 0 research artifacts, not in assumptions or convenience.

Inputs required before deciding:
- [FEATURE_MATRIX.md](../research/FEATURE_MATRIX.md)
- [LICENSE_ANALYSIS.md](../research/LICENSE_ANALYSIS.md)
- [ARCHITECTURE_COMPARISON.md](../research/ARCHITECTURE_COMPARISON.md)
- Device audit ([ANDROID-TV.md](../technical/ANDROID-TV.md))

## Decision

_TBD — to be written once research is complete._

## Alternatives

Evaluate each against: license · engineering effort · maintainability · architecture · UX flexibility ·
Android TV compatibility · long-term independence · custom-integration ability · upgrade path · upstream
dependency · testing · build/release complexity.

1. **Fork FLauncher**
2. **Fork LTvLauncher** (and/or Extended)
3. **Fork another project** (Yukuza / Tarang / TV Launcher / LauncherCompose / other)
4. **Greenfield Flutter implementation**
5. **Hybrid** — references + selective, license-cleared reuse; independent architecture _(current hypothesis)_

| Option | License | Effort | Maintainability | UX flexibility | TV compatibility | Independence | Upgrade path |
|--------|---------|--------|-----------------|----------------|------------------|--------------|--------------|
| Fork FLauncher | _TBD_ | _TBD_ | _TBD_ | _TBD_ | _TBD_ | _TBD_ | _TBD_ |
| Fork LTvLauncher | _TBD_ | _TBD_ | _TBD_ | _TBD_ | _TBD_ | _TBD_ | _TBD_ |
| Fork other | _TBD_ | _TBD_ | _TBD_ | _TBD_ | _TBD_ | _TBD_ | _TBD_ |
| Greenfield Flutter | _TBD_ | _TBD_ | _TBD_ | _TBD_ | _TBD_ | _TBD_ | _TBD_ |
| Hybrid | _TBD_ | _TBD_ | _TBD_ | _TBD_ | _TBD_ | _TBD_ | _TBD_ |

## Consequences

_TBD — positive and negative outcomes of the chosen option, including obligations inherited from any reused code._

## Rationale

_TBD — why this option over the others. Must not reduce to "it is easier."_
