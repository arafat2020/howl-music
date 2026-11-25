# Phase 7.1 — Analysis Snapshot Feed

**Milestone:** 7 — Visualizer & polish  
**Priority:** Optional (useful earlier for meters)  
**Depends on:** 4.1, 2.2  
**Status:** Not started

## Objective

Provide a real-time-safe path for audio analysis snapshots (peak/RMS first; FFT optional) that UI can poll without touching the audio callback beyond lightweight taps.

## Why it matters

Visualizer and reactive UI need data. The brief forbids coupling rendering to the audio callback — this phase builds the correct pipe.

## Architecture

```text
Callback: write peak/RMS into atomics / lock-free slot (minimal work)
Optional analysis thread: consume PCM tap → FFT → triple buffer
UI thread: read latest snapshot at frame rate
```

## Learning objectives

- Triple buffering / snapshot publication.
- Why FFT often belongs off the deadline thread.
- Smoothing analysis values for visuals.

## Scope

**Included**
- Snapshot struct (levels; optional spectrum bands).
- Engine API to copy latest snapshot.
- Debug meters on a screen.
- Document CPU cost.

**Excluded**
- Full visualizer styles (7.2).
- Shader toys.

**Must not implement yet**
- Allocating FFT workspaces in the callback each buffer.
- JNI per callback.

## Implementation steps

1. Define snapshot POD.
2. Implement lightweight peak/RMS in callback.
3. Optional: analysis thread for spectrum.
4. Kotlin API + debug meters.
5. Confirm no underrun regression.

## Expected project structure

```text
native/audio-engine/include/howl/analysis_snapshot.h
android-app/.../ui/debug/LevelMeters.kt
```

## Testing strategy

- Play tone; meters move.
- Silence → near-zero.
- Diagnostics underruns unchanged vs baseline.

## Acceptance criteria

- [ ] Snapshots readable from UI thread.
- [ ] Audio callback work stays bounded/minimal.
- [ ] No JNI from callback.
- [ ] Documented analysis architecture choice (in-callback meters vs analysis thread).

## Review checklist

- [ ] Understand publication model.
- [ ] Know what visualizer may consume.

## Potential pitfalls

- Heavy FFT in callback causing xruns.
- Unbounded queues of PCM to analysis thread.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 7.2 — Visualizer v1 |

**Approval required before Phase 7.2.**
