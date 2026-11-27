# Phase 4.2 — Media3 Custom Player Adapter

**Milestone:** 4 — Android integration  
**Priority:** Foundational  
**Depends on:** 4.1  
**Status:** Implemented and verified on the Android emulator

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

- [x] Media3 Player controls native engine successfully.
- [x] Engine remains sole renderer.
- [x] State mapping documented.
- [x] No ExoPlayer dependency or audio renderers in the playback path.

## State mapping

| Media3 state | Howl behavior |
|---|---|
| `IDLE` | Item has yet to be prepared, playback was explicitly stopped, or a command failed. A command failure also sets `playerError`. |
| `READY` | A local file has been loaded. Play starts the native stream. Pause leaves it open and freezes the native position. |
| `ENDED` | The native stream stopped at EOF. The adapter reports the item duration as the final position and can replay from the start. |

The adapter reads one native transport snapshot every 200 ms while playback advances. It refreshes immediately after transport commands. Media3 receives this cached position without a separate tracking clock. The bridge does not expose an instantaneous buffered duration or underrun-to-buffering transition, so the adapter does not advertise a buffering state.

The available commands cover setting a single local `file:` WAV or FLAC item, preparing, play and pause, seek, stop, and release. Playlist editing, remote sources, repeat and shuffle, speed, and volume are outside this adapter. A seek requested before native playback starts is applied just after opening the stream. Calls run synchronously on the player application looper.

The adapter extends Media3's `SimpleBasePlayer` and declares only the commands it handles. [AndroidX SimpleBasePlayer reference](https://developer.android.com/reference/androidx/media3/common/SimpleBasePlayer)

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
| What was built | A single-item Media3 Player backed by HowlEngine and a debug Activity with play, pause, seek, stop, and position controls. |
| Files created/modified | Media3 adapter, dependency catalog, debug Activity and launcher, Android instrumentation, and phase documentation. |
| Tests executed | Android debug APK build and unit test, native and Media3 Android instrumentation, runtime dependency inspection, and debug Activity smoke test. |
| Results | Emulator verified play and pause, advancing and frozen position, seek, end and replay, listener transitions, stop, and rejection of unsupported sources. The runtime graph contains media3-common as its only Media3 artifact. |
| Known limitations | Single local item and synchronous commands. Playback was tested on the arm64 emulator. |
| Decisions made | Pin media3-common 1.6.1. Use native snapshots as the position source and cancel polling when playback is paused, stopped, ended, or released. |
| Next phase | 4.3 — MediaSessionService & notification |

**Approval required before Phase 4.3.**
