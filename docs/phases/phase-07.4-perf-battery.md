# Phase 7.4 — Performance & Battery Profiling

**Milestone:** 7 — Visualizer & polish  
**Priority:** Release-hardening  
**Depends on:** 6.x (7.2 optional)  
**Status:** Not started

## Objective

Measure and document CPU, memory, underruns, frame time, and battery impact for core scenarios (background playback, Now Playing, EQ, optional visualizer).

## Why it matters

Commercial quality requires evidence. The brief demands measuring underruns, callback duration, latency, CPU, memory, and battery on real devices.

## Architecture

No new product architecture — this is a **measurement and remediation** phase. Fixes should be targeted, not speculative rewrites.

## Learning objectives

- Perfetto / Android Profiler basics.
- Distinguishing UI jank from audio xruns.
- Establishing baseline budgets.

## Scope

**Included**
- Device matrix subset (≥2 devices if possible).
- Scenario scripts (background 30 min; NP with art; EQ adjust; visualizer on/off).
- Written budgets and results table.
- Fix only clear regressions found.

**Excluded**
- Premature micro-optimizations without data.
- Claiming battery figures without methodology.

**Must not implement yet**
- New features during profiling unless blocking.

## Implementation steps

1. Define scenarios + metrics template.
2. Capture traces on physical devices.
3. Record results in `docs/perf/baseline.md`.
4. File/fix top issues (separate tiny follow-ups if large).

## Expected project structure

```text
docs/perf/baseline.md
docs/perf/traces/  # optional links/notes, not huge binaries in git
```

## Testing strategy

- Repeatability: run scenarios twice.
- Compare visualizer on vs off.
- Compare debug vs release builds (note differences).

## Acceptance criteria

- [ ] Baseline metrics documented for primary scenarios.
- [ ] Underrun behavior recorded.
- [ ] At least one actionable finding addressed or explicitly deferred.
- [ ] Methodology written clearly enough to rerun.

## Review checklist

- [ ] Results from physical devices.
- [ ] Budgets are realistic, not aspirational marketing.

## Potential pitfalls

- Profiling only on flagship devices.
- Checking in huge binary traces.
- Optimizing the wrong layer (UI vs audio).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 8.1 — Gapless implementation |

**Approval required before Phase 8.1.**
