#pragma once

#include "howl/diagnostics.h"
#include "howl/status.h"

#include <cstdint>
#include <memory>

namespace howl {

class PcmRingBuffer;  // defined in audio-engine; pointer-only here

enum class OutputMode : int32_t {
    Sine = 0,
    Ring = 1,
};

/**
 * Oboe output (Phase 1.2+) with sine or PCM-ring source (Phase 2.2).
 *
 * Realtime rules for the audio callback:
 * - No heap allocation
 * - No logging / file I/O / network / JNI
 * - No unbounded locks
 * - Ring pop only (underrun → silence + atomic count)
 */
class AudioOutput {
public:
    AudioOutput();
    ~AudioOutput();

    AudioOutput(const AudioOutput&) = delete;
    AudioOutput& operator=(const AudioOutput&) = delete;

    /**
     * Configure before start(). Ring pointer is non-owning; must outlive the
     * stream until stop() returns. For Sine mode, ring may be null.
     */
    void setOutputMode(OutputMode mode, PcmRingBuffer* ring);

    /** Open device stream and begin callback rendering. */
    Status start();

    /** Stop callback and close stream. Safe to call if not running. */
    Status stop();

    bool isRunning() const;

    int32_t sampleRate() const { return sampleRate_; }
    int32_t channelCount() const { return channelCount_; }

    void copyDiagnostics(EngineDiagnostics& out) const;

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;

    int32_t sampleRate_{0};
    int32_t channelCount_{0};
    int32_t framesPerBurst_{0};
};

}  // namespace howl
