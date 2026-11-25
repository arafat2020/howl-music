# Phase 3.3 — Graphic EQ

**Milestone:** 3 — DSP graph  
**Priority:** Foundational  
**Depends on:** 3.2  
**Status:** Not started

## Objective

Implement a graphic equalizer (fixed bands) using JUCE DSP building blocks, with per-band gains and host unit tests for filter response sanity.

## Why it matters

EQ is a headline capability for a Poweramp-class product and validates the modular DSP graph under realistic CPU load.

## Architecture

```text
ProcessGraph: Gain → GraphicEQ → (future nodes)
GraphicEQ: N peaking/shelf filters at fixed center frequencies
```

Band count decision (e.g. 10-band) recorded; parametric EQ can follow later as optional.

## Learning objectives

- Peaking filters / Q / center frequency.
- Measuring CPU cost of cascaded filters.
- Why EQ prepare() depends on sample rate.

## Scope

**Included**
- Graphic EQ node with agreed band count.
- Per-band gain control API.
- Unit tests (impulse or sine probes) for rough expected behavior.
- CPU measurement note on device.

**Excluded**
- Full parametric EQ UI/engine (optional later).
- Preset library UX (UI in 6.5; apply API 3.4).
- Visualizer curve (UI can approximate in 6.5).

**Must not implement yet**
- Rebuilding filters with heap alloc in callback.

## Implementation steps

1. Choose band frequencies + default Q.
2. Implement EQ node with JUCE dsp::IIR or ProcessorDuplicator as appropriate.
3. Host tests for boost/cut at center frequencies.
4. Device validation with music material.
5. Record CPU/callback duration impact.

## Expected project structure

```text
native/audio-juce/include/howl/dsp/graphic_eq.h
native/audio-juce/src/graphic_eq.cpp
native/tests/test_graphic_eq.cpp
```

## Testing strategy

- Unit: boost 1 kHz band → higher energy near 1 kHz vs neighbors.
- Device: extreme boosts should not crash (clipping possible).
- Diagnostics: callback duration vs baseline.

## Acceptance criteria

- [ ] Band gains audibly affect playback.
- [ ] Filter unit tests pass.
- [ ] Sample-rate changes re-prepare filters safely.
- [ ] CPU cost documented; no callback allocations.

## Review checklist

- [ ] Know band centers and Q rationale.
- [ ] Understand stereo processing approach.

## Potential pitfalls

- Unstable filters at extreme Q/gain.
- Ignoring Nyquist when sample rate is low.
- Per-band JNI spam; batch parameter sets.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 3.4 — Parameter smoothing & apply API |

**Approval required before Phase 3.4.**
