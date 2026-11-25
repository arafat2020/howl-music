# Phase 0.3 — Native CMake + JNI Smoke Test

**Milestone:** 0 — Project foundation  
**Priority:** Foundational  
**Depends on:** 0.2  
**Status:** Not started

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

- [ ] Native library loads on device.
- [ ] JNI call returns expected value.
- [ ] Build is reproducible via Gradle.
- [ ] No audio I/O code present.

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
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 0.4 — Module layout & build wiring |

**Approval required before Phase 0.4.**
