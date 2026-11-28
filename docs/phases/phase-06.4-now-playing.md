# Phase 6.4 — Now Playing

**Milestone:** 6 — Visual product  
**Priority:** Foundational  
**Depends on:** 6.2, 4.3  
**Status:** Implemented

## Objective

Build the immersive Now Playing screen: large artwork, track/artist hierarchy, prominent transport, progress, and queue access — with selective motion (parallax/tilt) that respects reduced-motion.

## Why it matters

Now Playing is the emotional center of the product. It must feel premium without starving the audio thread or battery.

## Architecture

```text
MainActivity’s MediaController listener → shared playback snapshot → Now Playing
Transport and scrubber → MediaController commands
Room preference + Android animator setting → artwork motion
```

## Playback and motion

The mini-player opens Now Playing with the active track, artist, and cover. The artwork scales with the viewport so the transport fits on a typical phone. Dragging the position slider previews the target and sends one seek when the gesture finishes. Position refreshes every 250 ms while the screen is active and audio is playing.

The Android session exposes one playable item, so the transport skips five seconds back or fifteen seconds forward. Queue opens a dedicated screen that shows the current track and the empty upcoming state. The artwork tilts and shifts only during a horizontal drag, then springs back. Settings stores the reduction preference in Room, and Android’s animator setting also gates the gesture.

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

- [x] Now Playing matches brief hierarchy (art + controls clear).
- [x] Transport and seek work.
- [x] Motion is selective and disableable.
- [x] Playback remains stable during animations.

## Review checklist

- [x] Motion not driven by audio callback.
- [x] Contrast of metadata text acceptable.

## Potential pitfalls

- Scrubber fighting with position updates (need scrubbing state).
- Huge art decoding causing jank on open.
- Over-layering neon accents reducing readability.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Now Playing layout, play/pause, seek, time skips, Queue navigation, and optional drag-driven artwork motion. |
| Files created/modified | Now Playing screen, playback UI state, controller binding, navigation, and the Room-backed motion preference. |
| Tests executed | Android build, JVM tests, Android instrumentation, and an emulator smoke test with a tagged 75-second FLAC. |
| Results | The scrubber committed on release. A midpoint tap landed at 37.500 s, then the skip buttons landed at 32.500 s and 47.500 s. Queue navigation worked, and the session remained playing during the artwork gesture. The artwork moved and returned with motion enabled, stayed still after restarting with the saved reduction setting, and stayed still when Android animations were disabled. |
| Known limitations | Queue editing and track-to-track skipping depend on a future multi-item session. Accessibility labels were inspected in the emulator UI hierarchy. A spoken TalkBack pass and physical-device frame timing are pending. The debug emulator recorded 18 janky frames out of 110 in one gesture run. |
| Decisions made | Reuse the Activity’s single MediaController connection and the existing bounded artwork loader. Keep the Queue destination ready for phase 6.6. |
| Next phase | 6.5 — Equalizer UI |

**Approval required before Phase 6.5.**
