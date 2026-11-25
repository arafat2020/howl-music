# Phase 7.2 — Visualizer v1

**Milestone:** 7 — Visualizer & polish  
**Priority:** Optional  
**Depends on:** 7.1, 6.1  
**Status:** Not started

## Objective

Ship a dedicated immersive visualizer with one or two styles (waveform/bars/particles) driven by smoothed analysis snapshots, with minimal controls and a reduced-motion fallback.

## Why it matters

Visualizer is part of the target experience but explicitly secondary to reliable playback. Keep it progressive and optional.

## Architecture

```text
VisualizerScreen
  poll snapshot → Compose Canvas / AGSL (if justified)
  styles selectable
  never calls into engine process graph
```

Evaluate AGSL only if Canvas is insufficient; Filament only if genuinely 3D — default to Compose.

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

- [ ] Immersive visualizer works from analysis snapshots.
- [ ] No audio-thread dependency for drawing.
- [ ] Reduced-motion option works.
- [ ] Performance/battery caveats documented.

## Review checklist

- [ ] Confirm brief’s “selective” visual richness.
- [ ] Controls remain reachable.

## Potential pitfalls

- Allocating paths/shaders every frame carelessly.
- Coupling particle counts to raw FFT bins without smoothing.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 7.3 — Motion polish + reduced motion |

**Approval required before Phase 7.3.**
