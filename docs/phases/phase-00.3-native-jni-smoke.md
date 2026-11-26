# Phase 0.3 — Native CMake + JNI Smoke Test

**Milestone:** 0 — Project foundation  
**Priority:** Foundational  
**Depends on:** 0.2  
**Status:** Approved / complete

## Objective

Prove the NDK/CMake/JNI pipeline: Kotlin calls a native function and receives a known result (e.g. version string or `1+1`).

## Why it matters

Most native audio bugs are compounded by broken load paths. A smoke test isolates “can we call C++?” from “does Oboe work?”

## Architecture

```text
MainActivity / small Bridge object
        │ System.loadLibrary("howl_engine")
        ▼
JNI_OnLoad / JNI method
        ▼
trivial C++ function
```

No audio streams. Prefer placing native code where Phase 0.4 will become `audio-bridge`, even if temporarily inside `android-app`.

## Learning objectives

- `System.loadLibrary` and JNI method naming.
- CMake `add_library` SHARED + Gradle `externalNativeBuild`.
- ABI filters (`arm64-v8a` first on modern devices).
- RAII mindset preview (even for tiny code).

## Scope

**Included**
- CMakeLists.txt building `howl_engine`.
- One JNI export (e.g. `nativeAdd` or `nativeEngineVersion`).
- Kotlin call site displaying result on the placeholder UI.
- abiFilters: at least `arm64-v8a` (add `x86_64` if emulator needed).

**Excluded**
- Oboe, threads, buffers, JUCE.
- Multi-module split (can wait for 0.4).

**Must not implement yet**
- Audio callbacks, MediaSession, file decoding.

## Implementation steps

1. Add NDK block + CMake to the module that will own the `.so`.
2. Implement tiny C++ + JNI glue.
3. Load library safely; surface errors if load fails.
4. Display returned value in Compose UI.
5. Confirm `.so` packaged in APK (`apk analyze` or Studio APK analyzer).

## Expected project structure

```text
android-app/ (or audio-bridge/)
  src/main/cpp/
    CMakeLists.txt
    howl_jni_smoke.cpp
  src/main/java/.../NativeSmoke.kt
```

## Testing strategy

- Install debug build; UI shows expected native result.
- Rotate device / relaunch; still works.
- Fail-path: temporarily wrong library name → clear error (then restore).

## Acceptance criteria

- [ ] Native library loads on device. *(APK contains `libhowl_engine.so`; confirm on emulator UI)*
- [ ] JNI call returns expected value. *(expect `howl_engine/0.0.1-smoke` and `1+1=2`)*
- [x] Build is reproducible via Gradle.
- [x] No audio I/O code present.

## Review checklist

- [ ] Understand JNI naming / `extern "C"`.
- [ ] Know where CMake output lands.
- [ ] No logging spam; load happens once.

## Potential pitfalls

- Forgetting `extern "C"` → UnsatisfiedLinkError.
- Building only for wrong ABI.
- Blocking UI with heavy native work (not an issue yet — keep it trivial).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | `libhowl_engine.so` via CMake; JNI exports `nativeEngineVersion` + `nativeAdd`; Kotlin `NativeSmoke` façade; Compose shows native status under “Howl”. |
| Files created/modified | `android-app/src/main/cpp/CMakeLists.txt`, `howl_jni_smoke.cpp`, `NativeSmoke.kt`, `MainActivity.kt`, `android-app/build.gradle.kts` (ndkVersion + externalNativeBuild + abiFilters). |
| Tests executed | `./gradlew :android-app:assembleDebug` (CMake arm64-v8a + x86_64). APK inspect: `lib/arm64-v8a/libhowl_engine.so` and `lib/x86_64/libhowl_engine.so` present. Device install blocked this session by emulator/adb flakiness. |
| Results | Native pipeline compiles and packages correctly. **Please run install on your emulator to confirm UI text.** |
| Known limitations | Native code still lives in `android-app` (moves to `audio-bridge` in 0.4). Emulator cold-boot/adb was unreliable during agent verification. |
| Decisions made | Library name `howl_engine`. ABIs `arm64-v8a` + `x86_64`. C++20. Load errors surfaced in UI instead of crashing. Prefer `ndkVersion` over deprecated `ndk.dir`. |
| Next phase | 0.4 — Module layout & build wiring |

### Device confirm (you run)

```bash
cd /Users/betopia/Desktop/Arafat/howl-music
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export GRADLE_USER_HOME=/Volumes/2TB/gradle-home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools

# Start Pixel_API_36 from Device Manager /:
# emulator -avd Pixel_API_36 -no-snapshot-load
adb devices   # wait until … device
./gradlew --stop
./gradlew :android-app:installDebug
adb shell am start -n com.howl.music/.MainActivity
```

Expected on screen: **Howl**, then `howl_engine/0.0.1-smoke` and `1+1=2`.

**Approval required before Phase 0.4.** (Approve after you see the native lines, or ask me to retry install once your emulator is stable.)
