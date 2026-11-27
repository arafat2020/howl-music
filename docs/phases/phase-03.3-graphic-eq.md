# Phase 3.3 — Graphic EQ

**Milestone:** 3 — DSP graph  
**Priority:** Foundational  
**Depends on:** 3.2  
**Status:** Implemented — pending Android listening and callback timing

## Objective

Implement a graphic equalizer (fixed bands) using JUCE DSP building blocks, with per-band gains and host unit tests for filter response sanity.

## Why it matters

EQ is a headline capability for a Poweramp-class product and validates the modular DSP graph under realistic CPU load.

## Architecture

```text
ProcessGraph: Gain → GraphicEQ → (future nodes)
GraphicEQ: N peaking/shelf filters at fixed center frequencies
```

Ten peaking bands are centered at 31.25, 62.5, 125, 250, 500, 1000, 2000, 4000, 8000, and 16000 Hz. The centers are one octave apart. Q is fixed at √2 for roughly one-octave bandwidth at low frequencies. Each band starts at 0 dB and accepts −12 to +12 dB.

## Learning objectives

- Peaking filters / Q / center frequency.
- Measuring CPU cost of cascaded filters.
- Why EQ prepare() depends on sample rate.

## Scope

**Included**
- Graphic EQ node with agreed band count.
- Per-band gain control API.
- Unit tests (impulse or sine probes) for rough expected behavior.
- CPU measurement note on device.

**Excluded**
- Full parametric EQ UI/engine (optional later).
- Preset library UX (UI in 6.5; apply API 3.4).
- Visualizer curve (UI can approximate in 6.5).

**Must not implement yet**
- Rebuilding filters with heap alloc in callback.

## Implementation steps

1. Choose band frequencies + default Q.
2. Implement EQ node with JUCE dsp::IIR or ProcessorDuplicator as appropriate.
3. Host tests for boost/cut at center frequencies.
4. Device validation with music material.
5. Record CPU/callback duration impact.

## Expected project structure

```text
native/audio-juce/include/howl/dsp/graphic_eq.h
native/audio-juce/src/graphic_eq.cpp
native/tests/test_graphic_eq.cpp
```

## Testing strategy

- Unit: boost 1 kHz band → higher energy near 1 kHz vs neighbors.
- Device: extreme boosts should not crash (clipping possible).
- Diagnostics: callback duration vs baseline.

## Acceptance criteria

- [ ] Confirm audible band gain changes during Android playback.
- [x] Filter unit tests pass with JUCE enabled and disabled.
- [x] Sample-rate changes clear filter histories, preserve band gains, and skip bands at or above half the stream sample rate.
- [x] Host DSP cost documented. Channel histories are allocated during preparation. Coefficient updates use fixed-size values in the callback.
- [ ] Measure Android callback duration with the EQ flat and with all bands active.

## Review checklist

- [ ] Know band centers and Q rationale.
- [ ] Understand stereo processing approach.

## Potential pitfalls

- Unstable filters at extreme Q/gain.
- Ignoring Nyquist when sample rate is low.
- Per-band JNI spam; batch parameter sets.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Ten-band file playback EQ after the preamp, a per-band Engine/C/JNI/Kotlin API, and a band selector and gain slider in the FLAC debug screen. |
| Files created/modified | GraphicEq, ProcessGraph, Engine and C API, JNI/Kotlin bridge, debug screen, and native filter tests. |
| Tests executed | Native host suites with JUCE enabled and disabled, Android debug APK build and app unit test, and an optimized native host timing run. |
| Results | 58/58 tests passed in each host configuration. Android debug APK built for arm64-v8a and x86_64. App unit test passed. The 1 kHz band measured about 3.98× at +12 dB and 0.251× at −12 dB. |
| Known limitations | Android listening and callback timing await an online device. EQ gain applies at block boundaries. Smoothing is planned for 3.4. Positive gain can clip. |
| Decisions made | Use JUCE IIR coefficient generation where enabled and the matching native peaking biquad design on Android. Each band shares coefficients across channels and keeps separate channel histories. The debug slider commits on release. |
| Next phase | 3.4 — Parameter smoothing & apply API |

## DSP timing

An optimized Apple Silicon host run with JUCE disabled measured a median of five runs at 48 kHz, 256 stereo frames per block, and 20,000 blocks per run. Each timed block included copying the input and running the whole graph. The average per block was about 0.09 µs with all bands flat and 19 µs with all ten bands alternating between +12 and −12 dB. A 256-frame block spans 5.33 ms at 48 kHz. Android callback timing remains pending.

For device verification, use Repeat All in FLAC queue mode and compare the 500 Hz band around the 440 Hz track and the 1 kHz band around the 880 Hz track. Also compare the callback duration under the same stream configuration with all EQ bands flat and with all ten bands active. When music fixtures are available, repeat the audible check on music.

**Approval required before Phase 3.4.**
