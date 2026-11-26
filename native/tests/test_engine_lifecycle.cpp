#include "howl/engine.h"

#include <gtest/gtest.h>

#include <cstdint>
#include <memory>

using howl::Engine;
using howl::EngineDiagnostics;
using howl::EngineState;
using howl::Status;

TEST(EngineLifecycle, HappyPathPrepareStartStop) {
    auto engine = Engine::create();
    ASSERT_NE(engine, nullptr);
    EXPECT_EQ(engine->state(), EngineState::Idle);

    EXPECT_EQ(engine->prepare(), Status::Ok);
    EXPECT_EQ(engine->state(), EngineState::Prepared);

    EXPECT_EQ(engine->start(), Status::Ok);
    EXPECT_EQ(engine->state(), EngineState::Running);

    EXPECT_EQ(engine->stop(), Status::Ok);
    EXPECT_EQ(engine->state(), EngineState::Idle);
}

TEST(EngineLifecycle, StartWithoutExplicitPrepare) {
    auto engine = Engine::create();
    EXPECT_EQ(engine->start(), Status::Ok);
    EXPECT_EQ(engine->state(), EngineState::Running);
    EXPECT_EQ(engine->stop(), Status::Ok);
}

TEST(EngineLifecycle, DoubleStartRejected) {
    auto engine = Engine::create();
    ASSERT_EQ(engine->start(), Status::Ok);
    EXPECT_EQ(engine->start(), Status::AlreadyRunning);
    EXPECT_EQ(engine->state(), EngineState::Running);
    EXPECT_EQ(engine->stop(), Status::Ok);
}

TEST(EngineLifecycle, DoubleStopIdempotent) {
    auto engine = Engine::create();
    ASSERT_EQ(engine->start(), Status::Ok);
    EXPECT_EQ(engine->stop(), Status::Ok);
    EXPECT_EQ(engine->stop(), Status::Ok);
    EXPECT_EQ(engine->state(), EngineState::Idle);
}

TEST(EngineLifecycle, PrepareWhileRunningInvalid) {
    auto engine = Engine::create();
    ASSERT_EQ(engine->start(), Status::Ok);
    EXPECT_EQ(engine->prepare(), Status::InvalidState);
    EXPECT_EQ(engine->stop(), Status::Ok);
}

TEST(EngineLifecycle, DestructorStopsRunningEngine) {
    auto engine = Engine::create();
    ASSERT_EQ(engine->start(), Status::Ok);
    engine.reset();
    // No crash / hang — destructor must stop cleanly.
}

TEST(EngineLifecycle, HostDiagnosticsStayZero) {
    // Host builds have no Oboe device — do not pretend I/O works here.
    auto engine = Engine::create();
    ASSERT_EQ(engine->start(), Status::Ok);

    const EngineDiagnostics d = engine->diagnostics();
    EXPECT_EQ(d.sample_rate, 0);
    EXPECT_EQ(d.channel_count, 0);
    EXPECT_EQ(d.callback_count, 0);
    EXPECT_EQ(d.underrun_count, 0);

    EXPECT_EQ(engine->stop(), Status::Ok);
}

TEST(EngineLifecycle, CApiNullHandle) {
    EXPECT_EQ(howl_engine_start(nullptr), static_cast<int32_t>(Status::NullHandle));
    EXPECT_EQ(howl_engine_stop(nullptr), static_cast<int32_t>(Status::NullHandle));
    EXPECT_EQ(howl_engine_get_diagnostics(nullptr, nullptr),
              static_cast<int32_t>(Status::NullHandle));
}

TEST(EngineLifecycle, CApiLifecycle) {
    HowlEngineHandle* handle = howl_engine_create();
    ASSERT_NE(handle, nullptr);
    EXPECT_EQ(howl_engine_start(handle), static_cast<int32_t>(Status::Ok));
    EXPECT_EQ(howl_engine_start(handle), static_cast<int32_t>(Status::AlreadyRunning));
    EXPECT_EQ(howl_engine_stop(handle), static_cast<int32_t>(Status::Ok));
    howl_engine_destroy(handle);
}
