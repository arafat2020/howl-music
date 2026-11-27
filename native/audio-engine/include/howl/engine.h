#pragma once

#include "howl/diagnostics.h"
#include "howl/dsp/process_graph.h"
#include "howl/engine_api.h"
#include "howl/queue.h"
#include "howl/status.h"
#include "howl/transport.h"

#include <atomic>
#include <memory>
#include <mutex>
#include <string>

#if defined(__ANDROID__)
#include "howl/audio_output.h"
#include "howl/decode_worker.h"
#include "howl/pcm_ring_buffer.h"
#include <thread>
#endif

namespace howl {

enum class PlayMode : int32_t {
    Sine = 0,
    File = 1,
};

/**
 * Playback engine (Phase 2.5: queue handoff + transport).
 *
 * Control thread: prepare/start/stop/load/queue/pause/resume/seek/transport.
 * Audio callback: pop or silence only — never decode, seek, flush, or queue ops.
 */
class Engine final {
public:
    static std::unique_ptr<Engine> create();

    Engine(const Engine&) = delete;
    Engine& operator=(const Engine&) = delete;
    Engine(Engine&&) = delete;
    Engine& operator=(Engine&&) = delete;

    ~Engine();

    Status prepare();
    Status start();
    Status stop();

    Status setPlayMode(PlayMode mode);

    /**
     * Probe path, clear queue, enqueue as sole item (backward-compatible).
     * Invalid while Running/Stopping.
     */
    Status load(const char* path);

    /** Append a validated stereo WAV path. OK while Running. */
    Status enqueue(const char* path);
    Status clearQueue();
    Status playAt(int index);
    Status next();
    Status previous();
    Status setPreampGainDb(float gainDb) noexcept;
    Status setEqBandGainDb(int32_t band, float gainDb) noexcept;
    Status applyEqState(const EqState& state) noexcept;
    void resetEqToFlat() noexcept;
    Status setRepeatMode(RepeatMode mode);

    int queueSize() const;
    int currentIndex() const;
    RepeatMode repeatMode() const;

    /** Pause output (silence, no ring pop). Engine stays Running. */
    Status pause();
    /** Resume after pause. */
    Status resume();

    /** Absolute seek in PCM frames (File mode). Flushes ring. */
    Status seekFrames(int64_t frameIndex);
    /** Absolute seek in milliseconds. */
    Status seekMs(int64_t positionMs);

    PlayMode playMode() const;
    EngineState state() const;
    bool isPaused() const;
    TransportSnapshot transport() const;
    EngineDiagnostics diagnostics() const;

    static const char* version();

private:
    Engine();

    Status probeAndStore(const char* path);
    Status syncLoadedFromQueue();

#if defined(__ANDROID__)
    Status changeTrack(const std::string& path);
    Status handleEofAdvance();
    Status stopPlayback();
    void startQueueWatch();
    void stopQueueWatch();
    void queueWatchMain();
#endif

    mutable std::mutex mutex_;
    EngineState state_{EngineState::Idle};
    PlayMode playMode_{PlayMode::Sine};
    std::string loadedPath_;
    QueueController queue_;
    std::atomic<bool> paused_{false};
    std::atomic<int64_t> positionFrames_{0};
    int64_t durationFrames_{0};
    int32_t fileSampleRate_{0};
    ProcessGraph processGraph_;

#if defined(__ANDROID__)
    static constexpr int32_t kRingCapacityFrames = 16384;
    static constexpr int32_t kRingChannels = 2;

    std::unique_ptr<PcmRingBuffer> ring_;
    std::unique_ptr<DecodeWorker> worker_;
    std::unique_ptr<AudioOutput> output_;

    std::thread queueWatchThread_;
    std::atomic<bool> queueWatchRunning_{false};
    std::atomic<bool> trackChanging_{false};

    void prepareProcessGraphLocked();
#endif
};

}  // namespace howl
