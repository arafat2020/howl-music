# Phase 2.5 — Queue Handoff & Track Change Design

**Milestone:** 2 — Playback pipeline  
**Priority:** Foundational  
**Depends on:** 2.3  
**Status:** Not started

## Objective

Design and implement a minimal in-engine playlist/queue handoff: advance to next item on EOF, explicit play-next/replace, and safe track change without tearing down the entire output stream.

## Why it matters

Queue behavior is product-critical and interacts with gapless later. We implement **correct track change** now and defer true gapless to Phase 8.1.

## Architecture

```text
QueueController (engine-side, non-RT)
  items: uri + metadata handles
  on EOF → load next → flush ring → continue

UI/MediaSession later map to the same commands
```

Gapless design notes captured; implementation not required here.

## Learning objectives

- Track change vs seek within track.
- Why tearing down Oboe every track is costly.
- Designing APIs that can later support gapless priming.

## Scope

**Included**
- Engine queue data structure (simple).
- Next/previous/play-at-index commands.
- Auto-advance on EOF.
- Repeat-all / repeat-one flags (basic).
- Design note for gapless (primer buffers, DSP tails) in completion report.

**Excluded**
- True sample-accurate gapless (8.1).
- Fancy Queue UI (6.6).
- Shuffle algorithm polish (can be naive).

**Must not implement yet**
- Crossfade effects.

## Implementation steps

1. Specify queue command API.
2. Implement queue + EOF auto-advance.
3. Keep Oboe stream running across track changes when format matches; document reopen rules when format differs.
4. Tests with two short WAVs.

## Expected project structure

```text
native/audio-engine/include/howl/queue.h
native/audio-engine/src/queue_controller.cpp
docs/decisions/gapless-design-notes.md  # short note OK
```

## Testing strategy

- Two-track queue advances automatically.
- Manual next/prev.
- Empty queue → stop cleanly.

## Acceptance criteria

- [ ] Multi-item queue plays sequentially on device.
- [ ] Track change does not crash; stream policy documented.
- [ ] Gapless design notes written (implementation deferred).
- [ ] Shuffle/repeat basic behavior defined.

## Review checklist

- [ ] Understand format-change reopen case.
- [ ] Know what 8.1 must add for gapless.

## Potential pitfalls

- Destroying Engine on every track.
- UI-owned queue diverging from engine-owned queue (pick one authority — recommend engine or a single domain service later mirrored to Media3).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 2.4 if pending; else 3.1 |

**Approval required before next phase.**
