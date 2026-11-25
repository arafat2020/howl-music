# Phase 6.2 — Navigation Shell + Mini-Player

**Milestone:** 6 — Visual product  
**Priority:** Foundational  
**Depends on:** 6.1, 4.3  
**Status:** Not started

## Objective

Create the app navigation scaffold (Library / EQ / etc. destinations) with a persistent mini-player connected to the MediaSession/engine.

## Why it matters

Navigation + mini-player is the product chrome. Continuity into Now Playing is a brief motion principle; start with correct structure before heavy animation.

## Architecture

```text
Scaffold
  NavHost (tabs/destinations)
  MiniPlayer bar → MediaController / Player state
  Expand action → Now Playing (placeholder destination OK)
```

## Learning objectives

- Compose Navigation.
- Collecting playback state flows safely.
- Mini-player as shared chrome, not a screen duplicate of engine.

## Scope

**Included**
- Nav graph with stub destinations.
- Mini-player: art, title, play/pause, open Now Playing.
- Wiring to session/controller.
- Basic adaptive padding for gesture nav.

**Excluded**
- Full Library (6.3) / Now Playing visuals (6.4).
- Shared-element transitions polish.

**Must not implement yet**
- Visualizer tab content (7.2).

## Implementation steps

1. Add Navigation dependency if needed.
2. Build scaffold + mini-player composables.
3. Connect play/pause to controller.
4. Placeholder routes for later screens.

## Expected project structure

```text
android-app/.../ui/nav/HowlNavHost.kt
android-app/.../ui/player/MiniPlayer.kt
```

## Testing strategy

- Play from debug; mini-player updates.
- Navigate between tabs; mini-player persists.
- Rotate device; state retained reasonably.

## Acceptance criteria

- [ ] Navigation shell works.
- [ ] Mini-player reflects playback state.
- [ ] Play/pause from mini-player works.
- [ ] Design tokens used (no rogue colors).

## Review checklist

- [ ] Understand single controller ownership.
- [ ] Know what state survives config change.

## Potential pitfalls

- Multiple competing collectors causing flicker.
- Mini-player recomposition heavy enough to jank (defer art loads).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 6.3 — Library UI |

**Approval required before Phase 6.3.**
