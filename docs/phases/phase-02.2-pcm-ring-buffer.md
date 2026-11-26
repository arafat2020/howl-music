# Phase 2.2 — PCM Ring Buffer / Worker → Callback

**Milestone:** 2 — Playback pipeline  
**Priority:** Foundational  
**Depends on:** 2.1  
**Status:** Implemented — awaiting your approval

## Objective

Connect decoder output to the audio callback through a preallocated, bounded, real-time-safe PCM ring (SPSC) filled by a worker thread.

## Why it matters

This is the core concurrency pattern of the player: **slow/variable decode** vs **hard-deadline render**. Getting this wrong causes glitches or deadlocks.

## Architecture

```text
Worker thread: decoder.read → ring.push
Audio callback: ring.pop → output (underrun → silence + counter)
Control thread: start/stop/load coordinates both
```

Optional simple resampling policy documented if file rate ≠ stream rate (decision may defer heavy resampler to 2.4).

## Learning objectives

- SPSC ring buffers and why they fit audio.
- Underrun behavior (output silence, count).
- Preallocation and capacity sizing (latency vs robustness).

## Scope

**Included**
- Lock-free or carefully bounded ring for PCM frames.
- Worker loop lifecycle tied to engine.
- Underrun → silence + diagnostics increment.
- Replace sine-only path with “file or sine” test mode.

**Excluded**
- Full transport UI (2.3).
- Gapless crossfade (8.1).
- Complex resampler (document interim: require matching rates or reject).

**Must not implement yet**
- Allocating ring buffers inside the callback.
- Blocking wait in callback when empty.

## Implementation steps

1. Implement `PcmRingBuffer` with fixed capacity.
2. Host tests: push/pop integrity; overflow/underflow behavior.
3. Wire worker + callback in Engine.
4. Play WAV fixture through device path.

## Expected project structure

```text
native/audio-engine/
  include/howl/pcm_ring_buffer.h
  src/pcm_ring_buffer.cpp
  src/decode_worker.cpp
```

## Testing strategy

- GoogleTest for ring correctness.
- Device: play WAV; underrun count near zero under normal load.
- Starve worker artificially → silence + underrun increment.

## Acceptance criteria

- [x] WAV audible via ring path on device (emulator verified: `mode=File`, `xruns=0` while playing).
- [x] Callback never blocks on decode/I/O.
- [x] Ring tests pass on host.
- [x] Underrun policy documented and observed.

## Review checklist

- [ ] Can explain single-producer/single-consumer assumptions.
- [ ] Know who owns the worker thread join on stop.

## Potential pitfalls

- Using `std::mutex` with unbounded waits in callback.
- Wrong capacity → constant underruns or huge latency.
- Race on stop (worker writes after ring destroy).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | SPSC `PcmRingBuffer` (power-of-2 frames); `DecodeWorker`; Engine `setPlayMode`/`load`; AudioOutput sine **or** ring source; underrun → silence + `underrun_count`; app Switch Sine/WAV + 5s asset. |
| Files created/modified | `pcm_ring_buffer.*`, `decode_worker.*`, `engine.*`, `audio_output.*`, JNI/Kotlin, `MainActivity`, assets WAV, `test_pcm_ring_buffer.cpp`, docs. |
| Tests executed | Host **26/26 PASS**. Emulator: File mode `start=Ok`, `rate=48000`, `xruns=0` during playback. |
| Results | Ring path live. Short fixture EOF produced underruns (expected); 5s asset stays clean. |
| Known limitations | No resampler — file rate/channels must match device (stereo + equal sample rate). No seek/pause UI (2.3). WAV does not loop. |
| Decisions made | Ring capacity **16384** frames (~340 ms @ 48 kHz). Prefill before `requestStart`. Stop order: **join worker → stop Oboe → drop ring**. Underrun policy: pad silence, increment atomic (File mode); Sine mode still reports Oboe xruns. Version `howl_engine/0.4.0-pcm-ring`. |
| Next phase | 2.3 — Transport controls |

### Underrun policy

When `pop` returns fewer frames than the callback requested: write **silence** for the remainder and increment **`underrun_count`**. Never block or decode on the audio thread. After EOF the ring drains and underruns rise — expected until transport/looping (2.3).

**Approval required before Phase 2.3.**
