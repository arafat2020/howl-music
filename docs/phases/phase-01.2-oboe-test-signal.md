# Phase 1.2 — Oboe Output + Deterministic Test Signal

**Milestone:** 1 — Audio foundation  
**Priority:** Foundational  
**Depends on:** 1.1  
**Status:** Implemented — awaiting device listen + your approval

## Objective

Open an Oboe (AAudio) output stream and render a deterministic test signal (e.g. 440 Hz sine) with lifecycle-safe start/stop on a real device.

## Why it matters

This is the first proof that the native output path works. Everything else (decoders, DSP, MediaSession) builds on a stable callback.

## Architecture

```text
Control thread: engine.start/stop
        │
        ▼
audio-android: Oboe AudioStream
        │ callback (real-time)
        ▼
Render sine into float/int16 buffer → device
```

Sine oscillator state lives in the engine/output object; **precomputed phase increment**; no allocations in callback.

## Learning objectives

- Oboe stream builder (sharing mode, performance mode, format).
- Audio callback constraints.
- Why test tones beat “silent” streams for validation.
- Float vs I16 output choices.

## Scope

**Included**
- Integrate Oboe (Prefetch/AndroidX or official package — pick one, document why).
- Output stream start/stop tied to Engine lifecycle.
- Deterministic sine (fixed frequency/amplitude).
- Basic failure handling if stream open fails.

**Excluded**
- File decoding, EQ, JNI transport API polish, underrun metrics UI (1.3 adds diagnostics).
- Low-latency tuning rabbit holes beyond a sane defaults.

**Must not implement yet**
- Disk I/O in callback; logging in callback; JNI from callback.

## Implementation steps

1. Add Oboe dependency to native CMake.
2. Implement `AudioOutput` in `native/audio-android`.
3. Generate sine in callback with preallocated state.
4. Start/stop from app button via existing bridge.
5. Verify audible tone on device; stop releases stream.

## Expected project structure

```text
native/audio-android/
  include/howl/audio_output.h
  src/oboe_output.cpp
```

## Testing strategy

- Manual: hear tone; stop silence; restart works.
- Leave running 2–5 minutes; no crash.
- Note device model + OS in completion report.

## Acceptance criteria

- [ ] Stable audible test signal on target device. *(build verified; confirm by ear)*
- [x] Start/stop is lifecycle-safe (no crash, no zombie streams). *(engine closes stream outside mutex; destructor stops)*
- [x] Callback performs no I/O/JNI/alloc.
- [ ] No recurring underruns under idle phone conditions (informal; formalize in 1.3).

## Review checklist

- [ ] Can explain stream configuration choices.
- [ ] Know what must never happen in the callback.
- [ ] Amplitude is safe (no full-scale blasting by default).

## Potential pitfalls

- Exclusive mode failures on some devices — fall back to shared.
- Forgetting to stop stream on destroy.
- Using `new`/`malloc` inside callback.
- Volume too high — use conservative gain (e.g. −12 dBFS).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | `howl::AudioOutput` (Oboe Shared / Float / stereo / 440 Hz sine @ ~−6 dBFS). Engine start/stop opens/closes stream. UI Start/Stop buttons. |
| Files created/modified | `native/audio-android/**`, Engine/status updates, `audio-bridge` Prefab+Oboe Gradle/CMake, `MainActivity.kt`, version catalog, docs. |
| Tests executed | Host lifecycle test PASS. `./gradlew :android-app:installDebug` SUCCESS. Emulator: Oboe/AAudio open OK; AudioFlinger active track; audible 440 Hz confirmed. |
| Results | **Audible acceptance met** on Pixel_API_36 emulator (Start → tone, Stop → silence). |
| Known limitations | Emulator audio quality ≠ device; Mac host output device matters. Diagnostics/underrun counters are Phase 1.3. Exclusive mode not forced (Shared + LowLatency with None fallback). |
| Decisions made | Oboe via **Maven Prefab** `com.google.oboe:oboe:1.9.3`. Format Float32. Amplitude 0.5 (hearable on quiet emulators). Stop tone in `onDestroy` only (not `onStop`). Version `howl_engine/0.2.0-oboe-sine`. |
| Next phase | 1.3 — Underrun & callback diagnostics |

### Listen checklist

```bash
./gradlew :android-app:installDebug
adb shell am start -n com.howl.music/.MainActivity
```

1. Tap **Start** → hear steady 440 Hz tone (not blasting).  
2. Tap **Stop** → silence.  
3. Start again → works.  
4. Leave running ~2 minutes → no crash.

**Approval required before Phase 1.3.**
