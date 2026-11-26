#pragma once

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

#ifdef __cplusplus
extern "C" {
#endif

/** C layout matches howl::EngineDiagnostics field order. */
typedef struct HowlEngineDiagnostics {
    int32_t sample_rate;
    int32_t channel_count;
    int32_t frames_per_burst;
    int32_t frames_per_callback;
    int64_t callback_count;
    int64_t underrun_count;
    int64_t error_count;
} HowlEngineDiagnostics;

#ifdef __cplusplus
}
#endif
