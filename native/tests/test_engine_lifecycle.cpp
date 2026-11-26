#include "howl/engine.h"

#include <cassert>
#include <cstdio>

int main() {
    using howl::Engine;
    using howl::EngineState;
    using howl::Status;

    auto engine = Engine::create();
    assert(engine != nullptr);
    assert(engine->state() == EngineState::Idle);

    assert(engine->prepare() == Status::Ok);
    assert(engine->state() == EngineState::Prepared);

    assert(engine->start() == Status::Ok);
    assert(engine->state() == EngineState::Running);

    // Double-start must fail cleanly.
    assert(engine->start() == Status::AlreadyRunning);
    assert(engine->state() == EngineState::Running);

    assert(engine->stop() == Status::Ok);
    assert(engine->state() == EngineState::Idle);

    // Double-stop is idempotent.
    assert(engine->stop() == Status::Ok);
    assert(engine->state() == EngineState::Idle);

    // prepare while running is invalid.
    assert(engine->start() == Status::Ok);
    assert(engine->prepare() == Status::InvalidState);
    assert(engine->stop() == Status::Ok);

    // Destroy while previously running is safe (destructor stops).
    assert(engine->start() == Status::Ok);
    engine.reset();

    // C API null-handle behavior.
    assert(howl_engine_start(nullptr) == static_cast<int32_t>(Status::NullHandle));
    assert(howl_engine_stop(nullptr) == static_cast<int32_t>(Status::NullHandle));

    HowlEngineHandle* handle = howl_engine_create();
    assert(handle != nullptr);
    assert(howl_engine_start(handle) == static_cast<int32_t>(Status::Ok));
    assert(howl_engine_start(handle) == static_cast<int32_t>(Status::AlreadyRunning));
    assert(howl_engine_stop(handle) == static_cast<int32_t>(Status::Ok));
    howl_engine_destroy(handle);

    std::printf("howl_engine_lifecycle_test: PASS (%s)\n", Engine::version());
    return 0;
}
