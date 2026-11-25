# Phase 7.3 — Motion Polish + Reduced Motion

**Milestone:** 7 — Visualizer & polish  
**Priority:** Release-hardening  
**Depends on:** 6.4  
**Status:** Not started

## Objective

Refine product motion (mini-player expansion, artwork transitions, control feedback) using physics-based springs, and ensure a coherent reduced-motion mode across the app.

## Why it matters

Brief motion principles are part of the premium feel — but accessibility and listening focus require an off switch for dramatic motion.

## Architecture

```text
MotionTokens / Duration / Spring specs in design system
UserPreference.reducedMotion → disables parallax, heavy transitions
```

## Learning objectives

- Spring specs vs fixed tweens.
- Accessibility `prefers reduced motion` mapping on Android.
- When motion helps hierarchy vs when it is noise.

## Scope

**Included**
- Shared motion tokens.
- Mini-player ↔ Now Playing transition improvement.
- Tactile pressed states on key controls.
- App-wide reduced-motion honor for known animations.

**Excluded**
- New visualizer styles.
- Filament.

**Must not implement yet**
- Constant ambient animations on Library lists.

## Implementation steps

1. Centralize motion specs in theme.
2. Improve NP expansion transition.
3. Wire reduced-motion preference to animations.
4. Review Library to remove excessive motion.

## Expected project structure

```text
android-app/.../ui/theme/Motion.kt
android-app/.../data/prefs/MotionPreferences.kt
```

## Testing strategy

- With reduced motion on: no parallax/tilt; transitions shorter/crossfade.
- With off: springs feel natural, not sluggish.
- TalkBack still usable during transitions.

## Acceptance criteria

- [ ] Motion tokens exist and are reused.
- [ ] Reduced-motion path verified on major screens.
- [ ] Library remains restrained.
- [ ] Playback unaffected.

## Review checklist

- [ ] Motion supports hierarchy, not decoration spam.
- [ ] Preference persists.

## Potential pitfalls

- Different screens inventing their own durations.
- Reduced-motion only implemented on one screen.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 7.4 — Performance & battery profiling |

**Approval required before Phase 7.4.**
