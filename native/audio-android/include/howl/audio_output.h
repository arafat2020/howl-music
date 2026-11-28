#pragma once

#include "howl/analysis_snapshot.h"
#include "howl/diagnostics.h"
#include "howl/status.h"

#include <atomic>
#include <cstdint>
#include <memory>

namespace howl {

class PcmRingBuffer;
class ProcessGraph;

enum class OutputMode : int32_t {
    Sine = 0,
    Ring = 1,
};

/**
 * Oboe output with sine or PCM-ring source (Phase 2.2/2.3 + 3.1 graph).
 *
 * Pause: callback outputs silence and does not pop the ring (no I/O).
 * Position: optional atomic incremented by frames successfully popped.
 * Ring path: pop → ProcessGraph::process (identity/bypass safe).
 */
class AudioOutput {
public:
    explicit AudioOutput(const std::atomic<bool>& analysisEnabled);
    ~AudioOutput();

    AudioOutput(const AudioOutput&) = delete;
    AudioOutput& operator=(const AudioOutput&) = delete;

    void setOutputMode(OutputMode mode, PcmRingBuffer* ring);

    /** Non-owning; callback increments by frames popped (ring mode). */
    void setPositionFrames(std::atomic<int64_t>* positionFrames);

    /**
     * Non-owning DSP graph. Must outlive the stream; prepare() before start.
     * Null = raw path (no graph).
     */
    void setProcessGraph(ProcessGraph* graph);

    /** When true, callback writes silence and does not consume the ring. */
    void setPaused(bool paused);
    bool isPaused() const;

    Status start(int32_t requestedSampleRate = 0, bool initiallyPaused = false);
    Status stop();

    bool isRunning() const;

    int32_t sampleRate() const { return sampleRate_; }
    int32_t channelCount() const { return channelCount_; }

    AnalysisSnapshot analysis() const noexcept;
    void clearAnalysis() noexcept;

    void copyDiagnostics(EngineDiagnostics& out) const;

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;

    int32_t sampleRate_{0};
    int32_t channelCount_{0};
    int32_t framesPerBurst_{0};
};

}  // namespace howl
