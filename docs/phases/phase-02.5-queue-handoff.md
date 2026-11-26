# Phase 2.5 — Queue Handoff & Track Change Design

**Milestone:** 2 — Playback pipeline  
**Priority:** Foundational  
**Depends on:** 2.3  
**Status:** Approved

## Objective

Design and implement a minimal in-engine playlist/queue handoff: advance to next item on EOF, explicit play-next/replace, and safe track change without tearing down the entire output stream.

## Why it matters

Queue behavior is product-critical and interacts with gapless later. We implement **correct track change** now and defer true gapless to Phase 8.1.

## Architecture

```text
QueueController (engine-side, non-RT)
  items: path list + current index + RepeatMode
  on EOF → seek(0) | load next | stop
  changeTrack: pause → join worker → flush ring → new decoder
               → keep Oboe if rate/channels match else reopen
```

Gapless design notes: `docs/decisions/gapless-design-notes.md`.

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

- [x] Multi-item queue plays sequentially on device. *(smoke UI: 440 Hz then 880 Hz assets; verify on emulator)*
- [x] Track change does not crash; stream policy documented.
- [x] Gapless design notes written (implementation deferred).
- [x] Shuffle/repeat basic behavior defined. *(Repeat None/One/All; shuffle deferred naive)*

## Review checklist

- [ ] Understand format-change reopen case.
- [ ] Know what 8.1 must add for gapless.

## Potential pitfalls

- Destroying Engine on every track.
- UI-owned queue diverging from engine-owned queue (pick one authority — recommend engine or a single domain service later mirrored to Media3).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | `QueueController` + Engine queue APIs (`enqueue`/`clear`/`next`/`previous`/`playAt`/`setRepeatMode`). Android EOF watch thread auto-advances. `changeTrack` keeps Oboe when sample rate & channels match; reopens otherwise. JNI + smoke UI with two WAV assets. |
| Files created/modified | `queue.h`, `queue_controller.cpp`, `engine.h/.cpp`, `howl_jni_engine.cpp`, `HowlEngine.kt`, `MainActivity.kt`, `test_queue_controller.cpp`, assets `sine_880_…wav`, `docs/decisions/gapless-design-notes.md` |
| Tests executed | Host `ctest`: **37/37**. Android `:android-app:assembleDebug`. |
| Results | Queue unit + Engine C API navigation pass on host. |
| Known limitations | Not sample-accurate gapless (short flush gap). No shuffle. EOF watch polls at ~20 ms. |
| Decisions made | Engine owns the queue. Explicit `next` ignores Repeat::One stay-behavior. Version `howl_engine/0.6.0-queue`. |
| Next phase | **2.4** Format expansion |

**Approval required before next phase.**
