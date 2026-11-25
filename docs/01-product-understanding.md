# 01 — Product Understanding

## Product objective

Build a **premium Android music player** that competes with established players (e.g. Poweramp) through two complementary strengths:

1. A **capable, extensible native audio engine** (C++ / JUCE DSP / Oboe).
2. A **distinctive, polished listening experience** (Kotlin / Jetpack Compose).

The product must feel powerful for audio enthusiasts without intimidating everyday listeners. **Playback reliability, sound-processing quality, and responsive interaction take priority over visual effects.**

## Product principles

| Principle | Meaning |
|-----------|---------|
| Audio first | Reliable playback, predictable processing, careful output-path handling |
| Beautiful by default | Strong visual identity, excellent typography, considered artwork |
| Fast and fluid | Responsive controls, smooth transitions, efficient rendering |
| Extensible | Modular engine that can grow into advanced DSP and device support |

## Target experience

- **Dark-first**, expressive music environment.
- **Library**: clean and practical for browsing.
- **Now Playing**: immersive.
- **Equalizer / Visualizer**: visually rich.
- Funky, futuristic vibe with **selective** 3D depth — not a cluttered “spaceship dashboard.”

## Visual direction (from brief)

| Role | Value | Usage |
|------|-------|-------|
| Base background | `#101014` | Primary dark canvas |
| Primary accent | `#B7F34A` (acid lime) | Active states, progress, key controls |
| Secondary accent | `#B65CFF` (electric purple) | Secondary emphasis / ambient |
| Warm accent | `#FF6B47` (vivid orange) | Occasional contrast |
| Light neutral | `#E9E5DC` | Text / high-contrast surfaces |

Motion: physics-based springs, selective depth, audio-reactive visuals from **smoothed analysis snapshots** (never from the audio callback). Provide a reduced-motion option.

## Success criteria (product)

- Stable background playback, queue management, seeking, and gapless-playback design.
- Modular DSP chain with EQ, gain control, and room for more effects.
- UI remains smooth while audio continues.
- Coherent design system across small and large screens.
- Clear diagnostics: format, sample rate, output route, processing state.

## First credible release (recommended scope)

**In:** local-file playback, library browsing, background playback, queue, high-quality EQ + presets, polished Now Playing.

**Later:** advanced visualizer modes, specialized effects, extensive USB DAC controls.

## North star

> A music player that sounds serious, feels alive, and stays effortless to use.

The differentiator is the combination: dependable modular native audio underneath an expressive premium UI. Visual spectacle must amplify music — not compromise playback, clarity, or battery life.
