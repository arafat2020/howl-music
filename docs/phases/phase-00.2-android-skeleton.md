# Phase 0.2 — Android Gradle + Compose Skeleton

**Milestone:** 0 — Project foundation  
**Priority:** Foundational  
**Depends on:** 0.1  
**Status:** Approved / complete

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

- [x] Debug APK builds reproducibly via wrapper.
- [x] App installs and launches on device.
- [x] Compose placeholder renders.
- [x] No native/media dependencies added.
- [x] Package name and SDK levels recorded.

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
| What was built | Minimal `android-app` with Gradle wrapper, version catalog, Compose placeholder screen showing “Howl” on brief dark canvas with acid-lime text. |
| Files created/modified | `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradlew*`, `gradle/wrapper/*`, `android-app/**`, `.gitignore`, `local.properties` (local only), `docs/evidence/phase-00.2-emulator.png`, this phase doc, `docs/README.md`, `docs/03-project-setup.md`. |
| Tests executed | `./gradlew :android-app:assembleDebug`; `./gradlew :android-app:testDebugUnitTest`; `./gradlew :android-app:installDebug` on `emulator-5556`; launched `MainActivity`; screenshot evidence. |
| Results | BUILD SUCCESSFUL. Unit smoke test passed. App installed and resumed on emulator; Compose placeholder visible (see `docs/evidence/phase-00.2-emulator.png`). |
| Known limitations | Emulator-only (no physical device). Android Studio not installed (CLI build path used). AGP warns about SDK XML version mismatch (cmdline-tools newer than AGP 8.7.3 expectation) — non-blocking. Offline emulator `emulator-5554` can make bare `adb` ambiguous; use `-s emulator-5556`. |
| Decisions made | Package `com.howl.music`. minSdk 26 / compile+target 35. AGP **8.7.3**, Kotlin **2.0.21**, Compose BOM **2024.12.01**, Gradle **8.11.1**. Kotlin Compose compiler plugin (not old `composeOptions`). No NDK/Media3/Room yet. |
| Next phase | 0.3 — Native CMake + JNI smoke test |

**Approval required before Phase 0.3.**
