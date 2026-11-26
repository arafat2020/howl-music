# Phase 0.4 — Module Layout & Build Wiring

**Milestone:** 0 — Project foundation  
**Priority:** Foundational  
**Depends on:** 0.3  
**Status:** Approved / complete

## Objective

Reshape the repo into the agreed Gradle/CMake module boundaries (`android-app`, `android-media` stub, `audio-bridge`, `native/*`) with a single `libhowl_engine.so` and clean dependency edges.

## Why it matters

Later phases add Media3, Room, and DSP. Clear boundaries prevent circular dependencies and “UI imports Oboe headers” mistakes.

## Architecture

```text
android-app  →  android-media (stub) → audio-bridge → libhowl_engine.so
                     │
                     └── (empty MediaSession placeholders OK)
```

`android-media` may be an empty shell with a README/TODO — no Media3 dependency yet.

## Learning objectives

- Gradle project dependencies (`implementation(project(...))`).
- Why bridge is the only module that should load the `.so`.
- CMake target graph vs Gradle module graph.

## Scope

**Included**
- Create modules per [04-repository-structure.md](../04-repository-structure.md).
- Move JNI smoke into `audio-bridge`.
- Scaffold `native/audio-engine` CMake target (empty/stub OK).
- `android-app` depends on bridge (and optionally media stub).
- Update docs if structure differs for a good reason (explain why).

**Excluded**
- Oboe/JUCE/Media3/Room implementation.
- Real engine lifecycle (Phase 1.1).

**Must not implement yet**
- Playback APIs beyond smoke test.

## Implementation steps

1. Add `audio-bridge` Android library with CMake.
2. Move smoke JNI from app → bridge; expose Kotlin façade.
3. Add `native/` CMake structure; link stub `audio-engine` into bridge.
4. Add empty `android-media` module.
5. Fix `settings.gradle.kts`; verify app still launches with smoke result.

## Expected project structure

```text
android-app/
android-media/
audio-bridge/
native/
  CMakeLists.txt
  audio-engine/
  audio-android/   # empty placeholder dir OK
  audio-juce/      # empty placeholder dir OK
```

## Testing strategy

- `./gradlew :android-app:assembleDebug` green.
- Smoke JNI still works through bridge façade.
- Confirm app does not contain duplicate `.so` builds from multiple modules.

## Acceptance criteria

- [x] Module graph matches agreed structure (or documented delta).
- [x] Single native library load path.
- [ ] App launches; smoke test passes. *(build+APK verified; confirm on emulator when available)*
- [x] No feature dependencies introduced early.

## Review checklist

- [ ] Can explain who depends on whom.
- [ ] No Compose code in `audio-bridge`.
- [ ] Native placeholders do not pull JUCE yet.

## Potential pitfalls

- Circular Gradle deps (`media` ↔ `app`).
- Two modules both defining `externalNativeBuild` for the same sources.
- Over-creating empty Java packages.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Multi-module layout: `android-app` → `android-media` (stub) → `audio-bridge` → `libhowl_engine.so`. Pure C++ stub in `native/audio-engine` linked by bridge CMake. Placeholders for `audio-android` / `audio-juce` / `tests`. |
| Files created/modified | `settings.gradle.kts`, `audio-bridge/**`, `android-media/**`, `native/**`, `android-app/build.gradle.kts` + `MainActivity.kt` (removed app-local cpp/NativeSmoke), version catalog (+ android-library plugin), this phase doc, `docs/README.md`. |
| Tests executed | `./gradlew :android-app:assembleDebug` — SUCCESS. APK contains exactly one `libhowl_engine.so` per ABI (`arm64-v8a`, `x86_64`). No emulator connected this session for install. |
| Results | Module graph and single `.so` load path verified at build/package level. |
| Known limitations | Device UI confirm still manual when emulator is up. `android-media` is a stub (no Media3). Engine is still smoke-only. |
| Decisions made | Bridge is sole `externalNativeBuild` / `loadLibrary` owner. Media uses `api(project(":audio-bridge"))` so app can call `NativeSmoke`. Native tree is CMake-only (not a Gradle module). Kotlin packages: `com.howl.music.bridge`, `com.howl.music.media`. |
| Next phase | 1.1 — C++ engine skeleton & lifecycle |

### Device confirm (optional)

```bash
adb devices
./gradlew :android-app:installDebug
adb shell am start -n com.howl.music/.MainActivity
```

Expect: `howl_engine/0.0.1-smoke`, `1+1=2`, and media stub line.

**Approval required before Phase 1.1.**
