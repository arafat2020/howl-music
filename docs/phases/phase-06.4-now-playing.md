# Phase 6.4 — Now Playing

**Milestone:** 6 — Visual product  
**Priority:** Foundational  
**Depends on:** 6.2, 4.3  
**Status:** Not started

## Objective

Build the immersive Now Playing screen: large artwork, track/artist hierarchy, prominent transport, progress, and queue access — with selective motion (parallax/tilt) that respects reduced-motion.

## Why it matters

Now Playing is the emotional center of the product. It must feel premium without starving the audio thread or battery.

## Architecture

```text
NowPlayingViewModel ← MediaController + metadata
UI: artwork, controls, slider → player commands
Motion: Compose graphics / graphicsLayer; no audio-callback coupling
```

## Learning objectives

- Gesture + slider seeking UX.
- Selective 3D-inspired depth via Compose transforms.
- Accessibility for transport controls.

## Scope

**Included**
- Layout per brief guidance.
- Play/pause, skip, seek bar.
- Entry from mini-player expansion (transition can be simple first).
- Reduced-motion: disable parallax/tilt.
- Queue affordance (navigates to Queue screen even if Queue is stub).

**Excluded**
- Full visualizer (7.2).
- Advanced Filament 3D.
- Lyrics.

**Must not implement yet**
- Continuous non-interactive motion that competes with listening.

## Implementation steps

1. Compose Now Playing layout with tokens.
2. Bind controls to controller.
3. Add subtle parallax on drag / limited tilt.
4. Honor reduced-motion preference.
5. Performance check while playing.

## Expected project structure

```text
android-app/.../ui/player/NowPlayingScreen.kt
```

## Testing strategy

- Seek accuracy vs engine position.
- TalkBack basics on controls.
- Reduced-motion toggle removes tilt/parallax.
- Frame time acceptable on mid-range device.

## Acceptance criteria

- [ ] Now Playing matches brief hierarchy (art + controls clear).
- [ ] Transport and seek work.
- [ ] Motion is selective and disableable.
- [ ] Playback remains stable during animations.

## Review checklist

- [ ] Motion not driven by audio callback.
- [ ] Contrast of metadata text acceptable.

## Potential pitfalls

- Scrubber fighting with position updates (need scrubbing state).
- Huge art decoding causing jank on open.
- Over-layering neon accents reducing readability.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 6.5 — Equalizer UI |

**Approval required before Phase 6.5.**
