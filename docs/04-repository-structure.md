# 04 — Proposed Repository & Module Structure

Logical modules from the brief are preserved. Gradle layout below is the concrete mapping.

## Top-level layout

```text
howl-music/
├── docs/                          # Engineering docs (this folder)
├── premium_music_player_product_brief.pdf
├── README.md                      # User-facing project readme (later)
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/
│   └── libs.versions.toml         # Version catalog
├── android-app/                   # Compose application module
├── android-media/                 # MediaSession / Media3 integration library
├── audio-bridge/                  # JNI + stable native API (Android library w/ CMake)
└── native/                        # Pure C++ sources (CMake project)
    ├── CMakeLists.txt
    ├── audio-engine/
    ├── audio-juce/
    ├── audio-android/
    └── tests/                     # GoogleTest
```

### Why `native/` instead of many Android modules for C++

Early on, **one shared library** (`libhowl_engine.so`) built from multiple CMake targets is simpler than multiple JNI `.so` loads. Logical boundaries (`audio-engine`, `audio-juce`, `audio-android`) remain as CMake libraries linked into the bridge module.

If we later need stronger isolation (e.g. optional JUCE feature flag), we can split AAR packaging without changing public Kotlin APIs.

## Module responsibilities

| Module | Type | Scope | Must not own |
|--------|------|-------|--------------|
| `android-app` | Application | Compose UI, navigation, design system, ViewModels, DI wiring | Real-time audio, MediaSession implementation details |
| `android-media` | Android library | `MediaSessionService`, Media3 custom `Player` adapter, notifications, audio focus | Compose screens, DSP algorithms |
| `audio-bridge` | Android library + NDK | JNI entry points, Kotlin façade, CMake → `libhowl_engine.so` | UI, Room, Coil |
| `native/audio-engine` | CMake static/shared lib | Transport, buffers, queue handoff, DSP graph ownership | Android framework APIs |
| `native/audio-juce` | CMake lib | JUCE DSP wrappers (EQ, etc.) | GUI, file choosers |
| `native/audio-android` | CMake lib | Oboe stream, device/output integration | App lifecycle |
| `native/tests` | Host + optional device | GoogleTest | — |

## Expected Kotlin package sketch (later)

```text
com.howl.music
  app/          # Application, MainActivity
  ui/           # Screens, theme, components
  library/      # Browse ViewModels (phase 5–6)
  player/       # Now playing UI state (phase 6)
```

```text
com.howl.music.media
  session/      # MediaSessionService
  player/       # Media3 adapter
```

```text
com.howl.music.bridge
  HowlEngine    # Kotlin API over JNI
```

> Package name is a Phase 0.2 decision; `com.howl.music` is a placeholder aligned with the repo name.

## Native public API shape (intent)

Expose a **small, explicit** C API (wrapped by JNI), for example:

- Lifecycle: `engine_create`, `engine_destroy`, `engine_start`, `engine_stop`
- Transport: `play`, `pause`, `seek`, `load` / queue ops
- Parameters: set EQ band / preamp with smoothed application
- Diagnostics: underrun count, sample rate, frames per burst
- Analysis: copy latest snapshot (non-blocking)

Avoid per-sample or per-buffer JNI chatter.

## Persistence & assets (later phases)

```text
android-app/
  src/main/
    java|kotlin/...
    res/
    assets/          # optional demo tones for early tests
```

Room schemas live under app or a future `android-data` module — **do not split a data module until Phase 5** if a single app module stays clear.

## Independence goals

| Goal | How |
|------|-----|
| Native tests without APK | Host CMake + GoogleTest for engine/DSP |
| UI builds without full DSP | Bridge can ship a stub engine behind interface in early UI phases if needed — prefer real engine once Phase 4 exists |
| Media layer testable | `android-media` depends on bridge API, not Compose |

## What we will not do in structure

- No Flutter/React Native / KMP UI modules.
- No second playback engine module “for demos.”
- No premature micro-module explosion (`ui-nowplaying`, `ui-library`, …) until the design system exists.
