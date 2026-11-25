# Phase 2.3 — Transport Controls (Play / Pause / Seek)

**Milestone:** 2 — Playback pipeline  
**Priority:** Foundational  
**Depends on:** 2.2  
**Status:** Not started

## Objective

Implement play, pause, and seek against the native engine with correct position reporting and safe coordination between control thread, worker, and callback.

## Why it matters

Transport is the user-visible core of any player. Seek especially stresses buffer flushing and decoder state.

## Architecture

```text
Commands (play/pause/seek) → lock-free command queue / atomic flags
Worker applies seek (flush ring, decoder.seek)
Callback observes pause (output silence) without blocking
Position = decoder/engine frame clock snapshot
```

## Learning objectives

- Command queues into real-time systems.
- Why seek must flush buffered PCM.
- Position/duration as engine-owned truth.

## Scope

**Included**
- Play / pause / seek(ms or frames).
- Position/duration getters (UI-safe snapshots).
- Temporary UI buttons for validation.
- Tests for seek-to-start/mid/end behavior on WAV.

**Excluded**
- Shuffle/repeat modes UI polish.
- Queue of multiple tracks (2.5).
- MediaSession (4.x).

**Must not implement yet**
- Gapless track boundaries.

## Implementation steps

1. Define transport command types.
2. Implement pause as callback-visible flag.
3. Implement seek with ring flush + decoder seek.
4. Expose position snapshots via bridge.
5. Manual device validation.

## Expected project structure

```text
native/audio-engine/include/howl/transport.h
audio-bridge/.../HowlEngine.kt  # play/pause/seek/position
```

## Testing strategy

- Host tests with fake decoder if helpful.
- Device: seek mid-track; audio resumes at expected region.
- Rapid play/pause spam does not crash.

## Acceptance criteria

- [ ] Play/pause/seek work on device for WAV.
- [ ] Position moves forward while playing; stable while paused.
- [ ] Seek does not click catastrophically (minor clicks OK until DSP smoothing).
- [ ] No I/O in callback.

## Review checklist

- [ ] Understand flush-on-seek.
- [ ] Know how pause differs from stop.

## Potential pitfalls

- Updating decoder from audio callback.
- Position computed from UI clock instead of engine.
- Leaving stale PCM post-seek (wrong audio plays).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 2.5 recommended before heavy 2.4, or 2.4 if format pressure is higher |

**Approval required before next phase.**
