# Phase 1.1 — C++ Engine Skeleton & Lifecycle

**Milestone:** 1 — Audio foundation  
**Priority:** Foundational  
**Depends on:** 0.4  
**Status:** Not started

## Objective

Define the C++ engine object with explicit create/destroy/start/stop lifecycle and clear ownership — without opening an audio device yet.

## Why it matters

Lifecycle bugs (double-start, use-after-free, stop from wrong thread) are endemic in audio apps. Establishing RAII and state rules first makes Oboe integration safer.

## Architecture

```text
HowlEngine (C++)
  states: Idle → Prepared → Running → Stopping → Idle
  owns: future output + graph pointers (null for now)
```

Kotlin/JNI may wrap create/destroy only; no stream.

## Learning objectives

- RAII and unique ownership (`std::unique_ptr`).
- Explicit state machines vs ad-hoc bool flags.
- Why audio engines need deterministic shutdown.

## Scope

**Included**
- `Engine` class with non-copyable semantics.
- Thread-safe enough start/stop **policy** documented (actual audio thread later).
- Error codes / results for invalid transitions.
- Unit-testable pure logic where possible.

**Excluded**
- Oboe stream (1.2).
- Decoders, DSP, JNI expansion beyond thin hooks.

**Must not implement yet**
- Real-time callback body with processing.

## Implementation steps

1. Define `Engine` API in headers under `native/audio-engine`.
2. Implement state transitions with clear errors.
3. Wire create/destroy through bridge (optional thin JNI).
4. Document which thread may call start/stop (control thread).

## Expected project structure

```text
native/audio-engine/
  include/howl/engine.h
  src/engine.cpp
```

## Testing strategy

- Host or device tests: create → start → start (fail) → stop → destroy.
- ASAN/HWASAN later; at least no crash on double-stop.

## Acceptance criteria

- [ ] Engine lifecycle API compiles into `libhowl_engine.so`.
- [ ] Invalid transitions return errors (no crash).
- [ ] Ownership documented; no raw owning pointers without rationale.
- [ ] No Oboe dependency required to pass this phase.

## Review checklist

- [ ] Understand why Engine is non-copyable.
- [ ] Know the allowed state transitions.
- [ ] Shutdown path is deterministic.

## Potential pitfalls

- Calling start/stop from random threads without a rule.
- Mixing JNI global refs into Engine early.
- Hidden singletons that complicate testing.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 1.2 — Oboe output + test signal |

**Approval required before Phase 1.2.**
