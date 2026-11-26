#pragma once

#include "howl/status.h"

#include <memory>
#include <mutex>

namespace howl {

/**
 * Playback engine skeleton (Phase 1.1).
 *
 * Ownership
 * - Prefer creating via Engine::create() and holding std::unique_ptr<Engine>.
 * - Non-copyable / non-movable to avoid accidental shared ownership of future
 *   realtime resources (Oboe stream, DSP graph).
 *
 * Threading policy (control thread)
 * - prepare/start/stop/state may be called only from a single control thread
 *   (e.g. the thread that owns the JNI/Kotlin HowlEngine, or a dedicated
 *   engine service thread). Do not call these from an audio callback.
 * - A mutex serializes state transitions for safety if that rule is broken,
 *   but it is not a substitute for correct call-site discipline.
 *
 * No audio device is opened in this phase.
 */
class Engine final {
public:
    static std::unique_ptr<Engine> create();

    Engine(const Engine&) = delete;
    Engine& operator=(const Engine&) = delete;
    Engine(Engine&&) = delete;
    Engine& operator=(Engine&&) = delete;

    ~Engine();

    /** Idle → Prepared. Future: configure sample rate / buffer sizes. */
    Status prepare();

    /** Prepared → Running. Rejects double-start. */
    Status start();

    /** Running → Stopping → Idle. Double-stop from Idle is Ok (idempotent). */
    Status stop();

    EngineState state() const;

    static const char* version();

private:
    Engine();

    mutable std::mutex mutex_;
    EngineState state_{EngineState::Idle};

    // Phase 1.2+: AudioOutput* / ProcessGraph* owned here (unique_ptr).
};

}  // namespace howl

/* -------------------------------------------------------------------------- */
/* Stable C API for JNI (opaque handle = howl::Engine*).                      */
/* -------------------------------------------------------------------------- */

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
