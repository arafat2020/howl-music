# Phase 8.2 — Output / Sample-Rate Diagnostics

**Milestone:** 8 — Device validation  
**Priority:** Release-hardening  
**Depends on:** 1.3, 4.1  
**Status:** Implemented

## Objective

Expose user-visible (and developer-visible) diagnostics for audio format, sample rate, output route label, underruns, and processing state — matching the product success criterion for clear diagnostics.

## Why it matters

Power users trust players that show what the output path is doing. It also prevents false marketing claims.

## Architecture

```text
Decoder and Oboe stream → engine diagnostics → JNI → same-app MediaSession command
Settings → Output diagnostics → foreground polling of the playback service
```

## Reading the screen

| Field | Meaning |
|---|---|
| Source format and channels | Decoder-reported container, sample representation, bit depth, and channel count for the current file. At a gapless boundary, metadata follows the PCM playback position. |
| Source sample rate | The sample rate declared by the decoded file. |
| App stream rate and channels | The Oboe callback’s configured rate and channels. Howl’s processing graph uses this rate. |
| Hardware-reported rate | The rate returned by Oboe’s hardware API when available. Android can still mix or resample downstream. The screen shows Unavailable when the platform does not provide a rate. |
| Output route | The Android output device label matched to the ID reported by the live stream. The label is best effort. |
| Equalizer and gapless handoff | The session’s EQ enable/bypass setting and the native handoff state. |
| Ring underruns and platform xruns | Howl ring-buffer shortages and the separate platform counter when available. A ring shortage can occur at the final partial callback. |
| Stream errors | Errors reported for the current output stream. |

Open **Settings → Output diagnostics**. It refreshes once per second while visible. The diagnostics request reads the playback session that owns the actual stream. It remains separate from the visualizer’s metering request. Hardware data depends on Android and Oboe support. [Oboe release notes](https://github.com/google/oboe/releases/tag/1.8.0) · [Android output device IDs](https://developer.android.com/reference/android/media/AudioDeviceInfo)

## Learning objectives

- Output route awareness limits on Android.
- Distinguishing source rate vs stream rate vs device rate.
- Communicating uncertainty honestly in UI copy.

## Scope

**Included**
- Diagnostics screen in app.
- Fields from engine + best-effort route info from Android APIs.
- Copy that avoids unverified bit-perfect claims.

**Excluded**
- USB DAC deep controls (8.3).
- Automated claims of 24-bit/192 kHz support.

**Must not implement yet**
- Fake “bit perfect” badges.

## Implementation steps

1. Extend diagnostics snapshot if needed.
2. Build Diagnostics UI using design system.
3. Verify values against known test signal configuration.
4. Write user-facing explanations for each field.

## Expected project structure

```text
android-app/.../ui/settings/DiagnosticsScreen.kt
```

## Testing strategy

- Play 44.1 vs 48 material; observe fields.
- Toggle EQ; processing state updates.
- Compare with `adb dumpsys audio` notes where useful.

## Acceptance criteria

- [x] Diagnostics screen shows format, rates, processing, underruns.
- [x] Values match engine configuration in controlled tests.
- [x] UI language is precise (no overclaim).
- [x] Accessible from settings.

## Review checklist

- [x] Source vs output rate distinction clear.
- [x] Route field labeled as best-effort if uncertain.

## Potential pitfalls

- Trusting UI labels without checking Oboe actual stream.
- Showing marketing sample rates that never reach the DAC.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Settings diagnostics screen with source, processing, output, route, EQ, gapless, and stream counters. |
| Files created/modified | Diagnostics screen and navigation, decoder/stream snapshots, JNI and session payload, and Android instrumentation. |
| Tests executed | Android build and JVM tests, Android instrumentation, 70 native host tests, and an emulator Settings smoke test. |
| Results | A 44.1 kHz WAV reported a 44,100 Hz source and app stream, with 48,000 Hz hardware and a built-in speaker route. The emulator AudioFlinger dump also showed a 48,000 Hz output. The 48 kHz WAV and the FLAC UI smoke fixture reported 48,000 Hz at all three points. The Settings smoke test confirmed the bypass state and displayed separate ring and platform counters. Instrumentation verified EQ enabled and bypassed, and cleared source and route fields after playback selection was removed. |
| Known limitations | Route and hardware rate depend on platform reporting. Physical route changes and USB DAC behavior remain unverified. |
| Decisions made | Reuse the session diagnostics request and keep source, app stream, hardware and counters distinct. |
| Next phase | 8.3 USB (optional) or 8.4 release checklist |

**Approval required before next phase.**
