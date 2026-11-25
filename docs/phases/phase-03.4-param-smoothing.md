# Phase 3.4 — Parameter Smoothing & Apply API

**Milestone:** 3 — DSP graph  
**Priority:** Foundational  
**Depends on:** 3.3  
**Status:** Not started

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

Preset *names* stay in app storage later; engine stores active numeric state only.

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
native/audio-engine/include/howl/eq_state.h
audio-bridge/.../EqState.kt
```

## Testing strategy

- Host: rapid apply; no leaks; process remains stable.
- Device: switch between flat and V-shape; listen for clicks.

## Acceptance criteria

- [ ] Batch apply works from bridge.
- [ ] Flat reset works.
- [ ] Click-free under normal preset-like changes.
- [ ] Engine remains free of preset name/storage concerns.

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
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 4.1 — Stable JNI / Kotlin engine API |

**Approval required before Phase 4.1.**
