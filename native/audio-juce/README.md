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
| `HOWL_WITH_JUCE=OFF` | — | yes | Identity `ProcessGraph` (same API, no JUCE download) |

Android stays OFF until NDK packaging is hardened; the realtime ring → graph path still runs.

## License

See `docs/decisions/juce-license.md`. Proprietary Play Store builds require a JUCE commercial license before shipping with `HOWL_WITH_JUCE=ON`.

## Not included

- JUCE GUI / graphics
- `AudioDeviceManager` (Oboe owns I/O)
