# Phase 4.2 — Media3 Custom Player Adapter

**Milestone:** 4 — Android integration  
**Priority:** Foundational  
**Depends on:** 4.1  
**Status:** Not started

## Objective

Implement an AndroidX Media3 `Player` adapter that delegates rendering to `HowlEngine`, preserving **one playback authority** (native engine).

## Why it matters

Media3 is how we integrate with system media patterns without inventing a second playback engine. The adapter must not become a competing pipeline.

## Architecture

```text
Media3 Player (adapter)
  getCurrentPosition → engine snapshot
  play/pause/seek → engine commands
  audio session / device → still engine/Oboe owned
```

Do not use ExoPlayer’s renderers for audio output in parallel.

## Learning objectives

- Media3 `Player` interface responsibilities.
- Why position should not drift on a separate clock.
- Mapping Media3 states to engine states.

## Scope

**Included**
- Media3 dependency in `android-media`.
- Custom `Player` implementation wrapping `HowlEngine`.
- Basic state listeners for playWhenReady / playbackState.
- Unit tests with fakes where practical.

**Excluded**
- Full MediaSessionService (4.3).
- Notification UI polish.
- Cast / MediaRoute.

**Must not implement yet**
- Dual output (ExoPlayer audio + Oboe).

## Implementation steps

1. Add Media3 libs carefully (only needed artifacts).
2. Implement adapter methods used by session/UI.
3. Poll engine position at a low rate for listeners.
4. Verify with a simple Activity using Media3 controls (debug).

## Expected project structure

```text
android-media/src/main/java/.../HowlMedia3Player.kt
```

## Testing strategy

- Play/pause/seek through Media3 APIs reaches engine.
- Position updates roughly track audio.
- No second audible stream.

## Acceptance criteria

- [ ] Media3 Player controls native engine successfully.
- [ ] Engine remains sole renderer.
- [ ] State mapping documented.
- [ ] No ExoPlayer audio renderers active for playback.

## Review checklist

- [ ] Can explain adapter vs engine ownership.
- [ ] Know which Player methods are stubbed vs supported.

## Potential pitfalls

- Accidentally using `ExoPlayer.Builder` defaults that open AudioTrack.
- High-frequency listener spam.
- Buffering state lies if not mapped carefully.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 4.3 — MediaSessionService & notification |

**Approval required before Phase 4.3.**
