# Phase 6.5 — Equalizer UI

**Milestone:** 6 — Visual product  
**Priority:** Foundational  
**Depends on:** 6.1, 3.4, 4.1  
**Status:** Not started

## Objective

Build the Equalizer screen: band controls, preamp, presets, and a clear indication of processing state — wired to the engine apply API.

## Why it matters

EQ is a differentiator for audio enthusiasts and a visually rich surface per the brief. It must remain usable and honest about processing state.

## Architecture

```text
EqualizerViewModel
  reads/writes EqState + preset repository
  applies to HowlEngine (via service-held engine or bridge API)
Optional: animated response curve from coefficients (approx OK)
```

## Learning objectives

- Mapping UI controls to DSP state.
- Debouncing vs batching slider updates.
- Showing processing state without claiming unverified fidelity.

## Scope

**Included**
- Band sliders + preamp.
- Preset list (flat + user presets from 5.3).
- Bypass/enable control if supported.
- Basic curve visualization (can be approximate).

**Excluded**
- Parametric EQ advanced editor.
- Per-output-device EQ profiles (later).
- Claiming measured room correction.

**Must not implement yet**
- Per-frame JNI band updates without batching.

## Implementation steps

1. EQ screen layout with design system.
2. Bind sliders to ViewModel; batch apply.
3. Preset save/load UX.
4. Verify audible changes and click-free updates.
5. Show sample rate / EQ active diagnostics snippet if available.

## Expected project structure

```text
android-app/.../ui/eq/EqualizerScreen.kt
android-app/.../ui/eq/EqualizerViewModel.kt
```

## Testing strategy

- Move bands; hear change.
- Switch presets rapidly.
- Bypass returns near-flat audible result.
- No audio glitches under normal use.

## Acceptance criteria

- [ ] Bands/preamp control engine EQ.
- [ ] Presets apply and persist.
- [ ] UI remains responsive during playback.
- [ ] Processing state visible (enabled/bypass).

## Review checklist

- [ ] Update rate to engine is sane.
- [ ] Accessibility labels on bands.

## Potential pitfalls

- Sending 60 JNI applies per second while dragging.
- Curve visualization implying laboratory accuracy it doesn’t have.
- Blocking UI while applying.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 6.6 — Queue UI |

**Approval required before Phase 6.6.**
