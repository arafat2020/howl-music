# audio-juce

Selective JUCE DSP wrappers for Howl (Phase 3.1+).

## What this module is

- CMake target `howl_audio_juce`
- Public API: `howl::ProcessGraph` (`include/howl/dsp/process_graph.h`)
- Optional backend: `juce_dsp` via FetchContent (JUCE 8.0.6, `JUCE_MODULES_ONLY`)

## Build flags

| Flag | Host default | Android default | Meaning |
|------|--------------|-----------------|--------|
| `HOWL_WITH_JUCE=ON` | yes | no | Fetch/link `juce_core` + `juce_audio_basics` + `juce_dsp` |
| `HOWL_WITH_JUCE=OFF` | — | yes | Native `ProcessGraph` (same preamp, no JUCE download) |

Android defaults to OFF. The file playback preamp uses the same native gain node in both builds.

## Preamp

- The file playback preamp defaults to 0 dB. It clamps finite inputs to −24 through +12 dB and rejects non-finite inputs.
- Linear amplitude is 10 raised to dB / 20. For example, −6 dB is approximately 0.501×.
- A 20 ms linear-amplitude ramp uses the same multiplier for every channel in a frame. Control updates publish the target with a lock-free atomic. The callback picks up the latest target at the start of each processed block.
- The target is retained across track and stream changes within an engine instance.
- Positive gain can clip peaks above full scale.

## License

See `docs/decisions/juce-license.md`. Proprietary Play Store builds require a JUCE commercial license before shipping with `HOWL_WITH_JUCE=ON`.

## Not included

- JUCE GUI / graphics
- `AudioDeviceManager` (Oboe owns I/O)
