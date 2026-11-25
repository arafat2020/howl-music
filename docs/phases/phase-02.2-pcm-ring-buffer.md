# Phase 2.2 — PCM Ring Buffer / Worker → Callback

**Milestone:** 2 — Playback pipeline  
**Priority:** Foundational  
**Depends on:** 2.1  
**Status:** Not started

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

- [ ] WAV audible via ring path on device.
- [ ] Callback never blocks on decode/I/O.
- [ ] Ring tests pass on host.
- [ ] Underrun policy documented and observed.

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
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 2.3 — Transport controls |

**Approval required before Phase 2.3.**
