# Phase 4.3 — MediaSessionService & Notification

**Milestone:** 4 — Android integration  
**Priority:** Foundational  
**Depends on:** 4.2  
**Status:** Implemented and verified on the Android emulator

## Objective

Run playback in a `MediaSessionService` (Media3) with a media-style notification and background playback that survives leaving the Activity.

## Why it matters

Background playback is a hard success criterion. Without a correct service foreground model, Android will kill playback.

## Architecture

```text
UI → binds/connects to MediaSession / Controller
MediaSessionService holds HowlMedia3Player → HowlEngine
Foreground notification with transport actions
```

## Learning objectives

- Foreground services + media exemptions.
- MediaSession vs Activity lifecycle.
- Notification actions → Player commands.

## Scope

**Included**
- `MediaSessionService` implementation.
- Notification via Media3 session utilities.
- Start playback from UI; leave app; audio continues.
- Manifest permissions/service declarations appropriate to target SDK.

**Excluded**
- Full headset/focus matrix (4.4).
- Library browsing.
- Fancy notification artwork pipeline (basic OK; Coil polish in 5.4/6.x).

**Must not implement yet**
- Multiple simultaneous sessions.

## Implementation steps

1. Implement service in `android-media`.
2. Connect app UI via `MediaController`.
3. Verify foreground notification appears.
4. Test process under recent-apps / screen off (basic).
5. Document start/stop service policies.

## Expected project structure

```text
android-media/.../HowlPlaybackService.kt
android-app/.../PlayerController.kt
```

## Testing strategy

- Play → Home → still playing.
- Notification pause/play works.
- Swipe-away behavior documented for target SDK.

## Acceptance criteria

- [x] Background playback continues with a visible media notification.
- [x] Play and pause work from Android’s media card.
- [x] Destroying the Activity releases the controller while active playback continues.
- [x] The service declares the media playback type and both foreground service permissions at target SDK 35.

## Service lifetime

| Action | Behavior |
|---|---|
| Tap Play in Howl | The Activity sends commands through its MediaController. The service resolves the bundled WAV, starts the native player, and lets Media3 post its media notification. |
| Press Home, turn off the screen, or destroy the Activity | Active playback continues. The Activity releases only its controller when stopped. Reopening Howl reconnects to the session without replacing the item. |
| Use the notification | Android routes Play and Pause to the same Player. Tapping the media card opens Howl. |
| Remove Howl from Recents | Media3 keeps active foreground playback running. When paused, stopped, or ended, its default task-removal handling stops the service if no controllers remain bound. |
| Open Engine diagnostics in a debug build | The controller clears the session item before opening the separate diagnostic screen. The native diagnostic playback stops when that screen is left. |
| Destroy the service | The service releases the MediaSession and the single player, which closes the native engine. |

Howl starts playback from the visible UI and uses Media3’s default foreground and notification handling. The manifest declares `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, and the `mediaPlayback` service type. The service is exported for media controls. Howl’s own controller can select the bundled item. Other trusted controllers get transport commands only. [Android MediaSessionService guide](https://developer.android.com/media/media3/session/background-playback)

## Review checklist

- [ ] Understand why service owns the Player.
- [ ] Know foreground notification obligations.

## Potential pitfalls

- Missing `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (API 34+).
- Leaking engine when service destroyed incorrectly.
- Starting service from illegal background contexts.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | One HowlPlaybackService and MediaSession, an Activity-scoped MediaController connection, and the standard Media3 transport notification. |
| Files created/modified | Media service and manifest, controller and launcher, diagnostic Activity, repeat handling, Gradle catalog, and Android instrumentation. |
| Tests executed | Android debug APK build and unit test, Android instrumentation, and API 36 arm64 emulator service and notification checks. |
| Results | Verified foreground notification, Home and screen-off playback, notification pause and resume, playback continuing after active task removal, service shutdown after paused task removal, and controller reconnect after Activity destruction. |
| Known limitations | The launcher currently plays the bundled WAV on repeat. Library browsing, persistent queue restoration, audio focus, and headset behavior are later phases. |
| Decisions made | Service owns playback and uses the default Media3 task-removal policy. Keep engine diagnostics in the debug build and clear session media before entering them. |
| Next phase | 4.4 — Audio focus, headset, interruptions |

**Approval required before Phase 4.4.**
