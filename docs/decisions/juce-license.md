# JUCE license decision (Phase 3.1)

**Status:** Confirmed for Howl’s intended path  
**Date:** 2026-11-26

## Product intent

Howl is planned as a **commercial Android Play Store app** (proprietary UX) with a native engine.

## JUCE terms (summary)

JUCE 8 is dual-licensed:

| Path | Fit for Howl |
|------|----------------|
| **AGPLv3** | OK only if the *entire* distributed app is AGPL-compatible (including network use clauses). Not compatible with a typical closed-source Play Store build. |
| **Commercial license** (per seat / product from juce.com) | Required for a proprietary store build that links JUCE. |

## Decision

1. **Development / this repo:** Link **DSP modules only** (`juce_dsp` + required deps, **no GUI**, no `AudioDeviceManager`) under evaluation while the engine graph is proven. Source remains attributable; see vendored/FetchContent checkout notices.
2. **Before Play Store / proprietary distribution:** Purchase and record a **JUCE commercial license** covering the shipping product and developers. Do not ship a closed-source APK that includes JUCE without that license.
3. **Notices:** Add JUCE to `THIRD_PARTY_NOTICES` in Phase 8.4 (AGPL text *or* commercial attribution as applicable).

## Modules we link (Phase 3.1)

Via CMake `JUCE_MODULES_ONLY` + `target_link_libraries(... juce::juce_dsp)` (host, `HOWL_WITH_JUCE=ON`):

- `juce_dsp` (Gain / future IIR EQ) — primary
- Transitive: `juce_core`, `juce_audio_basics`, and JUCE’s `juce_audio_formats` dependency of `juce_dsp`

Explicitly **not** linked: `juce_gui_basics`, `juce_graphics`, `juce_audio_devices`, `juce_audio_utils`, Projucer Android GUI wrappers.

Android NDK builds default to `HOWL_WITH_JUCE=OFF` (same `ProcessGraph` API) until packaging is hardened.

## Escape hatch

`HOWL_WITH_JUCE=OFF` builds the native preamp with the same public API and realtime wiring. Future DSP nodes that depend on JUCE require `HOWL_WITH_JUCE=ON` and a commercial license for proprietary builds.
