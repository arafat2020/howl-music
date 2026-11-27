# Phase 3.2 — Preamp / Gain

**Milestone:** 3 — DSP graph  
**Priority:** Foundational  
**Depends on:** 3.1  
**Status:** Implemented — pending device listening check

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

- [ ] Gain audible on device. Host buffer tests confirm measurable scaling.
- [ ] Smoothed changes without obvious clicks on device. Host tests confirm the 20 ms ramp.
- [x] Clamped range of −24 through +12 dB documented.
- [x] Real-time safe updates through a lock-free atomic target.

## Review checklist

- [ ] Can convert dB ↔ linear.
- [ ] Know why smoothing exists.

## Potential pitfalls

- Updating raw gain from UI every frame via JNI.
- Positive gain causing harsh clipping without warning (document; limiter later).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | File playback preamp with a 20 ms stereo-linked linear-amplitude ramp, an Engine/JNI gain setter, and a debug slider in FLAC queue mode. |
| Files created/modified | Gain node, ProcessGraph, Engine and C API, Oboe output, JNI and Kotlin bridge, Android debug screen, host graph tests, and DSP documentation. |
| Tests executed | Host suite with JUCE enabled and disabled. Android debug APK build and unit test. |
| Results | 53/53 host tests passed in each configuration. Android debug APK built for arm64-v8a and x86_64. Android unit test passed. |
| Known limitations | ADB had no online Android target for the listening check. Gain applies to file playback. Boost can clip. |
| Decisions made | Use the same native gain node in both build configurations so the default Android build supports preamp without JUCE. |
| Next phase | 3.3 — Graphic EQ |

For the device check, start the debug app in FLAC queue mode, select Repeat All, press Play, then move the Preamp slider between 0 dB and −24 dB while listening for abrupt level changes or clicks.

**Approval required before Phase 3.3.**
