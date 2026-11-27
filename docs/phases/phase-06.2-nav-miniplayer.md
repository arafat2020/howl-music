# Phase 6.2 — Navigation Shell + Mini-Player

**Milestone:** 6 — Visual product  
**Priority:** Foundational  
**Depends on:** 6.1, 4.3  
**Status:** Implemented and verified on the Android emulator

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

## Shell and controller

Library, EQ, and Settings are top-level destinations. A shared scaffold keeps the mini-player above the tab bar and uses the theme’s safe-drawing insets. Tapping the track opens the Now Playing placeholder. Back returns to the active tab. The Now Playing placeholder holds the existing seek and stop controls. Debug tools are under Settings. [Navigation Compose guide](https://developer.android.com/codelabs/jetpack-compose-navigation)

The Activity owns one controller connection and listens for Media3 events. The mini-player uses that snapshot for metadata and play/pause. The Now Playing placeholder polls position only while it is visible and playback is requested. Rotation restores the navigation stack and reconnects to the service-owned playback session.

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

- [x] Navigation shell works.
- [x] Mini-player reflects playback state.
- [x] Play/pause from mini-player works.
- [x] Design tokens used (no rogue colors).

## Review checklist

- [ ] Understand single controller ownership.
- [ ] Know what state survives config change.

## Potential pitfalls

- Multiple competing collectors causing flicker.
- Mini-player recomposition heavy enough to jank (defer art loads).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Compose navigation tabs, persistent mini-player, Now Playing placeholder, and controller state wired to the shell. |
| Files created/modified | MainActivity, HowlNavHost, MiniPlayer, Compose Navigation dependency, and this phase report. |
| Tests executed | Android debug build and unit test, emulator instrumentation, and emulator interaction and rotation checks. |
| Results | Started playback from the mini-player, switched to EQ, rotated with playback active, expanded to Now Playing, returned to EQ, paused, and confirmed the same track in Settings. |
| Known limitations | Tabs are placeholders. Playback uses the bundled sample, which has no embedded cover. The mini-player shows its artwork fallback. |
| Decisions made | Keep playback and position authoritative in MediaSession and the native engine. Preserve tab state in Navigation Compose. |
| Next phase | 6.3 — Library UI |

**Approval required before Phase 6.3.**
