#pragma once

#include "howl/engine_api.h"

#include <cstdint>

namespace howl {

/**
 * Plain snapshot of stream / callback health (Phase 1.3).
 *
 * Filled on the control thread by copying atomics + stream config.
 * Never pass this into the audio callback; never allocate for it there.
 */
struct EngineDiagnostics {
    int32_t sample_rate{0};
    int32_t channel_count{0};
    int32_t frames_per_burst{0};
    int32_t frames_per_callback{0};  // last onAudioReady numFrames
    int64_t callback_count{0};
    int64_t underrun_count{0};       // Oboe getXRunCount()
    int64_t error_count{0};          // Oboe error callback firings
};

}  // namespace howl
