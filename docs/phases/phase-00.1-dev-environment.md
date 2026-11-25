# Phase 0.1 — Development Environment

**Milestone:** 0 — Project foundation  
**Priority:** Foundational  
**Depends on:** —  
**Status:** Approved / complete

## Objective

Verify and document a reproducible local toolchain (JDK, Android SDK/NDK, CMake, device) so later phases build consistently.

## Why it matters

Audio + NDK projects fail mysteriously when toolchain versions drift. Locking the environment first prevents false negatives during engine work.

## Architecture

No application architecture yet. This phase produces an **environment contract** referenced by all later builds.

## Learning objectives

- Difference between JDK, Android SDK, NDK, and CMake roles.
- Why min SDK choice affects AAudio/Oboe availability.
- How to confirm a physical device is usable for audio validation.

## Scope

**Included**
- Install/verify JDK 17.
- Install/verify Android Studio, SDK Platform (proposed 35), NDK r27+, CMake 3.22.1+.
- Confirm `adb` sees a device.
- Record exact versions in a short env notes section (update [03-project-setup.md](../03-project-setup.md)).
- Decide provisional min SDK (recommend 26).

**Excluded**
- Creating the Android project.
- Adding Oboe, JUCE, Media3, Room.
- CI setup.

**Must not implement yet**
- Any app or native source code.

## Implementation steps

1. Check `java -version` → expect 17.x.
2. Open SDK Manager → install Platform 35, NDK, CMake, platform-tools.
3. Run `sdkmanager --licenses` (or accept via Studio).
4. Connect device → `adb devices`.
5. Write versions into project setup doc / this completion report.
6. Confirm min SDK provisional decision (26 vs other).

## Expected project structure

```text
docs/03-project-setup.md   # updated with locked versions
docs/phases/phase-00.1-... # this file; completion report filled
```

No source tree required yet.

## Testing strategy

Manual verification only: commands succeed; device authorized; NDK path visible to Studio.

## Acceptance criteria

- [x] JDK 17 confirmed.
- [x] SDK Platform + NDK + CMake installed and version-recorded.
- [x] At least one physical Android device (or explicitly accepted emulator limitation) available via `adb`.
- [x] Provisional min/compile/target SDK written down.
- [x] No application code committed as part of this phase.

## Review checklist

- [ ] Versions are pinned/recorded, not “latest.”
- [ ] Emulator-only limitation acknowledged if no device.
- [ ] Min SDK rationale understood.

## Potential pitfalls

- Multiple JDKs on PATH → Gradle picks wrong one.
- NDK not selected in Studio project structure (later).
- Assuming emulator validates underruns/latency (it usually does not).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Environment contract only — verified local toolchain and recorded locked versions. No application or native source code. |
| Files created/modified | `docs/03-project-setup.md` (locked environment section); `docs/phases/phase-00.1-dev-environment.md` (this report); `docs/README.md` (project state). |
| Tests executed | Manual: `java -version`; SDK package inventory via `sdkmanager --list_installed`; NDK `aarch64-linux-android26-clang++ -std=c++20` compile smoke; `adb devices -l`; emulator `getprop` for API/ABI. |
| Results | JDK 17.0.21 OK. Platforms 34/35/36 present. NDK 28.2.13676358 (r28c) OK with C++20. CMake 3.22.1 OK. Licenses present under SDK. `adb` sees emulator-5556 (API 36, arm64). |
| Known limitations | **No physical device connected** — emulator-only accepted for Phase 0.x install/launch; not valid for Phase 1.2+ audio underrun/latency acceptance. **Android Studio not installed** — CLI SDK/NDK is enough for early phases. Host `cmake` not on PATH (SDK CMake is used). `/usr/libexec/java_home` does not see Homebrew JDK (Gradle should use `JAVA_HOME`). |
| Decisions made | minSdk **26**; compileSdk/targetSdk **35**; NDK **28.2.13676358**; CMake **3.22.1**; proceed without Android Studio for now. |
| Next phase | 0.2 — Android Gradle + Compose skeleton |

**Approval required before Phase 0.2.**
