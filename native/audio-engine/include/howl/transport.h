#pragma once

#include <cstdint>

namespace howl {

/**
 * UI-safe transport snapshot (Phase 2.3).
 * Filled on the control thread from atomics / decoder metadata — never from
 * the audio callback via JNI.
 */
struct TransportSnapshot {
    int64_t positionFrames{0};
    int64_t durationFrames{0};
    int32_t sampleRate{0};
    bool paused{false};
    bool playing{false};  // engine Running (may still be paused)
};

/** Convert frames → milliseconds at sampleRate (0 if rate unknown). */
inline int64_t framesToMs(int64_t frames, int32_t sampleRate) {
    if (sampleRate <= 0) {
        return 0;
    }
    return (frames * 1000) / static_cast<int64_t>(sampleRate);
}

inline int64_t msToFrames(int64_t ms, int32_t sampleRate) {
    if (sampleRate <= 0) {
        return 0;
    }
    return (ms * static_cast<int64_t>(sampleRate)) / 1000;
}

}  // namespace howl

#ifdef __cplusplus
extern "C" {
#endif

typedef struct HowlTransportSnapshot {
    int64_t position_frames;
    int64_t duration_frames;
    int32_t sample_rate;
    int32_t paused;   // 0/1
    int32_t playing;  // 0/1
} HowlTransportSnapshot;

#ifdef __cplusplus
}
#endif
