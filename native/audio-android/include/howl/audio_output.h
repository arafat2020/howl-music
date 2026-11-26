#pragma once

#include "howl/diagnostics.h"
#include "howl/status.h"

#include <cstdint>
#include <memory>

namespace howl {

/**
 * Oboe output + deterministic sine (Phase 1.2+) with diagnostics (1.3).
 *
 * Realtime rules for the audio callback:
 * - No heap allocation
 * - No logging / file I/O / network / JNI
 * - No unbounded locks
 * - Counters: atomics only (relaxed)
 */
class AudioOutput {
public:
    AudioOutput();
    ~AudioOutput();

    AudioOutput(const AudioOutput&) = delete;
    AudioOutput& operator=(const AudioOutput&) = delete;

    /** Open device stream and begin callback rendering. */
    Status start();

    /** Stop callback and close stream. Safe to call if not running. */
    Status stop();

    bool isRunning() const;

    int32_t sampleRate() const { return sampleRate_; }
    int32_t channelCount() const { return channelCount_; }

    /**
     * Control-thread snapshot. Safe to call while the stream is running;
     * does not block the audio callback.
     */
    void copyDiagnostics(EngineDiagnostics& out) const;

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;

    int32_t sampleRate_{0};
    int32_t channelCount_{0};
    int32_t framesPerBurst_{0};
};

}  // namespace howl
