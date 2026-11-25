# Phase 4.3 — MediaSessionService & Notification

**Milestone:** 4 — Android integration  
**Priority:** Foundational  
**Depends on:** 4.2  
**Status:** Not started

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

- [ ] Background playback works with visible media notification.
- [ ] Notification transport controls work.
- [ ] Activity destroy does not necessarily stop engine (policy documented).
- [ ] Target-SDK service requirements satisfied.

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
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 4.4 — Audio focus, headset, interruptions |

**Approval required before Phase 4.4.**
