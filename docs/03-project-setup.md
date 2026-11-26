# 03 — Project Setup Proposal

Establish a reproducible environment **before** application features. Add dependencies only when the corresponding phase needs them.

## Locked environment (Phase 0.1 — 25 November 2026)

Verified on this machine. Prefer these exact versions in Gradle/CMake until a later phase deliberately upgrades them.

| Tool | Locked version | Location / notes |
|------|----------------|------------------|
| Host OS | macOS 26.3 (arm64) | Darwin 25.3.0 |
| JDK | **17.0.21** (Homebrew OpenJDK 17) | `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` |
| Android SDK root | Homebrew command-line tools | `ANDROID_SDK_ROOT=/opt/homebrew/share/android-commandlinetools` |
| compileSdk / targetSdk | **35** | `platforms;android-35` installed (34 and 36 also present) |
| minSdk | **26** (provisional) | Reliable AAudio/Oboe path; NDK provides `aarch64-linux-android26-clang++` |
| Android NDK | **28.2.13676358** (r28c) | `$SDK/ndk/28.2.13676358`; Clang 19.0.1; C++20 OK |
| CMake (SDK) | **3.22.1** | `$SDK/cmake/3.22.1` |
| Build-Tools | **36.0.0** | Installed |
| platform-tools / adb | **37.0.1** | On PATH via SDK |
| Emulator package | **37.2.13** | System image: android-36 google_apis_playstore arm64-v8a |
| Git | 2.50.1 (Apple Git-155) | — |
| Android Studio | **Not installed** | CLI SDK/NDK sufficient for early phases; install Studio before heavy Compose UI work if desired |

### Device status (Phase 0.1)

| Item | Status |
|------|--------|
| `adb` | Works |
| Connected target | **Emulator only** — `emulator-5556` (`sdk_gphone64_arm64`, API 36 / Android 16) |
| Physical device | **None connected** |

**Accepted limitation:** Phase 0.1 proceeds with the emulator for install/launch checks. **Physical-device validation is required before accepting Phase 1.2 (Oboe / underruns / latency).** Emulators are not authoritative for realtime audio quality.

### Decisions locked in Phase 0.1

1. **minSdk = 26** (provisional for first credible release). Revisit only if product explicitly needs API 24–25 devices.
2. **compileSdk / targetSdk = 35** for the app skeleton (Phase 0.2). Platform 36 may be adopted later; do not churn without a reason.
3. **NDK 28.2.13676358** is the project NDK until a deliberate upgrade.
4. **CMake 3.22.1** (SDK package) is the native build CMake.

## Required development tools

| Tool | Baseline | Notes |
|------|----------|-------|
| JDK | **17** (locked 17.0.21 on this machine) | Required by modern Android Gradle Plugin |
| Android Studio | Optional early; recommended for Compose | Not installed here yet; CLI SDK is present |
| Android SDK | API **35** compile / target; min API **26** | See locked table above |
| Android NDK | **28.2.13676358** (r28c) | C++20 verified |
| CMake | **3.22.1** (SDK CMake package) | Matches AGP expectations |
| Gradle | **8.11.1** wrapper + version catalog | AGP **8.7.3**, Kotlin **2.0.21**, Compose BOM **2024.12.01** (Phase 0.2) |
| Git | Required | 2.50.1 present |
| Device / emulator | Physical device required from Phase 1.2 | Emulator OK for 0.x install smoke only |

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

# Native host GoogleTest (Phase 1.4) — no Android device required
./scripts/run-host-native-tests.sh
# Equivalent:
#   cmake -S native -B native/build-host -DHOWL_BUILD_TESTS=ON
#   cmake --build native/build-host --target howl_native_tests
#   ctest --test-dir native/build-host --output-on-failure

# Install on device
./gradlew :android-app:installDebug
```

**Host vs device:** `howl_native_tests` covers Engine lifecycle + pure math (`SineOsc`). Oboe I/O / underruns stay manual on emulator or physical device (Phases 1.2–1.3).

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
