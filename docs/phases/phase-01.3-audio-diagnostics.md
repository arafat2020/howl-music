# Phase 1.3 — Underrun & Callback Diagnostics

**Milestone:** 1 — Audio foundation  
**Priority:** Foundational  
**Depends on:** 1.2  
**Status:** Implemented — awaiting your approval

## Objective

Add engine-level diagnostics: underrun/xrun counts, stream sample rate, frames per burst/callback, and a safe way to read them from the control/UI thread.

## Why it matters

Without metrics, “it sounds fine” is not engineering. Diagnostics support later claims about stability and feed the product’s output diagnostics success criterion.

## Architecture

```text
Audio callback: increment atomics only (xruns, callback count)
Control/UI thread: snapshot read → Kotlin → on-screen debug text
```

No logging inside the callback.

## Learning objectives

- Atomics for cross-thread counters.
- Difference between measurement and logging.
- What Oboe reports vs what we compute.

## Scope

**Included**
- Counters/fields: underruns, callbacks, sample rate, channel count, frames/callback.
- Thread-safe snapshot API on Engine.
- Temporary debug UI to display values (can be ugly).

**Excluded**
- Full settings screen polish.
- Battery/CPU profiling tools (Phase 7.4).
- USB route labeling (Phase 8.2).

**Must not implement yet**
- Visualizer FFT.

## Implementation steps

1. Define `EngineDiagnostics` POD + atomic fields.
2. Hook Oboe error/xrun callbacks carefully.
3. Expose `engine_get_diagnostics` via bridge.
4. Show values on placeholder UI; update on a timer (e.g. 2 Hz), not per callback via JNI.

## Expected project structure

```text
native/audio-engine/include/howl/diagnostics.h
audio-bridge/.../EngineDiagnostics.kt
```

## Testing strategy

- Start tone; verify sample rate/channels non-zero.
- Background the app briefly; note underrun behavior.
- Confirm UI updates without audio glitches.

## Acceptance criteria

- [x] Diagnostics readable without blocking audio.
- [x] Values match stream configuration.
- [x] No log/alloc in audio callback for this feature.
- [ ] Documented baseline on at least one physical device. *(emulator baseline below; physical still open)*

## Review checklist

- [ ] Understand why UI polls snapshots.
- [ ] Know which fields are authoritative.

## Potential pitfalls

- Calling JNI every callback “to update UI.”
- Using mutexes with unpredictable wait in callback.
- Treating emulator metrics as meaningful.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | `EngineDiagnostics` POD; atomics in Oboe callback (`callbackCount`, `lastFramesPerCallback`, `underrunCount` via `getXRunCount`, `errorCount` via error callback); `Engine::diagnostics()` + C/JNI/Kotlin snapshot; Compose debug text polled at 500 ms while running. |
| Files created/modified | `diagnostics.h`, `audio_output.*`, `engine.*`, `howl_jni_engine.cpp`, `HowlEngine.kt`, `EngineDiagnostics.kt`, `MainActivity.kt`, host test, docs. |
| Tests executed | Host `howl_engine_lifecycle_test` PASS (`0.3.0-diagnostics`). `installDebug` on Pixel_API_36. UI: `start=Ok`, `rate=48000 ch=2 burst=960 cbFrames=504`, callbacks rising (325→732), xruns=0. |
| Results | Snapshot path works without JNI-from-callback. Emulator baseline recorded. |
| Known limitations | Physical-device baseline not yet documented. Emulator xrun counts are not meaningful. |
| Decisions made | UI polls at **2 Hz** (`delay(500)`). Pack snapshot over `LongArray(7)` JNI. Version `howl_engine/0.3.0-diagnostics`. Authoritative: sample rate / channels / framesPerBurst from stream at open; framesPerCallback + callbackCount from our atomics; underruns from Oboe `getXRunCount()`. |
| Next phase | 1.4 — GoogleTest harness |

### Emulator baseline (Pixel_API_36)

```text
rate=48000 Hz  ch=2
burst=960  cbFrames=504
callbacks=rising
xruns=0  errors=0
```

**Approval required before Phase 1.4.**
