# Phase 0.4 — Module Layout & Build Wiring

**Milestone:** 0 — Project foundation  
**Priority:** Foundational  
**Depends on:** 0.3  
**Status:** Not started

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

- [ ] Module graph matches agreed structure (or documented delta).
- [ ] Single native library load path.
- [ ] App launches; smoke test passes.
- [ ] No feature dependencies introduced early.

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
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 1.1 — C++ engine skeleton & lifecycle |

**Approval required before Phase 1.1.**
