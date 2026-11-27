# Phase 4.1 — Stable JNI / Kotlin Engine API

**Milestone:** 4 — Android integration  
**Priority:** Foundational  
**Depends on:** 2.3, 1.3 (3.4 recommended if EQ exposed)  
**Status:** Implemented and verified

## Objective

Publish a small, documented Kotlin façade over JNI for lifecycle, transport, queue basics, diagnostics, and EQ apply — suitable for Media3 and UI to consume.

## Why it matters

This is the contract boundary. If it’s chatty or leaky, Media3 and Compose will both amplify the mistakes.

## Architecture

```text
HowlEngine (Kotlin)
  synchronous commands on one serial control thread
        │ JNI
        ▼
C API / Engine*
```

Rules: no per-buffer JNI; coarse commands; snapshot getters.

## Learning objectives

- Designing JNI as a stable façade.
- Native resource lifetime vs JVM GC (`Closeable` / `EngineHandle`).
- Error propagation across the boundary.

## Scope

**Included**
- Kotlin API: create/destroy/start/stop, play/pause/seek, position, queue ops, diagnostics, applyEqState.
- Explicit thread rules in KDoc.
- Basic instrumentation tests calling API.
- Deprecate ad-hoc smoke-only methods or fold them in.

**Excluded**
- Media3 (4.2).
- Room.
- Compose design system.

**Must not implement yet**
- Calling into engine from Compose draw loops beyond polling snapshots.

## Implementation steps

1. Freeze C header surface for control ops.
2. Implement JNI glue carefully (exception safety).
3. Kotlin `HowlEngine` with `Closeable`.
4. Wire debug UI to façade only.
5. Document API in `docs/api/howl-engine-kotlin.md` (short).

## Expected project structure

```text
audio-bridge/src/main/java/.../HowlEngine.kt
audio-bridge/src/main/cpp/jni_api.cpp
docs/api/howl-engine-kotlin.md
```

## Testing strategy

- Android instrumentation: create → load wav → play → pause → seek → close.
- Stress destroy while playing.

## Acceptance criteria

- [x] Documented Kotlin API covers lifecycle, transport, queue, diagnostics, and DSP.
- [x] Clean Android emulator install loads and exercises the native library without linkage errors.
- [x] Instrumentation covers close during playback, close near EOF, and restart after EOF.
- [x] Kotlin receives control results and snapshots. Audio buffers stay native.

## Review checklist

- [ ] Understand who owns the native pointer.
- [ ] Know which methods are UI-thread safe.

## Potential pitfalls

- Global singleton engine hard to test.
- Forgetting to release on process death edge cases.
- Oversized “god” API — keep focused.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Documented Kotlin façade, C-compatible control header, consolidated JNI entry points, and Android lifecycle instrumentation. |
| Files created/modified | Kotlin bridge and compatibility layer, JNI and C API headers, native engine teardown, Android test runner, and API documentation. |
| Tests executed | C11 header compile check, 62 host tests in each JUCE configuration, Android debug build and unit test, and three instrumentation passes on a clean emulator install under an isolated application ID. |
| Results | Instrumentation loaded a WAV with a Unicode filename, paused, sought, applied and reset EQ, and covered 36 destroy/restart stress iterations across the three passes. |
| Known limitations | Android runtime verification used the arm64 emulator. |
| Decisions made | Use explicit single-thread ownership and synchronous commands. The owning thread joins the playback watcher before releasing or replacing the stream. Keep deprecated Kotlin smoke entry points for compatibility. |
| Next phase | 4.2 — Media3 custom player adapter |

**Approval required before Phase 4.2.**
