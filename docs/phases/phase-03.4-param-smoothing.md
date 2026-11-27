# Phase 3.4 — Parameter Smoothing & Apply API

**Milestone:** 3 — DSP graph  
**Priority:** Foundational  
**Depends on:** 3.3  
**Status:** Implemented — pending physical-device listening check

## Objective

Unify DSP parameter application: batch set EQ bands + preamp, smooth transitions, and a stable engine API suitable for JNI and future presets (without Room yet).

## Why it matters

Presets and Equalizer UI will spam parameter changes. A batch apply API prevents JNI chatter and clicky updates.

## Architecture

```text
EqState { preampDb; bands[N] }
Engine.applyEqState(state)  // control thread
Graph nodes ramp toward targets on audio thread
```

Each update publishes the preamp and all ten bands together. The audio callback takes the latest complete update at the start of a block and ramps over 20 ms. If another update arrives during the ramp, the next ramp starts at the current values.

Preset names and storage belong to the app. The engine stores active numeric state only.

## Learning objectives

- Batch vs per-control updates across JNI.
- Separating persistence (Room) from processing state (engine).
- Click-free preset switching.

## Scope

**Included**
- `EqState` POD + apply/reset-flat APIs.
- Smoothing for band changes (reuse gain approach).
- Host test: apply state changes without allocating in process().
- Bridge methods ready for Phase 4.1/6.5.

**Excluded**
- Room preset database (5.3 / 6.5).
- Parametric EQ.
- Automatic loudness.

**Must not implement yet**
- UI preset picker (6.5).

## Implementation steps

1. Define `EqState` layout (ABI-stable-ish).
2. Implement apply + bypass/flat.
3. Ensure process() only reads smoothed values.
4. Add tests for rapid apply storms.

## Expected project structure

```text
native/audio-juce/include/howl/dsp/eq_state.h
audio-bridge/src/main/java/com/howl/music/bridge/EqState.kt
```

## Testing strategy

- Host: rapid apply; no leaks; process remains stable.
- Device: switch between flat and V-shape; listen for clicks.

## Acceptance criteria

- [x] Batch apply works from the Kotlin bridge during emulator playback.
- [x] Flat reset restores the preamp and all ten bands to 0 dB.
- [ ] Confirm at a listening test that normal preset-like changes are click-free. Host waveform checks passed.
- [x] Engine state contains numeric targets only.

## Review checklist

- [ ] Can explain app-owned presets vs engine-owned coefficients.
- [ ] Know ABI/layout implications for JNI.

## Potential pitfalls

- Divergent Kotlin and C structs.
- Partial updates leaving inconsistent bands.
- Smoothing time too long (feels laggy) or too short (clicks).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Complete EQ state apply and flat reset through native, C, JNI and Kotlin APIs. Both preamp and EQ changes ramp over 20 ms. The FLAC queue debug screen can reset to flat. |
| Files created/modified | Shared native and Kotlin EQ state types, ProcessGraph, gain and EQ nodes, Engine and bridge code, debug screen, and native tests. |
| Tests executed | Native host suites with and without JUCE, Android debug build and unit test, and an emulator smoke test that alternated a V-shaped update and flat reset during FLAC playback. |
| Results | 62/62 tests passed in both host configurations. No C++ allocations were observed in the callback path during the rapid-update test. Android build, unit test, and emulator playback smoke test passed. |
| Known limitations | A physical-device listening check for clicks is still pending. |
| Decisions made | Preserve individual controls and publish complete snapshots through three preallocated slots. Serialized control writers publish the latest target. The callback consumes it without a mutex. Put the shared POD in the DSP module so the graph and engine use the same type. The ABI passes a preamp float and ten band floats explicitly. |
| Next phase | 4.1 — Stable JNI / Kotlin engine API |

**Approval required before Phase 4.1.**
