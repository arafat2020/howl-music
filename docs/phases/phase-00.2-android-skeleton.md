# Phase 0.2 — Android Gradle + Compose Skeleton

**Milestone:** 0 — Project foundation  
**Priority:** Foundational  
**Depends on:** 0.1  
**Status:** Not started

## Objective

Create a minimal Android application module that builds, installs, and shows a placeholder Compose screen — establishing Gradle/AGP/Compose baselines without native code or media features.

## Why it matters

Gives a runnable host for later JNI, MediaSession, and UI work. Validates the Kotlin/Compose toolchain before NDK complexity.

## Architecture

```text
android-app (application)
  MainActivity → setContent { Placeholder }
```

No ViewModels, navigation graph, or design system yet beyond a trivial theme stub if needed.

## Learning objectives

- Gradle version catalog basics.
- Application vs library modules.
- Compose entry point (`ComponentActivity` + `setContent`).
- Debug install via Gradle/Studio.

## Scope

**Included**
- Root Gradle files + wrapper.
- `android-app` module with Compose.
- Application ID / package decision (provisional `com.howl.music`).
- Empty/placeholder UI proving the app launches.
- `.gitignore` for Android/NDK.

**Excluded**
- NDK/CMake, Oboe, JUCE, Media3, Room, Coil.
- Design system tokens from the brief (Phase 6.1).
- Multi-module media/bridge split (Phase 0.4 can refine).

**Must not implement yet**
- Playback, permissions for music library, notifications.

## Implementation steps

1. Create Gradle root + `libs.versions.toml` with AGP, Kotlin, Compose BOM.
2. Create `android-app` with `MainActivity` and a simple “Howl” placeholder text.
3. Set compileSdk 35, targetSdk 35, minSdk per Phase 0.1 decision.
4. Run `assembleDebug` and install on device.
5. Add `.gitignore`.

## Expected project structure

```text
settings.gradle.kts
build.gradle.kts
gradle/libs.versions.toml
gradlew / gradlew.bat
android-app/
  build.gradle.kts
  src/main/AndroidManifest.xml
  src/main/java/.../MainActivity.kt
```

## Testing strategy

- `./gradlew :android-app:assembleDebug` succeeds.
- App launches on device; placeholder visible.
- Optional: trivial JUnit `example` test to prove test task works.

## Acceptance criteria

- [ ] Debug APK builds reproducibly via wrapper.
- [ ] App installs and launches on device.
- [ ] Compose placeholder renders.
- [ ] No native/media dependencies added.
- [ ] Package name and SDK levels recorded.

## Review checklist

- [ ] Understand `settings.gradle.kts` vs module `build.gradle.kts`.
- [ ] Version catalog pins versions.
- [ ] Manifest has only necessary activity entry.

## Potential pitfalls

- Mixing Groovy/KTS inconsistently.
- Enabling every Compose optional dependency early.
- Committing `local.properties`.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 0.3 — Native CMake + JNI smoke test |

**Approval required before Phase 0.3.**
