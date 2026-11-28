# Phase 6.6 — Queue UI

**Milestone:** 6 — Visual product  
**Priority:** Foundational  
**Depends on:** 6.2, 2.5  
**Status:** Implemented

## Objective

Provide a readable queue screen with reorder, remove, and play-next actions, synchronized with the engine/session queue authority.

## Why it matters

Queue management is a success criterion. Direct manipulation should feel stable, not flashy.

## Architecture

```text
Queue screen ← MediaController timeline ← service-owned playlist
Queue actions → same-app MediaSession commands → playlist → native playback
LazyColumn + drag handles and accessible move actions
```

The Media3 player in the playback service owns the ordered entries and current position. The native engine renders the selected file. The same order drives Now Playing, the Queue screen, media transport, and automatic advancement at end of track. The UI renders a controller snapshot and submits edits by entry ID. Duplicate files get distinct IDs.

Use **Play next** in Library to insert after the current track. With an empty queue, this selects the track and leaves playback paused. Tapping a library track starts it and preserves the upcoming entries. In Queue, drag a handle or use the row’s move actions. **Play now** pulls the selected track forward and keeps the other upcoming tracks. Removing the current track advances to the next entry while preserving play or pause. Removing the final current track empties the queue.

Now Playing skips tracks when available and falls back to time skips. The session retains completed entries for previous-track navigation. Library files are staged on a background worker before insertion. Insertion resolves against the latest current entry after staging finishes.

## Learning objectives

- Reorder UX in Compose.
- Keeping UI queue consistent with playback index.
- Play-next semantics.

## Scope

**Included**
- Upcoming tracks list.
- Reorder, remove, play-next / play-now.
- Current item indication.
- Stable scrolling during updates.

**Excluded**
- Collaborative queues.
- Lyrics/queue hybrid screens.

**Must not implement yet**
- Overdone drag particles/effects.

## Implementation steps

1. Bind queue state from controller/repository.
2. Implement list actions → commands.
3. Reorder persistence through authority.
4. Navigate from Now Playing.

## Expected project structure

```text
android-app/.../ui/queue/QueueScreen.kt
```

## Testing strategy

- Reorder then skip — order respected.
- Remove current vs other items.
- Rapid updates don’t crash or desync permanently.

## Acceptance criteria

- [x] Queue mirrors playback order.
- [x] Reorder/remove/play-next work.
- [x] Current track highlighted.
- [x] Design remains readable/clean.

## Review checklist

- [x] Authority for queue clearly documented.
- [x] Empty queue state handled.

## Potential pitfalls

- Dual queues (UI list vs engine) diverging.
- Index shifts during concurrent auto-advance.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Current and upcoming tracks, drag reorder, accessible move actions, removal, Play next, Play now, and queue-aware transport. |
| Files created/modified | Queue screen, Library and Now Playing integration, MediaSession playlist and edit commands, and playback instrumentation. |
| Tests executed | Android build and JVM tests, Android instrumentation, 62 JUCE-enabled host tests, and an emulator smoke test with four MediaStore tracks. |
| Results | Drag reorder changed the next track, Next followed the edited order, and removing the current track advanced while playback continued. Play now and the empty state worked on the emulator. Instrumentation also checked removal while paused and end-of-track advancement after Play now. |
| Known limitations | Queue order and history live for the playback service lifetime. Drag targets are limited to visible rows. Staged tracks and history use temporary cache space. Track changes can have a playback gap. |
| Decisions made | Use one service-owned Media3 playlist. Keep entry identities stable across reorder and duplicate tracks. Stage files before insertion and release the staged sources when their entries are removed or the service ends. |
| Next phase | 7.1 analysis feed (optional) or 7.3 / 8.x hardening |

**Approval required before next phase.**
