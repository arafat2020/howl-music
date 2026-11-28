# Phase 8.3 — USB DAC Investigation

**Milestone:** 8 — Device validation  
**Priority:** Optional  
**Depends on:** 8.2  
**Status:** Investigation documented. USB validation is blocked on hardware.

## Objective

Investigate USB audio / DAC behavior on available hardware: sample-rate negotiation, failure modes, and what can be honestly exposed in diagnostics — without promising universal DAC support.

## Why it matters

USB DAC support is a Poweramp-class expectation for some users, but Android fragmentation makes unverified claims harmful. This phase is research + guarded enablement.

## Architecture

```text
Oboe/AAudio device selection → optional USB endpoint
Diagnostics show negotiated params
Feature flag: experimental USB extras
```

## Learning objectives

- Android USB audio realities.
- Why “bit perfect” is a full-path claim.
- How to write evidence-based support matrices.

## Scope

**Included**
- Test on ≥1 USB DAC if hardware available (else document blocked).
- Notes on rates/formats observed.
- Any safe UX hints (select device / show rate).
- Explicit non-claims list.

**Excluded**
- Guaranteeing every DAC.
- Custom ASIO-like control panels.

**Must not implement yet**
- Marketing “USB DAC optimized” without matrix evidence.

## Implementation steps

1. Enumerate devices via platform APIs / Oboe.
2. Run controlled playback tests.
3. Record matrix in `docs/perf/usb-dac-matrix.md`.
4. Only then consider small UX improvements.

## Expected project structure

```text
docs/perf/usb-dac-matrix.md
```

## Testing strategy

- Multiple rates if DAC supports them.
- Hot-plug connect/disconnect behavior.
- Compare to built-in speaker path diagnostics.

## Acceptance criteria

- [x] Written investigation results (even if “hardware unavailable”).
- [x] No unverified product claims added.
- [x] Diagnostics show negotiated rate when known.
- [ ] Crash-free hotplug handling for tested devices.

## Review checklist

- [x] Results distinguish phone vs DAC limitations.
- [x] Follow-up work clearly optional.

## Potential pitfalls

- Assuming Desktop USB audio behavior equals Android.
- Chasing exclusive mode failures endlessly.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | USB DAC investigation and physical verification procedure. |
| Files created/modified | [USB DAC matrix](../perf/usb-dac-matrix.md). |
| Tests executed | 44.1 kHz and 48 kHz playback through the ARM64 API 36 emulator speaker route. |
| Results | Diagnostics distinguished source/stream rate from the reported hardware rate. |
| Known limitations | Physical phone and DAC availability blocked USB negotiation and hot-plug tests. |
| Decisions made | Keep USB-specific controls and claims out of the release until physical evidence is available. |
| Next phase | 8.4 — Device matrix & release checklist |

**Approval required before Phase 8.4.**
