# Phase 8.1 — Gapless Implementation

**Milestone:** 8 — Device validation  
**Priority:** Release-hardening  
**Depends on:** 2.5, 2.4  
**Status:** Implemented

## Objective

Implement sample-accurate (or near) gapless playback between consecutive tracks when formats/rates match, based on the design notes from Phase 2.5.

## Why it matters

Gapless is expected by enthusiasts (albums, live recordings). Doing it after stable track change reduces risk.

## Architecture

```text
Near EOF: prime next decoder
Worker stitches PCM into ring without underrun bubble
DSP: define policy for EQ state continuity (keep) vs reverb tails (if any)
```

See the [gapless decision and verification notes](../decisions/gapless-status.md) for the queue, fallback, and diagnostics behavior.

## Learning objectives

- Priming and encoder padding issues (MP3 gapless info if applicable).
- Why container/codec padding breaks naïve EOF joins.
- Testing methodology for gaps (waveform capture / ear + metrics).

## Scope

**Included**
- Gapless path for supported same-format transitions.
- Tests with known gapless album pairs if available.
- Diagnostics flag: gapless active/fallback.
- Document limitations per codec.

**Excluded**
- Crossfade as substitute for true gapless (optional separate effect later).
- Universal promise across all codecs/devices.

**Must not implement yet**
- Marketing “perfect gapless everywhere” claims.

## Implementation steps

1. Re-read 2.5 design notes; refine.
2. Implement priming + stitch.
3. Device-test with gapless source material.
4. Record codec-specific limitations.

## Expected project structure

```text
native/audio-engine/src/gapless_handoff.cpp
docs/decisions/gapless-status.md
```

## Testing strategy

- Album track pairs known to be gapless.
- Compare vs intentional pause.
- Underrun counters during transition.

## Acceptance criteria

- [x] Same-format gapless works on target test material.
- [x] Fallback behavior documented when gapless impossible.
- [x] No crash on rapid next-track spam.
- [x] Limitations listed by codec.

## Review checklist

- [x] Understand padding/delay compensation needs.
- [x] DSP state policy across boundary is explicit.

## Potential pitfalls

- MP3 encoder delay ignored.
- Ring underrun at boundary creating a “gap” that looks like failure of logic.
- Different channel counts mid-queue.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Pre-open and decode the next track head, append compatible PCM in the playback ring, and publish the boundary to Media3. |
| Files created/modified | Native handoff and decode worker, engine APIs and JNI, Media3 queue integration, and native and Android tests. |
| Tests executed | 70 native host tests, Android build and JVM tests, and the Android instrumentation suite on an ARM64 emulator. |
| Results | Host output matched exact PCM concatenation. Emulator WAV and FLAC joins each recorded 49 additional callbacks and zero additional underruns. Media3 advanced metadata, rapid Next commands preserved the playlist, and seeking within a joined track retained track-relative position. |
| Known limitations | WAV and FLAC stereo at matching decoded rates only. Late queue edits can be rejected after PCM is committed. Physical listening remains pending. |
| Decisions made | Preserve the stream and EQ state on the join. Keep Media3 as the playlist authority and reopen the stream for an incompatible source. |
| Next phase | 8.2 — Output / sample-rate diagnostics |

**Approval required before Phase 8.2.**
