# 05 — Phased Development Roadmap

The PDF’s eight delivery phases are preserved as **milestones**. Each milestone is split into **small, independently reviewable engineering phases** suitable for one focused session.

**Rule:** Do not start a phase until the previous phase is accepted. Do not auto-advance after code generation.

## Legend

| Tag | Meaning |
|-----|---------|
| **F** | Foundational — required for first credible release |
| **R** | Release-hardening — required before calling the product “solid” |
| **O** | Optional / follow-on — after reliable playback |

## Milestone map (PDF → engineering phases)

| PDF milestone | Engineering phases | Priority |
|---------------|--------------------|----------|
| Setup (implied) | 0.1 – 0.4 | **F** |
| 01 Audio foundation | 1.1 – 1.4 | **F** |
| 02 Playback pipeline | 2.1 – 2.5 | **F** |
| 03 DSP graph | 3.1 – 3.4 | **F** |
| 04 Android integration | 4.1 – 4.4 | **F** |
| 05 Library & data | 5.1 – 5.4 | **F** |
| 06 Visual product | 6.1 – 6.6 | **F** |
| 07 Visualizer & polish | 7.1 – 7.4 | **O** / partial **R** |
| 08 Device validation | 8.1 – 8.4 | **R** / USB **O** |

---

## Phase index

### Milestone 0 — Project foundation

| Phase | Name | Depends on | Priority |
|-------|------|------------|----------|
| [0.1](phases/phase-00.1-dev-environment.md) | Development environment | — | **F** |
| [0.2](phases/phase-00.2-android-skeleton.md) | Android Gradle + Compose skeleton | 0.1 | **F** |
| [0.3](phases/phase-00.3-native-jni-smoke.md) | Native CMake + JNI smoke test | 0.2 | **F** |
| [0.4](phases/phase-00.4-module-wiring.md) | Module layout & build wiring | 0.3 | **F** |

### Milestone 1 — Audio foundation

| Phase | Name | Depends on | Priority |
|-------|------|------------|----------|
| [1.1](phases/phase-01.1-engine-lifecycle.md) | C++ engine skeleton & lifecycle | 0.4 | **F** |
| [1.2](phases/phase-01.2-oboe-test-signal.md) | Oboe output + deterministic test signal | 1.1 | **F** |
| [1.3](phases/phase-01.3-audio-diagnostics.md) | Underrun & callback diagnostics | 1.2 | **F** |
| [1.4](phases/phase-01.4-native-gtest.md) | GoogleTest harness | 1.1 (parallelizable after 1.1) | **F** |

### Milestone 2 — Playback pipeline

| Phase | Name | Depends on | Priority |
|-------|------|------------|----------|
| [2.1](phases/phase-02.1-wav-decoder.md) | Decoder abstraction + WAV/PCM | 1.2 | **F** |
| [2.2](phases/phase-02.2-pcm-ring-buffer.md) | PCM ring buffer / worker → callback | 2.1 | **F** |
| [2.3](phases/phase-02.3-transport-controls.md) | Play / pause / seek | 2.2 | **F** |
| [2.4](phases/phase-02.4-format-expansion.md) | Format expansion (decision + impl) | 2.3 | **F** |
| [2.5](phases/phase-02.5-queue-handoff.md) | Queue handoff & track change design | 2.3 | **F** |

### Milestone 3 — DSP graph

| Phase | Name | Depends on | Priority |
|-------|------|------------|----------|
| [3.1](phases/phase-03.1-juce-graph-skeleton.md) | JUCE integration & DSP graph skeleton | 2.3 + license check | **F** |
| [3.2](phases/phase-03.2-preamp-gain.md) | Preamp / gain | 3.1 | **F** |
| [3.3](phases/phase-03.3-graphic-eq.md) | Graphic EQ | 3.2 | **F** |
| [3.4](phases/phase-03.4-param-smoothing.md) | Parameter smoothing & apply API | 3.3 | **F** |

### Milestone 4 — Android integration

| Phase | Name | Depends on | Priority |
|-------|------|------------|----------|
| [4.1](phases/phase-04.1-jni-control-api.md) | Stable JNI / Kotlin engine API | 2.3, 1.3 | **F** |
| [4.2](phases/phase-04.2-media3-adapter.md) | Media3 custom player adapter | 4.1 | **F** |
| [4.3](phases/phase-04.3-mediasession-service.md) | MediaSessionService & notification | 4.2 | **F** |
| [4.4](phases/phase-04.4-focus-interruptions.md) | Audio focus, headset, interruptions | 4.3 | **F** |

### Milestone 5 — Library & data

| Phase | Name | Depends on | Priority |
|-------|------|------------|----------|
| [5.1](phases/phase-05.1-room-schema.md) | Room schema | 4.1 | **F** |
| [5.2](phases/phase-05.2-media-scan.md) | Media scanning & permissions | 5.1 | **F** |
| [5.3](phases/phase-05.3-playlists-prefs.md) | Playlists & preferences | 5.1 | **F** |
| [5.4](phases/phase-05.4-artwork-cache.md) | Artwork loading & cache | 5.2 | **F** |

### Milestone 6 — Visual product

| Phase | Name | Depends on | Priority |
|-------|------|------------|----------|
| [6.1](phases/phase-06.1-design-system.md) | Compose design system | 0.2 | **F** |
| [6.2](phases/phase-06.2-nav-miniplayer.md) | Navigation shell + mini-player | 6.1, 4.3 | **F** |
| [6.3](phases/phase-06.3-library-ui.md) | Library UI | 6.2, 5.2 | **F** |
| [6.4](phases/phase-06.4-now-playing.md) | Now Playing | 6.2, 4.3 | **F** |
| [6.5](phases/phase-06.5-equalizer-ui.md) | Equalizer UI | 6.1, 3.4, 4.1 | **F** |
| [6.6](phases/phase-06.6-queue-ui.md) | Queue UI | 6.2, 2.5 | **F** |

### Milestone 7 — Visualizer & polish (mostly optional)

| Phase | Name | Depends on | Priority |
|-------|------|------------|----------|
| [7.1](phases/phase-07.1-analysis-feed.md) | Analysis snapshot feed | 4.1, 2.2 | **O** (meters useful earlier) |
| [7.2](phases/phase-07.2-visualizer-v1.md) | Visualizer v1 | 7.1, 6.1 | **O** |
| [7.3](phases/phase-07.3-motion-a11y.md) | Motion polish + reduced motion | 6.4 | **R** |
| [7.4](phases/phase-07.4-perf-battery.md) | Performance & battery profiling | 6.x, 7.2 optional | **R** |

### Milestone 8 — Device validation & hardening

| Phase | Name | Depends on | Priority |
|-------|------|------------|----------|
| [8.1](phases/phase-08.1-gapless.md) | Gapless implementation | 2.5, 2.4 | **R** |
| [8.2](phases/phase-08.2-output-diagnostics.md) | Output / sample-rate diagnostics UI | 1.3, 4.1 | **R** |
| [8.3](phases/phase-08.3-usb-dac.md) | USB DAC investigation | 8.2 | **O** |
| [8.4](phases/phase-08.4-release-checklist.md) | Device matrix & release checklist | 8.1, 8.2, 7.4 | **R** |

---

## Suggested first-release critical path

```text
0.1 → 0.2 → 0.3 → 0.4
  → 1.1 → 1.2 → 1.3 → 1.4
  → 2.1 → 2.2 → 2.3 → 2.5 → 2.4
  → 3.1 → 3.2 → 3.3 → 3.4
  → 4.1 → 4.2 → 4.3 → 4.4
  → 5.1 → 5.2 → 5.3 → 5.4
  → 6.1 → 6.2 → 6.3 → 6.4 → 6.5 → 6.6
  → 7.3 → 8.1 → 8.2 → 8.4
```

Parallelism (only after dependencies exist):

- **6.1 Design system** can start after 0.2 (UI learning track) while audio work continues — integrate carefully so UI does not invent a second player API.
- **1.4 GoogleTest** can proceed in parallel with 1.2–1.3 once lifecycle interfaces exist.

## Phase template reminder

Every phase doc includes: Objective, Why it matters, Architecture, Learning objectives, Scope, Implementation steps, Expected structure, Testing strategy, Acceptance criteria, Review checklist, Potential pitfalls, Completion report.

## Explicit non-goals until approved

- Building every effect/codec before stable playback.
- Filament / full 3D engine for decorative depth.
- Multiple competing playback pipelines.
- Optimizing theoretical max sample rates before measuring devices.
- Over-animating library/navigation.

---

## Approval gate

**Stop here for review.**

Please review:

1. [Product understanding](01-product-understanding.md)
2. [Architecture review](02-architecture-review.md) (especially open decisions)
3. [Project setup](03-project-setup.md)
4. [Repository structure](04-repository-structure.md)
5. This roadmap and the phase specs under [`phases/`](phases/)

When you approve, we begin **[Phase 0.1 — Development environment](phases/phase-00.1-dev-environment.md)** only.
