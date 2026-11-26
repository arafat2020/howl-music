# Phase 1.4 — GoogleTest Harness

**Milestone:** 1 — Audio foundation  
**Priority:** Foundational  
**Depends on:** 1.1 (can parallelize with 1.2–1.3)  
**Status:** Approved

## Objective

Stand up GoogleTest for native code on the host (and document device limitations), with initial tests for engine lifecycle and pure DSP/math helpers.

## Why it matters

Real-time audio code needs fast regression tests that do not require wearing headphones for every change.

## Architecture

```text
native/tests (GoogleTest)
  → links audio-engine (and later audio-juce)
Host CMake or Gradle task runs tests offline
```

Oboe device I/O tests remain manual/device-only for now.

## Learning objectives

- GoogleTest basics (`TEST`, `EXPECT_*`).
- What belongs in host tests vs device tests.
- Keeping engine logic testable without Android.

## Scope

**Included**
- GoogleTest fetch/content or pinned submodule — document choice.
- Lifecycle tests for Engine.
- One example numeric test (e.g. sine next-sample determinism helper).
- Convenient run command documented in setup doc.

**Excluded**
- Full CI matrix.
- Instrumenting Oboe on host (usually not available the same way).

**Must not implement yet**
- Decoder corpus suites (Phase 2).

## Implementation steps

1. Add GoogleTest to native build.
2. Write lifecycle tests.
3. Add Gradle or script target to run host tests.
4. Update [03-project-setup.md](../03-project-setup.md) with the command.

## Expected project structure

```text
native/tests/
  CMakeLists.txt
  test_engine_lifecycle.cpp
  test_sine_osc.cpp
scripts/run-host-native-tests.sh
```

## Testing strategy

- Running the test binary returns 0.
- Intentionally break a transition → test fails.

## Acceptance criteria

- [x] Host tests run without an Android device.
- [x] Lifecycle tests cover happy path + invalid transition.
- [x] Documented how to run tests.
- [x] Oboe I/O not falsely “unit tested” on host.

## Review checklist

- [ ] Know which modules link into tests.
- [ ] Understand host vs device test boundaries.

## Potential pitfalls

- Pulling Android JNI into host tests.
- Network-flaky FetchContent without pin.
- Tests depending on wall-clock sleeps unnecessarily.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Host GoogleTest target `howl_native_tests` (13 cases): Engine lifecycle + `SineOsc` determinism. `scripts/run-host-native-tests.sh`. Tone path now uses shared `howl::SineOsc`. |
| Files created/modified | `native/tests/**`, `native/CMakeLists.txt`, `audio-engine/CMakeLists.txt`, `sine_osc.h`, `oboe_output.cpp`, `scripts/run-host-native-tests.sh`, `docs/03-project-setup.md`, phase/docs README. |
| Tests executed | `./scripts/run-host-native-tests.sh` → **13/13 PASS**. `:android-app:assembleDebug` SUCCESS. |
| Results | Host suite green; no device required. |
| Known limitations | First configure needs network for FetchContent. Oboe I/O still manual. No CI matrix yet. |
| Decisions made | GoogleTest via **FetchContent**, pinned tag **`v1.15.2`** (not a submodule). `gtest_discover_tests` + top-level `enable_testing()`. Host diagnostics asserted **zero** (no fake Oboe). |
| Next phase | 2.1 — Decoder abstraction + WAV/PCM |

**Approval required before Phase 2.1 (or continue parallel 1.x if still open).**
