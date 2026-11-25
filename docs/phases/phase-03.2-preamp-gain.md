# Phase 3.2 — Preamp / Gain

**Milestone:** 3 — DSP graph  
**Priority:** Foundational  
**Depends on:** 3.1  
**Status:** Not started

## Objective

Add a preamp/gain node with smoothed gain changes to avoid clicks, controllable from a non-audio thread.

## Why it matters

Gain is the simplest useful DSP node and proves parameter update patterns before multi-band EQ.

## Architecture

```text
Control thread: setTargetGainDb(x)
Audio thread: smoothedGain → apply per sample/frame
```

## Learning objectives

- Parameter smoothing / one-pole interpolation.
- Linear gain vs dB.
- Headroom and clipping awareness.

## Scope

**Included**
- Gain node in graph.
- dB API with clamping.
- Smoothing on changes.
- Host tests for gain scaling.
- Temporary control from debug UI.

**Excluded**
- Limiter/compressor (optional later).
- Full EQ (3.3).
- Preset persistence (Room later; apply API in 3.4).

**Must not implement yet**
- Instant gain jumps without smoothing.

## Implementation steps

1. Implement `GainNode` with smoothing.
2. Wire setGain from Engine API.
3. Tests: 0 dB ≈ identity; −6 dB ≈ 0.5 linear.
4. Device: sweep gain; listen for clicks.

## Expected project structure

```text
native/audio-juce/src/gain_node.cpp
```

## Testing strategy

- Unit tests on buffers.
- Manual click check on device.

## Acceptance criteria

- [ ] Gain audible and measurable.
- [ ] Smoothed changes without obvious clicks.
- [ ] Clamped range (e.g. −24..+12 dB) documented.
- [ ] Real-time safe updates.

## Review checklist

- [ ] Can convert dB ↔ linear.
- [ ] Know why smoothing exists.

## Potential pitfalls

- Updating raw gain from UI every frame via JNI.
- Positive gain causing harsh clipping without warning (document; limiter later).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 3.3 — Graphic EQ |

**Approval required before Phase 3.3.**
