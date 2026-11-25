# Phase 3.1 — JUCE Integration & DSP Graph Skeleton

**Milestone:** 3 — DSP graph  
**Priority:** Foundational  
**Depends on:** 2.3 + JUCE license confirmation  
**Status:** Not started

## Objective

Integrate JUCE **DSP-only** (no JUCE GUI) and introduce a modular processing graph placeholder in the engine that can host future EQ/effects.

## Why it matters

Selective JUCE is a core architecture decision. Doing it after PCM playback works reduces integration risk and forces license diligence before deep investment.

## Architecture

```text
Engine callback path:
  ring.pop → ProcessGraph.process(buffer) → output

ProcessGraph
  ordered nodes (initially bypass / identity)
```

JUCE headers used inside `audio-juce` wrappers; engine depends on wrappers, not sprawling JUCE includes everywhere.

## Learning objectives

- JUCE as library vs application framework.
- DSP graph / node chain concept.
- License obligations for commercial distribution.

## Scope

**Included**
- Confirm JUCE license fit; record decision.
- Build wiring for JUCE modules needed for DSP.
- `ProcessGraph` with bypass identity node.
- Measure CPU cost of empty graph vs raw path.

**Excluded**
- EQ bands (3.3), preamp (3.2).
- JUCE AudioDeviceManager / GUI.

**Must not implement yet**
- Allocating graph nodes inside the callback.

## Implementation steps

1. License check → write short decision note.
2. Add JUCE to CMake (module subset).
3. Implement graph skeleton + bypass.
4. Host test: process buffer in == out for identity.
5. Device: confirm no audible regression vs pre-graph.

## Expected project structure

```text
native/audio-juce/
  include/howl/dsp/process_graph.h
  src/process_graph.cpp
docs/decisions/juce-license.md
```

## Testing strategy

- Identity graph bit-exact (or float-eq) test.
- Device listen test with diagnostics CPU if available.

## Acceptance criteria

- [ ] JUCE license decision recorded.
- [ ] Graph runs in realtime path without allocations.
- [ ] Identity tests pass.
- [ ] No JUCE GUI linked.

## Review checklist

- [ ] Know which JUCE modules are linked.
- [ ] Understand graph prepare(sampleRate, blockSize) lifecycle.

## Potential pitfalls

- Accidentally enabling JUCE GUI/OS wrappers.
- prepare() not called on sample rate change.
- Heavy dynamic allocation on graph rebuild in callback.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 3.2 — Preamp / gain |

**Approval required before Phase 3.2.**
