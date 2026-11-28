#pragma once

#include "howl/engine_api.h"

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
    int64_t handoffToken{0};
    int32_t gaplessMode{0};
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
