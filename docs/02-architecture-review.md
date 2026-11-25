# 02 — Architecture Review

## Concise verdict

The brief’s architecture is **sound for a commercial Android player**: Kotlin/Compose owns product UX and lifecycle; a C++ engine owns real-time playback and DSP; Media3 adapts the custom engine into system media surfaces. The two biggest risks are **decoder/licensing complexity** and **over-coupling UI to the audio path**. Both are manageable if we sequence work as “engine first, Media3 second, visuals last.”

## Proposed architecture (as specified)

```
┌─────────────────────────────────────────────────────────────┐
│ android-app                                                 │
│  Compose UI · ViewModels · Design system · Coil artwork     │
└───────────────────────────┬─────────────────────────────────┘
                            │ Flow / State
┌───────────────────────────▼─────────────────────────────────┐
│ android-media                                               │
│  MediaSessionService · Notifications · Audio focus          │
│  Media3 custom Player adapter                               │
└───────────────────────────┬─────────────────────────────────┘
                            │ JNI (coarse commands / snapshots)
┌───────────────────────────▼─────────────────────────────────┐
│ audio-bridge                                                │
│  Stable C API / thin JNI surface                            │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│ audio-engine                                                │
│  Transport · Queue handoff · Buffers · DSP graph ownership  │
│        ┌──────────────┐          ┌────────────────────┐     │
│        │ audio-juce   │          │ audio-android      │     │
│        │ EQ / effects │          │ Oboe / AAudio I/O  │     │
│        └──────────────┘          └────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### Ownership rules (preserve these)

| Concern | Owner |
|---------|--------|
| App lifecycle, permissions, notifications, MediaSession | Android (Kotlin) |
| Decoding orchestration, transport, real-time processing | Native engine (C++) |
| System media controls / headset / lock screen | Media3 adapter → engine |
| UI rendering / visualizer drawing | Compose (reads analysis snapshots only) |

### Real-time isolation (non-negotiable)

The audio callback must never:

- Wait on UI, disk, network, database, or blocking JNI.
- Allocate heap memory in the steady state.
- Hold locks with unbounded wait times.

Commands and analysis data move through **bounded, thread-safe** channels (e.g. atomics, lock-free SPSC rings, triple-buffered snapshots).

## Strengths

1. **Clear separation of concerns** — UI and audio fail independently; easier to test.
2. **One playback authority** — avoids dual Media3 + native pipelines fighting for the stream.
3. **Selective JUCE** — DSP without forcing JUCE GUI onto Android.
4. **Honest claims policy** — no bit-perfect / universal high-res promises until the full path is measured.
5. **Risk-first roadmap** — prove audio before investing in visuals.

## Technical inconsistencies & gaps

These are not blockers; they need explicit decisions before the related phase.

### 1. Decoding strategy is underspecified

Brief says “FFmpeg and/or dedicated decoders.” That choice affects binary size, licensing (LGPL/GPL implications), maintenance, and whether decode happens on a worker thread into a PCM ring.

**Recommendation:** Phase 2 starts with a **simple PCM/WAV path** to prove transport + Oboe. Then decide:

| Option | Pros | Cons |
|--------|------|------|
| FFmpeg (selected codecs) | Broad format coverage | Size, licensing diligence, build complexity |
| Android MediaCodec / extractor (via JNI helper thread) | Platform-maintained | Less control; format quirks |
| Hybrid | WAV/FLAC native; others via MediaCodec | Two paths to keep correct |

**Decision needed before Phase 2.4.** Do not block Phase 1–2.3 on it.

### 2. Gapless playback is listed as success, but design is not specified

Gapless needs decoder priming, buffer priming across track boundaries, and careful DSP state handling (EQ tails, limiter). Brief correctly calls it a “design,” not a free feature.

**Recommendation:** Design the queue/handoff API in Phase 2.5; implement true gapless as Phase 8.x after stable seek/track change.

### 3. Media3 “custom player” vs engine clock

Media3 expects a `Player` with position, duration, buffering states. The native engine owns the true clock. Drift between Media3 reported position and engine position will confuse seek UI and system controls if not defined.

**Recommendation:** Engine is sole source of truth for position/duration; Media3 adapter polls/snapshots at UI-safe rates (e.g. 10–20 Hz), never from the audio callback.

### 4. Sample-rate / format negotiation path unclear

Oboe opens a stream; DSP graph runs at a process rate; decoder may produce a different rate. Who resamples?

**Recommendation:** Document in Phase 1–2: prefer opening Oboe at a device-native rate; resample decoder → engine process rate on the decode/worker side, not in the callback’s hot path beyond a fixed resampler instance.

### 5. Analysis / visualizer data path not specified

Brief forbids coupling UI to the audio callback, but visualizer needs FFT/spectrum.

**Recommendation:** Engine writes **smoothed snapshots** into a triple buffer / atomics; UI/Visualizer reads latest snapshot on the main/render thread. FFT can run on a dedicated analysis thread fed from a tap — never block the callback on FFT completion if it cannot meet deadline (prefer lightweight time-domain meters first).

### 6. Module naming vs Gradle project layout

Brief modules (`android-app`, `audio-engine`, …) are logical. Gradle will need concrete project names and whether native code is one AAR/shared lib or multiple static libs linked into one `.so`.

**Recommendation:** See [04 — Repository structure](04-repository-structure.md). Prefer **one native shared library** (`libhowl_engine.so`) composed of CMake targets, rather than multiple JNI-loaded `.so` files early on.

### 7. JUCE licensing / commercial terms

Brief correctly flags this. Using JUCE DSP in a commercial app may require a paid license depending on version/terms.

**Decision needed before Phase 3.1:** confirm JUCE license for intended distribution; record in third-party notices process.

### 8. Persistence of EQ presets: Room vs engine-side

Presets are product state (Room) but must apply to the engine. Avoid storing two conflicting sources of truth.

**Recommendation:** Room owns preset definitions; engine receives applied parameter sets via bridge. Active preset ID lives in app preferences / Room; engine remains stateless regarding “which preset name.”

## Alternatives considered (and not recommended now)

| Alternative | Why not (yet) |
|-------------|----------------|
| Pure Media3 / ExoPlayer for playback | Fights the “serious native engine” differentiator; harder to own DSP graph |
| Full JUCE Android app | Wrong UI ownership; Compose is the product UI |
| Filament / full 3D engine for depth | Premature; Compose transforms + shaders first |
| Multiple simultaneous playback pipelines | Complexity and bugs; one authority |

## Open decisions checklist

Resolve these at the named phase — do not silently change architecture:

- [ ] Decoder strategy (Phase 2.4)
- [ ] JUCE commercial license confirmation (Phase 3.1)
- [ ] Gapless strategy detail (design 2.5, implement 8.x)
- [ ] Resampling ownership (Phase 2.2 / 2.4)
- [ ] Analysis thread vs in-callback light meters (Phase 7.1)
- [x] Min SDK / NDK versions (Phase 0.1) — minSdk 26, compile/target 35, NDK 28.2.13676358
- [x] App package / product name branding in code (Phase 0.2) — `com.howl.music`

## Architecture principles we will not change without discussion

1. Kotlin + Compose for Android UI; C++20 for the engine.
2. Selective JUCE for DSP only.
3. Oboe/AAudio for output.
4. Native engine is the single playback authority; Media3 adapts.
5. Real-time isolation of the audio callback.
6. Dependencies added only when a phase requires them.
