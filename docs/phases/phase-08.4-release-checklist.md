# Phase 8.4 — Device Matrix & Release Checklist

**Milestone:** 8 — Device validation  
**Priority:** Release-hardening  
**Depends on:** 8.1, 8.2, 7.4  
**Status:** Not started

## Objective

Produce a release-ready checklist: device matrix results, regression suite status, known limitations, licensing notices, and go/no-go criteria for the first credible release.

## Why it matters

Shipping without a checklist reintroduces risk that incremental phases worked hard to contain. This phase is the gate to calling the product “first credible release.”

## Architecture

Process/documentation phase. May include minor bugfixes discovered during matrix runs — no new feature work.

## Learning objectives

- Release engineering for audio apps.
- Distinguishing blockers vs accepted limitations.
- Third-party notices / SBOM hygiene.

## Scope

**Included**
- Device matrix table (OS versions, OEMs tested).
- Regression checklist (transport, EQ, background, focus, library scan, gapless where claimed).
- THIRD_PARTY_NOTICES completeness review (JUCE/FFmpeg/etc.).
- Known limitations section for release notes.
- Go/no-go recommendation.

**Excluded**
- Play Store listing copy marathon (unless requested).
- New feature milestones.

**Must not implement yet**
- Large new DSP modules as “just one more thing.”

## Implementation steps

1. Draft matrix template; run tests on available devices.
2. Execute regression suite; log failures.
3. Fix only release blockers.
4. Finalize notices and limitations.
5. Write go/no-go with rationale.

## Expected project structure

```text
docs/release/device-matrix.md
docs/release/checklist.md
docs/release/known-limitations.md
THIRD_PARTY_NOTICES.txt
```

## Testing strategy

- Full manual regression on primary device.
- Smoke on secondary device.
- Native host tests green.
- Android unit tests green.

## Acceptance criteria

- [ ] Device matrix filled for available hardware.
- [ ] Regression checklist completed with results.
- [ ] Notices cover shipped third-party code.
- [ ] Explicit go/no-go recorded.
- [ ] No unmarked critical bugs in playback/background/focus.

## Review checklist

- [ ] Claims in UI/README match validated behavior.
- [ ] Optional features (visualizer/USB) labeled appropriately.

## Potential pitfalls

- Expanding scope instead of shipping.
- Hiding known gapless/codec limitations.
- Skipping physical-device confirmation.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | Post-release backlog (advanced effects, visualizer styles, USB extras) |

**First credible release gate — requires explicit approval to ship.**
