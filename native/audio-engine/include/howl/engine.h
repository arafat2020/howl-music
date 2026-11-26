#pragma once

#include "howl/status.h"

#include <memory>
#include <mutex>

#if defined(__ANDROID__)
#include "howl/audio_output.h"
#endif

namespace howl {

/**
 * Playback engine (Phase 1.2: Oboe sine output on Android).
 *
 * Ownership
 * - Prefer Engine::create() → std::unique_ptr<Engine>.
 * - Non-copyable / non-movable.
 * - On Android, owns AudioOutput (Oboe stream) while Running.
 *
 * Threading policy (control thread)
 * - prepare/start/stop/state: control thread only — never from audio callback.
 * - Audio callback (inside AudioOutput) must not take Engine's mutex.
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

    EngineState state() const;

    static const char* version();

private:
    Engine();

    mutable std::mutex mutex_;
    EngineState state_{EngineState::Idle};

#if defined(__ANDROID__)
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

const char* howl_engine_version(void);
const char* howl_engine_status_string(int32_t status);
const char* howl_engine_state_string(int32_t state);

#ifdef __cplusplus
}
#endif
