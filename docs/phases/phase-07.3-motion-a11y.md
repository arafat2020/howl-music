# Phase 7.3 — Motion Polish + Reduced Motion

**Milestone:** 7 — Visualizer & polish  
**Priority:** Release-hardening  
**Depends on:** 6.4  
**Status:** Implemented

## Objective

Refine product motion (mini-player expansion, artwork transitions, control feedback) using physics-based springs, and ensure a coherent reduced-motion mode across the app.

## Why it matters

Brief motion principles are part of the premium feel — but accessibility and listening focus require an off switch for dramatic motion.

## Architecture

```text
Room preference + Android animator scale → theme motion mode
Theme motion mode → navigation, artwork, controls, queue, visualizer
```

## Motion behavior

| Mode | Opening and closing Now Playing | Artwork and controls | Queue and visualizer |
|---|---|---|---|
| Full | A short spring and fade connect the mini-player to Now Playing. | Artwork changes crossfade and drag settles with a spring. Buttons use subtle spring press feedback. | The dragged queue row follows the pointer. The visualizer reacts during playback. |
| Reduce motion | A 90 ms fade keeps the destination change in place. | Artwork switches in place, tilt is disabled, and buttons use an immediate pressed tint. | Queue reorder keeps rows stationary and highlights the target. The visualizer shows a static composition. |
| Android animations off | The destination changes immediately. | Artwork and controls use the reduced-motion behavior. | Queue and visualizer use the reduced-motion behavior. |

Settings stores the app preference in Room. The theme observes Android’s animator scale while active. The Settings switch reflects the saved preference independently of the system setting and displays when Android animations are off. During a fade, the departing destination is removed from accessibility traversal. Material focus feedback and control hit targets are preserved. Library tabs and rows remain stationary, with brief feedback only on interaction.

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

- [x] Motion tokens exist and are reused.
- [x] Reduced-motion path verified on major screens.
- [x] Library remains restrained.
- [x] Playback unaffected.

## Review checklist

- [x] Motion supports hierarchy, not decoration spam.
- [x] Preference persists.

## Potential pitfalls

- Different screens inventing their own durations.
- Reduced-motion only implemented on one screen.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Shared spring and fade specs, mini-player transitions, artwork crossfades, pressed feedback, and one app-wide motion policy. |
| Files created/modified | Motion tokens and system preference observer, theme and Settings, player and navigation, visualizer, queue, and shared controls. |
| Tests executed | Android build and JVM tests, Android instrumentation, emulator motion checks across a restart, and a TalkBack focus check on the emulator. The sine-meter assertion now waits for a representative callback block to account for short emulator callbacks. |
| Results | Artwork tilted and settled in full motion and stayed still with either motion setting. The visualizer followed the same policy, the preference persisted, and playback continued during standard navigation. A reduced-motion queue drag highlighted its target and reordered upcoming tracks. With TalkBack enabled, Back had focus in Now Playing and focus moved to the Settings heading on return. |
| Known limitations | The emulator check covered TalkBack focus and navigation. Spoken announcements and physical-device motion feel remain to be validated. |
| Decisions made | Animate only mini-player expansion and collapse. Apply reduced motion to transforms and retain non-spatial feedback and accessible actions. |
| Next phase | 7.4 — Performance & battery profiling |

**Approval required before Phase 7.4.**
