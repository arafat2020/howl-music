# USB DAC investigation

## Evidence and status

The available Android target was the ARM64 Pixel emulator on Android 16 / API 36. ADB reported no physical Android phone. No Android phone–DAC pair was available for testing.

| Phone / target | Route | Source | Howl app stream | Hardware-reported rate | Result |
|---|---|---:|---:|---:|---|
| Pixel ARM64 emulator, API 36 | Built-in speaker | 44.1 kHz stereo WAV | 44.1 kHz | 48 kHz | Verified in phase 8.2 and release diagnostics regression. |
| Pixel ARM64 emulator, API 36 | Built-in speaker | 48 kHz stereo WAV | 48 kHz | 48 kHz | Verified in phase 8.2 and release diagnostics regression. |
| Physical Android phone | USB DAC | 44.1 and 48 kHz stereo, then capabilities supported by the test pair | Pending | Pending | Connection, rate negotiation, and hot-plug tests blocked on hardware. |

The emulator rows demonstrate how the diagnostics distinguish three rate measurements. The hardware-reported rate comes from Oboe. The best-effort device label matches the stream device ID against Android's output list. A missing ID or rate appears as **Unavailable**.

## Platform behavior and app scope

Android's USB host audio routing is governed by the phone's audio policy and its compatibility with the peripheral. The DAC's advertised format list describes capabilities. Check the active route, negotiated values, and, when available, the DAC's own readout independently. [Android USB audio architecture](https://source.android.com/docs/core/audio/usb)

Android 14 adds a preferred mixer API for USB endpoints, with behavior dependent on device support. Howl currently uses the Android-selected output route and the existing Oboe stream. The Settings diagnostics screen is the supported place to read the values Android and Oboe provide. [Android preferred USB mixer attributes](https://source.android.com/docs/core/audio/preferred-mixer-attr) · [Oboe stream documentation](https://github.com/google/oboe/blob/1.9.3/docs/FullGuide.md)

## Physical test procedure

1. Record the phone model, Android build, DAC model and firmware, adapter, and power arrangement. Use an emulator-independent 44.1/48 kHz stereo WAV pair and the same test files for the speaker comparison.
2. With the DAC disconnected, play each track and record the three rates, channel count, EQ state, route label, stream errors, and both underrun counters. Capture the relevant AudioFlinger/audio-policy lines.
3. Attach the DAC and record any route change. Repeat both rates with EQ bypassed and with a modest EQ adjustment. If the test pair advertises further useful rates or formats, add matrix rows for them.
4. Disconnect during playback, observe pause or recovery, and reconnect. Repeat with background playback and a paused track. Record recovery time, app/service state, stale labels, and playback errors.
5. Repeat with a second phone or adapter if routing fails. Separate phone or adapter power/routing failures from format rejection by the DAC. Record each recurrence before considering a guarded device-selection feature.

Current evidence supports reporting the stream fields only. Universal DAC compatibility, exclusive mode, bit-perfect output, native DSD, fixed high-resolution modes, and hot-plug recovery are unverified. Device-selection extras remain optional follow-up work.
