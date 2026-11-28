# Phase 7.2 — Visualizer v1

**Milestone:** 7 — Visualizer & polish  
**Priority:** Optional  
**Depends on:** 7.1, 6.1  
**Status:** Implemented

## Objective

Ship a dedicated immersive visualizer with one or two styles (waveform/bars/particles) driven by smoothed analysis snapshots, with minimal controls and a reduced-motion fallback.

## Why it matters

Visualizer is part of the target experience but explicitly secondary to reliable playback. Keep it progressive and optional.

## Architecture

```text
Visualizer → same-app MediaSession snapshot request → playback engine level snapshot
Snapshot → UI-side smoothing → Compose Canvas (Pulse or Levels)
```

Open **Visualizer** from Now Playing. Pulse displays concentric rings that respond to peak and RMS. Levels displays peak in the upper bar and RMS in the lower bar. Both styles keep back and play/pause controls available. The visuals use the existing theme and lightweight Canvas geometry.

Active playback on a resumed Visualizer screen reads at most 20 snapshots per second, with one request in flight. Reads keep the engine’s analysis enabled through a renewable 750 ms lease. Pausing stops analysis, and the lease expires after the last read when the screen is left or hidden. UI smoothing uses a faster attack and a slower release. Display levels are quantized to 64 steps so small fluctuations can reuse the last frame. The app motion preference and Android’s animator scale both select a static composition and stop polling. Canvas uses the published snapshot without calling the DSP process graph. The two styles use basic geometry, so neither AGSL nor Filament is needed.

## Performance and battery

Frame work follows the snapshot cadence. A static screen draws on demand. The instrumentation runner supports a manual joint profile via `-e profileVisualizer true` and an optional indexed `-e profileTrackUri`. Follow its prompts to open Now Playing and then Visualizer. It samples five seconds per screen and records window frame durations and playback underruns over the same windows.

On an ARM64 emulator with a continuous FLAC, both screens recorded zero underruns in both runs. The first run reported total-frame p95 of 21 ms for Now Playing and 1,017 ms for Visualizer. After suppressing small redraws and isolating the Canvas layer, the next run reported 1,785 ms and 974 ms, while draw-stage p95 was 2.63 ms and 0.51 ms. The large swings and small frame samples leave frame pacing inconclusive on this emulator. A mid-range device and a battery measurement are still needed.

## Learning objectives

- Audio-reactive drawing without harming audio.
- Frame pacing vs analysis rate.
- Battery implications of full-screen effects.

## Scope

**Included**
- Visualizer destination.
- ≥1 solid style; optional second.
- Style picker minimal UI.
- Reduced-motion static/fallback.
- Basic performance note.

**Excluded**
- Marketplace of 50 skins.
- Rive/Filament unless justified in writing.

**Must not implement yet**
- Effects that require continuous max GPU at 120 Hz with no cap.

## Implementation steps

1. Visualizer screen shell.
2. Implement style drawing from snapshots.
3. Add reduced-motion path.
4. Profile frame time + underruns together.
5. Decision note if AGSL/Filament needed (default: no).

## Expected project structure

```text
android-app/.../ui/visualizer/
```

## Testing strategy

- Visualizer on mid-range device while playing.
- Toggle styles; no crash.
- Reduced-motion disables heavy animation.
- Audio underruns not worsened materially.

## Acceptance criteria

- [x] Immersive visualizer works from analysis snapshots.
- [x] No audio-thread dependency for drawing.
- [x] Reduced-motion option works.
- [x] Performance/battery caveats documented.

## Review checklist

- [x] Confirm brief’s “selective” visual richness.
- [x] Controls remain reachable.

## Potential pitfalls

- Allocating paths/shaders every frame carelessly.
- Coupling particle counts to raw FFT bins without smoothing.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Visualizer destination, Pulse and Levels styles, play/pause, and a static reduced-motion fallback. |
| Files created/modified | Visualizer screen and navigation, the session analysis bridge, and Android instrumentation. |
| Tests executed | Android build and JVM tests, Android instrumentation, isolated emulator UI smoke tests, and an emulator frame-time and underrun profile. |
| Results | Emulator smoke tests switched both styles, played and paused from the visualizer, and verified a static fallback for both motion settings. Joint profiling recorded zero underruns with and without Visualizer. Frame pacing varied substantially between emulator runs. |
| Known limitations | Both styles show block-level amplitude. Physical-device frame pacing, audio behavior, and battery draw remain unmeasured. |
| Decisions made | Use one capped polling loop and a short service lease. Draw with Canvas and apply smoothing entirely in the UI. |
| Next phase | 7.3 — Motion polish + reduced motion |

**Approval required before Phase 7.3.**
