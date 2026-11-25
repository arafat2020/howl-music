# 03 — Project Setup Proposal

Establish a reproducible environment **before** application features. Add dependencies only when the corresponding phase needs them.

## Required development tools

| Tool | Proposed baseline | Notes |
|------|-------------------|-------|
| JDK | **17** (Temurin or Oracle) | Required by modern Android Gradle Plugin |
| Android Studio | Latest stable | Compose + NDK tooling |
| Android SDK | API **35** compile / target; min API **26** (proposed) | Min 26 gives reliable AAudio/Oboe path; revisit if you need older devices |
| Android NDK | **r27+** (side-by-side in SDK Manager) | C++20 support |
| CMake | **3.22.1+** (SDK CMake package) | Matches AGP expectations |
| Gradle | Version catalog + AGP matching Studio stable | Wrapper committed in repo |
| Git | Required | — |
| Device / emulator | Physical device strongly preferred for audio | Emulators are weak for latency/underrun validation |

> **Open decision:** Min SDK 26 vs 24. Prefer 26 unless product requires older phones.

## C++ toolchain

- **Language:** C++20
- **Compiler:** NDK Clang
- **Build:** CMake via Gradle `externalNativeBuild` (or Prefab later if needed)
- **Test host:** GoogleTest running on **host** (macOS/Linux) where practical for pure DSP/transport unit tests; device tests for Oboe I/O

## Dependency introduction schedule

| Dependency | Introduce in | Why wait |
|------------|--------------|----------|
| Jetpack Compose BOM | Phase 0.2 | App skeleton |
| AndroidX Core / Lifecycle / ViewModel | Phase 0.2 / 4.x | As needed |
| Oboe | Phase 1.2 | Audio foundation |
| GoogleTest | Phase 1.4 | Native tests |
| Decoder libs / FFmpeg | Phase 2.4 | After transport proven |
| JUCE (DSP modules only) | Phase 3.1 | After PCM playback works |
| AndroidX Media3 | Phase 4.2 | After JNI control API exists |
| Room | Phase 5.1 | After playback + bridge |
| Coil | Phase 5.4 / 6.x | With artwork UI |
| Rive / Lottie / Filament / AGSL extras | Phase 7.x only if justified | Avoid early |

## Suggested local setup checklist

1. Install JDK 17; confirm `java -version`.
2. Install Android Studio; SDK Platform 35; NDK; CMake; build-tools.
3. Accept SDK licenses: `sdkmanager --licenses`.
4. Confirm `adb devices` sees a physical device.
5. Clone repo; open in Android Studio; use committed Gradle wrapper only.
6. (Later phases) Configure CMake + NDK paths via `local.properties` (never commit secrets).

## Build & test commands (target once skeleton exists)

```bash
# Android unit tests
./gradlew :android-app:testDebugUnitTest

# Assemble debug app
./gradlew :android-app:assembleDebug

# Native host tests (exact target name TBD in Phase 1.4)
./gradlew :audio-engine:runHostTests
# or: cmake --build build-host --target howl_engine_tests && ./howl_engine_tests

# Install on device
./gradlew :android-app:installDebug
```

Exact module names follow [04 — Repository structure](04-repository-structure.md).

## Debugging workflow

| Layer | Approach |
|-------|----------|
| Compose UI | Layout Inspector, Logcat, breakpoint ViewModels |
| MediaSession / service | `adb shell dumpsys media_session`, notification shade, headset keys |
| JNI boundary | Explicit error codes; avoid chatty per-buffer logs |
| Native engine | `adb logcat` with tagged logger **off** the audio thread; systrace/perfetto for underruns |
| DSP correctness | Host GoogleTest with known impulses / sine / step responses |
| Output path | Engine diagnostics: sample rate, frames/callback, underrun count, route label |

## Reproducibility rules

- Commit **Gradle wrapper** and version catalog.
- Pin dependency versions; no floating `+` versions.
- Document NDK/CMake versions in this file when Phase 0 locks them.
- Keep a **THIRD_PARTY_NOTICES** process from Phase 3 onward (JUCE/FFmpeg/codecs).
- Do not commit `local.properties`, keystores, or machine-specific NDK paths.

## What setup deliberately excludes

- Full FFmpeg / JUCE / Media3 wiring on day one.
- CI matrix (add when Phase 1.4 / 0.2 builds are green).
- Release signing and Play Store packaging (late hardening).
