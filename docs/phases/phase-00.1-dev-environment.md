# Phase 0.1 — Development Environment

**Milestone:** 0 — Project foundation  
**Priority:** Foundational  
**Depends on:** —  
**Status:** Not started

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

- [ ] JDK 17 confirmed.
- [ ] SDK Platform + NDK + CMake installed and version-recorded.
- [ ] At least one physical Android device (or explicitly accepted emulator limitation) available via `adb`.
- [ ] Provisional min/compile/target SDK written down.
- [ ] No application code committed as part of this phase.

## Review checklist

- [ ] Versions are pinned/recorded, not “latest.”
- [ ] Emulator-only limitation acknowledged if no device.
- [ ] Min SDK rationale understood.

## Potential pitfalls

- Multiple JDKs on PATH → Gradle picks wrong one.
- NDK not selected in Studio project structure (later).
- Assuming emulator validates underruns/latency (it usually does not).

## Completion report

_Fill after execution — do not mark complete until acceptance criteria pass._

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 0.2 — Android Gradle + Compose skeleton |

**Approval required before Phase 0.2.**
