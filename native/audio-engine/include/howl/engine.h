#pragma once

#include "howl/diagnostics.h"
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
#endif

namespace howl {

enum class PlayMode : int32_t {
    Sine = 0,
    File = 1,
};

/**
 * Playback engine (Phase 2.3: transport + ring path).
 *
 * Control thread: prepare/start/stop/load/pause/resume/seek/transport/diagnostics.
 * Audio callback: pop or silence only — never decode, seek, or flush.
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
    Status load(const char* path);

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

    mutable std::mutex mutex_;
    EngineState state_{EngineState::Idle};
    PlayMode playMode_{PlayMode::Sine};
    std::string loadedPath_;
    std::atomic<bool> paused_{false};
    std::atomic<int64_t> positionFrames_{0};
    int64_t durationFrames_{0};
    int32_t fileSampleRate_{0};

#if defined(__ANDROID__)
    static constexpr int32_t kRingCapacityFrames = 16384;
    static constexpr int32_t kRingChannels = 2;

    std::unique_ptr<PcmRingBuffer> ring_;
    std::unique_ptr<DecodeWorker> worker_;
    std::unique_ptr<AudioOutput> output_;
#endif
};

}  // namespace howl

#ifdef __cplusplus
extern "C" {
#endif

typedef struct HowlEngineOpaque HowlEngineHandle;

HowlEngineHandle* howl_engine_create(void);
void howl_engine_destroy(HowlEngineHandle* engine);

int32_t howl_engine_prepare(HowlEngineHandle* engine);
int32_t howl_engine_start(HowlEngineHandle* engine);
int32_t howl_engine_stop(HowlEngineHandle* engine);
int32_t howl_engine_state(HowlEngineHandle* engine);
int32_t howl_engine_set_play_mode(HowlEngineHandle* engine, int32_t mode);
int32_t howl_engine_load(HowlEngineHandle* engine, const char* path);
int32_t howl_engine_play_mode(HowlEngineHandle* engine);

int32_t howl_engine_pause(HowlEngineHandle* engine);
int32_t howl_engine_resume(HowlEngineHandle* engine);
int32_t howl_engine_seek_frames(HowlEngineHandle* engine, int64_t frame);
int32_t howl_engine_seek_ms(HowlEngineHandle* engine, int64_t ms);
int32_t howl_engine_get_transport(HowlEngineHandle* engine, HowlTransportSnapshot* out);

int32_t howl_engine_get_diagnostics(HowlEngineHandle* engine, HowlEngineDiagnostics* out);

const char* howl_engine_version(void);
const char* howl_engine_status_string(int32_t status);
const char* howl_engine_state_string(int32_t state);

#ifdef __cplusplus
}
#endif
