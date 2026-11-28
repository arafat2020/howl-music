#pragma once

#include "howl/engine_api.h"
#include "howl/audio_format.h"

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
    int64_t underrun_count{0};       // Ring underruns in file mode
    int64_t error_count{0};          // Oboe error callback firings
    AudioFormat source;
    int32_t hardware_rate{0};
    int32_t device_id{0};
    int64_t platform_xruns{-1};
};

}  // namespace howl
