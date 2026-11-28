# Phase 6.5 — Equalizer UI

**Milestone:** 6 — Visual product  
**Priority:** Foundational  
**Depends on:** 6.1, 3.4, 4.1  
**Status:** Implemented

## Objective

Build the Equalizer screen: band controls, preamp, presets, and a clear indication of processing state — wired to the engine apply API.

## Why it matters

EQ is a differentiator for audio enthusiasts and a visually rich surface per the brief. It must remain usable and honest about processing state.

## Architecture

```text
EqualizerViewModel → Room preset repository
EqualizerViewModel → app-only MediaSession commands → service-owned HowlEngine
Gain guide → preamp and band settings
```

## Controls and presets

The preamp spans −24 to +12 dB, and each of the ten bands spans −12 to +12 dB. Sliders preview adjustments in half-decibel steps and submit a complete state on release. The ViewModel keeps one apply in flight and coalesces later changes to the latest requested state. The playback service acknowledges the state after the native call succeeds.

Bypass ramps the preamp and bands to zero and keeps the adjusted settings available for re-enabling. The graph plots configured preamp plus band gain at each octave center. Flat resets the gains. Named user presets are stored in Room and can be loaded across restarts.

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

- [x] Bands/preamp control engine EQ.
- [x] Presets apply and persist.
- [x] UI remains responsive during playback.
- [x] Processing state visible (enabled/bypass).

## Review checklist

- [x] Update rate to engine is sane.
- [x] Accessibility labels on bands.

## Potential pitfalls

- Sending 60 JNI applies per second while dragging.
- Curve visualization implying laboratory accuracy it doesn’t have.
- Blocking UI while applying.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Ten band sliders, preamp, bypass, gain guide, Flat and named presets on the EQ tab. |
| Files created/modified | Equalizer screen and ViewModel, Room preset queries, MediaSession EQ commands, and playback instrumentation. |
| Tests executed | Android build and JVM tests, Android instrumentation, JUCE-enabled host tests, and emulator UI checks during playback. |
| Results | All 62 host tests passed, including frequency response and smooth transitions back to flat. Android instrumentation verified complete EQ updates, bypass and restore, and malformed-command rejection during playback. The emulator restored the saved gains after an app restart and applied a 16 kHz cut while playback continued. |
| Known limitations | Active gains and bypass live for the playback service lifetime. Named presets persist in Room. The gain guide shows settings at band centers. Listening on physical hardware is pending. |
| Decisions made | Commit full EQ snapshots on slider release. Keep playback and EQ on the same native engine. Apply bypass through the existing smooth flat transition. |
| Next phase | 6.6 — Queue UI |

**Approval required before Phase 6.6.**
