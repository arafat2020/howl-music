# Phase 4.4 — Audio Focus, Headset & Interruptions

**Milestone:** 4 — Android integration  
**Priority:** Foundational  
**Depends on:** 4.3  
**Status:** Implemented — physical headset validation pending

## Objective

Handle audio focus changes, noisy/headset disconnect, and common interruptions (incoming call patterns) with predictable pause/duck/resume policy.

## Why it matters

Enthusiasts forgive missing visualizers; they do not forgive playing over phone calls or continuing into an empty headphone jack unexpectedly.

## Architecture

```text
Android audio focus / noisy intent
        → AudioFocusPolicy on the session player looper
        → Media3 pause and native HowlEngine pause
```

Prefer implementing policy at the Media3/session layer, commanding the engine.

## Learning objectives

- Audio focus request/abandon.
- `ACTION_AUDIO_BECOMING_NOISY`.
- Ducking vs pausing trade-offs.

## Scope

**Included**
- Audio focus integration.
- Becoming-noisy → pause (recommended default).
- Documented call-interrupt behavior on test device.
- Resume policy (do/don’t auto-resume) explicit.

**Excluded**
- Automotive / Bluetooth edge-case matrix (partial later in 8.x).
- Per-app sound routing UI.

**Must not implement yet**
- Custom USB DAC controls.

## Implementation steps

1. Wire focus via Media3 / AudioManager appropriately.
2. Register noisy receiver (or Media3 equivalent handling).
3. Test with headset unplug and focus loss simulation.
4. Write policy table in completion report.

## Expected project structure

```text
android-media/.../AudioFocusPolicy.kt
```

## Testing strategy

- Unplug wired headset → pause.
- Start another media app → focus loss behavior matches policy.
- Notification still consistent with state.

## Acceptance criteria

- [x] Focus loss handled per written policy.
- [x] A synthetic becoming-noisy event pauses native playback.
- [ ] Confirm the Android broadcast with a physical wired-headset disconnect.
- [x] No crash during rapid focus acquisition and release.
- [x] Policy documented.

## Interruption policy

| Event | Howl behavior |
|---|---|
| Play or resume | Request media audio focus before starting or resuming the native stream. If the request is denied, remain paused. |
| Permanent or temporary focus loss | Pause the native stream, preserve position, and clear the Media3 play request. |
| Ducking request | Pause with the same focus-loss reason. |
| Becoming noisy | Pause with Media3’s becoming-noisy reason. |
| Incoming call | Pause when Android takes focus for the ringtone or call. |
| Focus returns, the call ends, or headphones reconnect | Remain paused until the user presses Play. |
| Pause, stop, EOF, or service teardown | Release held focus and unregister the receiver. |

The service registers the receiver while it has audio focus and runs the policy on the player looper. Android requires apps targeting API 35 to request focus from the top app or an active foreground service. If focus cannot be acquired from the notification, opening Howl and pressing Play retries the request. [Android audio focus guide](https://developer.android.com/media/optimize/audio-focus) · [Android AudioManager reference](https://developer.android.com/reference/android/media/AudioManager#ACTION_AUDIO_BECOMING_NOISY)

## Review checklist

- [ ] Know duck vs pause choice and why.
- [ ] Understand where policy lives (session vs engine).

## Potential pitfalls

- Engine keeps rendering silence while “playing” (state desync).
- Auto-resume surprising users after calls.
- Missing unregister → leaks.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Service-owned audio focus and becoming-noisy handling, with pause reasons propagated through Media3. |
| Files created/modified | AudioFocusPolicy, Media3 adapter, playback service, Android instrumentation, and this report. |
| Tests executed | Android debug build and unit test, emulator instrumentation for permanent, temporary, and ducking focus loss, 12 rapid requests, simulated incoming call, and synthetic becoming-noisy receiver invocation. |
| Results | Focus loss and the simulated call paused playback without automatic resume. The receiver test paused native playback, cleared the play request, reported the route-loss reason, and released focus. The media session reported Paused. |
| Known limitations | The emulator audio policy rejected the simulated wired output, so Android did not emit a headset-disconnect broadcast. A physical wired-headset check remains pending. |
| Decisions made | Pause on every loss, including ducking requests. Require explicit Play after interruptions. Scope the policy to session playback. |
| Next phase | 5.1 — Room schema |

**Approval required before Phase 5.1.**
