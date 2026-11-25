# Phase 4.4 — Audio Focus, Headset & Interruptions

**Milestone:** 4 — Android integration  
**Priority:** Foundational  
**Depends on:** 4.3  
**Status:** Not started

## Objective

Handle audio focus changes, noisy/headset disconnect, and common interruptions (incoming call patterns) with predictable pause/duck/resume policy.

## Why it matters

Enthusiasts forgive missing visualizers; they do not forgive playing over phone calls or continuing into an empty headphone jack unexpectedly.

## Architecture

```text
Android audio focus / noisy intent
        → MediaSession / Player callbacks
        → HowlEngine pause or volume duck policy
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

- [ ] Focus loss handled per written policy.
- [ ] Becoming-noisy pauses playback.
- [ ] No crash on rapid focus changes.
- [ ] Policy documented for users/devs.

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
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 5.1 — Room schema |

**Approval required before Phase 5.1.**
