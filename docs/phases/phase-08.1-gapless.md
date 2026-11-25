# Phase 8.1 — Gapless Implementation

**Milestone:** 8 — Device validation  
**Priority:** Release-hardening  
**Depends on:** 2.5, 2.4  
**Status:** Not started

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

If rates differ, document fallback (short gap / resample).

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

- [ ] Same-format gapless works on target test material.
- [ ] Fallback behavior documented when gapless impossible.
- [ ] No crash on rapid next-track spam.
- [ ] Limitations listed by codec.

## Review checklist

- [ ] Understand padding/delay compensation needs.
- [ ] DSP state policy across boundary is explicit.

## Potential pitfalls

- MP3 encoder delay ignored.
- Ring underrun at boundary creating a “gap” that looks like failure of logic.
- Different channel counts mid-queue.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 8.2 — Output / sample-rate diagnostics |

**Approval required before Phase 8.2.**
