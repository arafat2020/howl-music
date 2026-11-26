# Gapless design notes (Phase 2.5)

**Status:** Design only — implementation deferred to Phase 8.1.

## What Phase 2.5 does

Track change is **correct but not sample-accurate gapless**:

1. Pause Oboe pop + decode push.
2. Flush the PCM ring.
3. Replace the decoder / DecodeWorker for the next file.
4. Prefill the ring, then resume.

When sample rate and channel count match the open Oboe stream, the stream **stays open**. When they differ, Oboe is stopped and restarted (documented reopen rule).

There is a short audible gap (flush + join + prefill), typically tens of milliseconds.

## What Phase 8.1 must add

| Piece | Role |
|-------|------|
| Primer / look-ahead decode | Decode the next track into a side buffer before EOF of the current track |
| Boundary splice | At the last sample of track N, switch the ring/source to track N+1 without clearing audible PCM |
| DSP tail / latency | EQ/graph delay lines must drain or crossfade so filters do not click at the splice |
| Format guard | Same as today: if rate/channels differ, fall back to reopen (gapless only within matching format) |
| Encoder padding | Some compressed formats have encoder delay/padding — strip or account for when we leave WAV |

## API shape that stays stable

- Engine-owned queue remains the authority for next/prev/EOF.
- `playAt` / `next` / `previous` already mean “change track”; 8.1 upgrades the handoff implementation behind those calls.
- Seek-within-track stays distinct from track change (flush + decoder seek vs decoder replace).

## Non-goals until 8.1

- Crossfade between tracks.
- Gapless across sample-rate changes.
- Media3 / UI-owned parallel queues.
