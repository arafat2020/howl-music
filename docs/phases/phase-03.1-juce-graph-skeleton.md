# Phase 3.1 — JUCE Integration & DSP Graph Skeleton

**Milestone:** 3 — DSP graph  
**Priority:** Foundational  
**Depends on:** 2.3 + JUCE license confirmation  
**Status:** Implemented — awaiting your approval

## Objective

Integrate JUCE **DSP-only** (no JUCE GUI) and introduce a modular processing graph placeholder in the engine that can host future EQ/effects.

## Why it matters

Selective JUCE is a core architecture decision. Doing it after PCM playback works reduces integration risk and forces license diligence before deep investment.

## Architecture

```text
Engine callback path:
  ring.pop → ProcessGraph.process(buffer) → output

ProcessGraph
  ordered nodes (initially unity Gain / identity)
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

- [x] JUCE license decision recorded.
- [x] Graph runs in realtime path without allocations.
- [x] Identity tests pass.
- [x] No JUCE GUI linked.

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
| What was built | `howl_audio_juce` + `ProcessGraph` (prepare/reset/bypass/process). Host default `HOWL_WITH_JUCE=ON` FetchContent JUCE 8.0.6 modules `juce_core` / `juce_audio_basics` / `juce_dsp`. Android default OFF (same API, identity). Engine prepares graph on File start; Oboe ring path pops then `process()`. |
| Files created/modified | `audio-juce/*`, `juce-license.md`, `oboe_output.*`, `engine.*`, tests, CMake, architecture review |
| Tests executed | Host **50/50** (ProcessGraph identity + CPU sanity; backend `juce_dsp`). Android `:installDebug` with ring→graph path (`HOWL_WITH_JUCE=OFF` identity). |
| Results | Identity float-eq pass; empty-graph CPU well under ceiling; APK installs. |
| Known limitations | Android does not fetch JUCE yet (`HOWL_WITH_JUCE=OFF`). No preamp/EQ nodes. |
| Decisions made | Commercial JUCE license required before proprietary store ship. Version `howl_engine/0.8.0-dsp-graph`. |
| Next phase | **3.2** — Preamp / gain |

## Verification before Phase 3.2

The Phase 3.1 baseline passed all 50 host tests with JUCE enabled, including identity, bypass, and the graph CPU sanity check. The license decision is recorded, the JUCE target uses DSP modules without GUI modules, and the Oboe file callback calls the graph after reading from the ring buffer. The original report records an Android install. A fresh device listening check was unavailable.

**Approval required before Phase 3.2.**
