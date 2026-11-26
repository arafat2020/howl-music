#include "howl/engine.h"

#include <utility>

namespace howl {

std::unique_ptr<Engine> Engine::create() {
    return std::unique_ptr<Engine>(new Engine());
}

Engine::Engine() = default;

Engine::~Engine() {
    (void)stop();
}

Status Engine::prepare() {
    std::lock_guard<std::mutex> lock(mutex_);
    switch (state_) {
        case EngineState::Idle:
            state_ = EngineState::Prepared;
            return Status::Ok;
        case EngineState::Prepared:
            return Status::Ok;
        case EngineState::Running:
        case EngineState::Stopping:
            return Status::InvalidState;
    }
    return Status::InvalidState;
}

Status Engine::start() {
#if defined(__ANDROID__)
    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (state_ == EngineState::Running) {
            return Status::AlreadyRunning;
        }
        if (state_ == EngineState::Stopping) {
            return Status::InvalidState;
        }
        // Mark Prepared while we open the device outside the tight state window.
        if (state_ == EngineState::Idle) {
            state_ = EngineState::Prepared;
        }
    }

    // Open Oboe WITHOUT holding Engine mutex (callback must never need that lock).
    auto output = std::make_unique<AudioOutput>();
    const Status opened = output->start();
    if (opened != Status::Ok) {
        std::lock_guard<std::mutex> lock(mutex_);
        state_ = EngineState::Idle;
        output_.reset();
        return opened;
    }

    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (state_ == EngineState::Prepared || state_ == EngineState::Idle) {
            output_ = std::move(output);
            state_ = EngineState::Running;
            return Status::Ok;
        }
    }
    // Raced into an unexpected state — close the stream we opened.
    (void)output->stop();
    return Status::InvalidState;
#else
    std::lock_guard<std::mutex> lock(mutex_);
    switch (state_) {
        case EngineState::Idle:
            state_ = EngineState::Prepared;
            state_ = EngineState::Running;
            return Status::Ok;
        case EngineState::Prepared:
            state_ = EngineState::Running;
            return Status::Ok;
        case EngineState::Running:
            return Status::AlreadyRunning;
        case EngineState::Stopping:
            return Status::InvalidState;
    }
    return Status::InvalidState;
#endif
}

Status Engine::stop() {
#if defined(__ANDROID__)
    std::unique_ptr<AudioOutput> toStop;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (state_ == EngineState::Idle && !output_) {
            return Status::Ok;
        }
        state_ = EngineState::Stopping;
        toStop = std::move(output_);
    }

    // Close stream outside the mutex.
    if (toStop) {
        (void)toStop->stop();
        toStop.reset();
    }

    std::lock_guard<std::mutex> lock(mutex_);
    state_ = EngineState::Idle;
    return Status::Ok;
#else
    std::lock_guard<std::mutex> lock(mutex_);
    switch (state_) {
        case EngineState::Idle:
            return Status::Ok;
        case EngineState::Prepared:
            state_ = EngineState::Idle;
            return Status::Ok;
        case EngineState::Running:
            state_ = EngineState::Stopping;
            state_ = EngineState::Idle;
            return Status::Ok;
        case EngineState::Stopping:
            state_ = EngineState::Idle;
            return Status::Ok;
    }
    return Status::InvalidState;
#endif
}

EngineState Engine::state() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return state_;
}

const char* Engine::version() {
    return "howl_engine/0.2.0-oboe-sine";
}

}  // namespace howl

extern "C" {

HowlEngineHandle* howl_engine_create(void) {
    auto engine = howl::Engine::create();
    return reinterpret_cast<HowlEngineHandle*>(engine.release());
}

void howl_engine_destroy(HowlEngineHandle* engine) {
    if (engine == nullptr) {
        return;
    }
    std::unique_ptr<howl::Engine> owned(reinterpret_cast<howl::Engine*>(engine));
    owned.reset();
}

int32_t howl_engine_prepare(HowlEngineHandle* engine) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->prepare());
}

int32_t howl_engine_start(HowlEngineHandle* engine) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->start());
}

int32_t howl_engine_stop(HowlEngineHandle* engine) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->stop());
}

int32_t howl_engine_state(HowlEngineHandle* engine) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::EngineState::Idle);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->state());
}

const char* howl_engine_version(void) {
    return howl::Engine::version();
}

const char* howl_engine_status_string(int32_t status) {
    return howl::statusToString(static_cast<howl::Status>(status));
}

const char* howl_engine_state_string(int32_t state) {
    return howl::engineStateToString(static_cast<howl::EngineState>(state));
}

}  // extern "C"
