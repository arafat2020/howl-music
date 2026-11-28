# Phase 8.4 — Device Matrix & Release Checklist

**Milestone:** 8 — Device validation  
**Priority:** Release-hardening  
**Depends on:** 8.1, 8.2, 7.4  
**Status:** Release audit completed. Public release is no-go pending physical validation and production signing.

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

- [x] Device matrix filled for available hardware.
- [x] Regression checklist completed with results.
- [x] Notices cover shipped third-party code.
- [x] Explicit go/no-go recorded.
- [x] No unmarked critical bugs in playback/background/focus.

## Review checklist

- [x] Claims in UI/README match validated behavior.
- [x] Optional features (visualizer/USB) labeled appropriately.

## Potential pitfalls

- Expanding scope instead of shipping.
- Hiding known gapless/codec limitations.
- Skipping physical-device confirmation.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Device matrix, go/no-go checklist, and release limitations. Corrected native ABI packaging and packaged third-party notices. |
| Files created/modified | [Device matrix](../release/device-matrix.md), [checklist](../release/checklist.md), [known limitations](../release/known-limitations.md), third-party notices and Android build scripts. |
| Tests executed | JUCE-enabled native tests, Android JVM tests, Android release-profile instrumentation and APK inspection. |
| Results | 70 native tests passed. See the [checklist](../release/checklist.md) for regression and packaging results. |
| Known limitations | Physical primary and secondary phones, x86_64 runtime smoke, sustained performance and battery testing, and production-signed verification remain pending. |
| Decisions made | Keep the candidate for internal testing. Record the physical-device and signed-artifact gates before asking for release approval. |
| Next phase | Post-release backlog (advanced effects, visualizer styles, USB extras) |

**First credible release gate — requires explicit approval to ship.**
