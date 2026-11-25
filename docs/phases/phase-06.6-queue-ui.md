# Phase 6.6 — Queue UI

**Milestone:** 6 — Visual product  
**Priority:** Foundational  
**Depends on:** 6.2, 2.5  
**Status:** Not started

## Objective

Provide a readable queue screen with reorder, remove, and play-next actions, synchronized with the engine/session queue authority.

## Why it matters

Queue management is a success criterion. Direct manipulation should feel stable, not flashy.

## Architecture

```text
QueueViewModel ↔ MediaSession queue / engine queue commands
LazyColumn + reorder gestures
```

Pick a single authority mapping (document): prefer session queue mirrored from domain that commands engine.

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

- [ ] Queue mirrors playback order.
- [ ] Reorder/remove/play-next work.
- [ ] Current track highlighted.
- [ ] Design remains readable/clean.

## Review checklist

- [ ] Authority for queue clearly documented.
- [ ] Empty queue state handled.

## Potential pitfalls

- Dual queues (UI list vs engine) diverging.
- Index shifts during concurrent auto-advance.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 7.1 analysis feed (optional) or 7.3 / 8.x hardening |

**Approval required before next phase.**
