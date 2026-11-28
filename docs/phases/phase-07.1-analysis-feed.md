# Phase 7.1 — Analysis Snapshot Feed

**Milestone:** 7 — Visualizer & polish  
**Priority:** Optional (useful earlier for meters)  
**Depends on:** 4.1, 2.2  
**Status:** Implemented

## Objective

Provide a real-time-safe path for audio analysis snapshots (peak/RMS first; FFT optional) that UI can poll without touching the audio callback beyond lightweight taps.

## Why it matters

Visualizer and reactive UI need data. The brief forbids coupling rendering to the audio callback — this phase builds the correct pipe.

## Architecture

```text
Oboe output buffer → opt-in peak and mean square → single lock-free atomic word
Engine control thread → C / JNI snapshot → foreground debug meters (20 Hz)
```

The callback samples the final interleaved output after DSP and zero-filling. Peak and mean square cover all output channels and the full callback block. A single 64-bit atomic carries the IEEE-754 bits for both values. One atomic load yields a coherent pair, and RMS is calculated on the reader. The callback uses one bounded pass, one store, and no allocation, lock, or JNI call. When metering is off, the callback checks one atomic flag and skips the pass.

The C and Kotlin APIs expose linear full-scale amplitudes, where 1.0 is 0 dBFS. A reader can poll at its own rate and smooth the readings for display. Disabled, paused, stopped, and newly opened streams read as silence until analyzed audio arrives. Engine diagnostics opts in and displays peak and RMS in dBFS.

## Cost

A release-style Apple clang 17 `-O3` host microbenchmark on an Apple M4 timed the meter on 48 kHz stereo input. The best of five 300,000-block runs measured about 0.51 µs for 192 frames and 3.12 µs for 1,024 frames, roughly 0.013% and 0.015% of those block periods. This measures the meter pass and publication alone. Android instrumentation measures underruns separately.

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

- [x] Snapshots readable from UI thread.
- [x] Audio callback work stays bounded/minimal.
- [x] No JNI from callback.
- [x] Documented analysis architecture choice (in-callback meters vs analysis thread).

## Review checklist

- [x] Understand publication model.
- [x] Know what visualizer may consume.

## Potential pitfalls

- Heavy FFT in callback causing xruns.
- Unbounded queues of PCM to analysis thread.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Opt-in peak and RMS snapshots, C and Kotlin readers, and debug meters. |
| Files created/modified | Native output tap and snapshot, engine API and JNI bridge, Android diagnostics, and native and Android tests. |
| Tests executed | 66 JUCE-enabled host tests, Android build and JVM tests, the Android instrumentation suite, and a debug-screen smoke test on the ARM64 emulator. |
| Results | Host tests covered stereo, silence, non-finite input, argument validation, and coherent concurrent publication. Android checked audio from both sine and file paths. Three consecutive two-second sine windows (off, on, off) saw 200, 200, and 197 callbacks and zero underruns each. The debug meters showed about −6 dBFS peak and −9 dBFS RMS and dropped below −60 dBFS when paused or stopped. |
| Known limitations | Snapshots show the latest block across channels. Spectrum, physical-device performance measurements, and a consumer in the playback service remain future work. |
| Decisions made | Publish aggregate levels from the callback in one atomic word. Calculate RMS on read and opt in per engine. |
| Next phase | 7.2 — Visualizer v1 |

**Approval required before Phase 7.2.**
