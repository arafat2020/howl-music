#include "howl/engine.h"
#include "howl/wav_decoder.h"

#if defined(__ANDROID__)
#include <vector>
#endif

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

Status Engine::setPlayMode(PlayMode mode) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (state_ == EngineState::Running || state_ == EngineState::Stopping) {
        return Status::InvalidState;
    }
    playMode_ = mode;
    return Status::Ok;
}

Status Engine::load(const char* path) {
    if (path == nullptr || path[0] == '\0') {
        return Status::NullHandle;
    }
    std::lock_guard<std::mutex> lock(mutex_);
    if (state_ == EngineState::Running || state_ == EngineState::Stopping) {
        return Status::InvalidState;
    }

    // Validate the file can open as WAV (control thread only).
    auto probe = createWavDecoder();
    const Status opened = probe->open(path);
    if (opened != Status::Ok) {
        return opened;
    }
    const AudioFormat fmt = probe->format();
    probe->close();

    // Interim policy (Phase 2.2): stereo Float32 path only; rate checked at start
    // against the device stream (reject mismatch — no resampler yet).
    if (fmt.channelCount != 2) {
        return Status::UnsupportedFormat;
    }

    loadedPath_ = path;
    return Status::Ok;
}

PlayMode Engine::playMode() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return playMode_;
}

Status Engine::start() {
#if defined(__ANDROID__)
    PlayMode mode;
    std::string path;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (state_ == EngineState::Running) {
            return Status::AlreadyRunning;
        }
        if (state_ == EngineState::Stopping) {
            return Status::InvalidState;
        }
        if (state_ == EngineState::Idle) {
            state_ = EngineState::Prepared;
        }
        mode = playMode_;
        path = loadedPath_;
        if (mode == PlayMode::File && path.empty()) {
            state_ = EngineState::Idle;
            return Status::InvalidState;
        }
    }

    auto ring = std::make_unique<PcmRingBuffer>(kRingCapacityFrames, kRingChannels);
    auto worker = std::make_unique<DecodeWorker>();
    auto output = std::make_unique<AudioOutput>();

    if (mode == PlayMode::File) {
        auto decoder = createWavDecoder();
        const Status opened = decoder->open(path.c_str());
        if (opened != Status::Ok) {
            std::lock_guard<std::mutex> lock(mutex_);
            state_ = EngineState::Idle;
            return opened;
        }

        // Prefill ring before starting the device to reduce startup underruns.
        {
            std::vector<float> prefill(
                static_cast<size_t>(1024) * static_cast<size_t>(kRingChannels));
            for (int i = 0; i < 8; ++i) {
                int32_t n = 0;
                if (decoder->read(prefill.data(), 1024, &n) != Status::Ok || n <= 0) {
                    break;
                }
                (void)ring->push(prefill.data(), n);
            }
        }

        output->setOutputMode(OutputMode::Ring, ring.get());
        const Status outSt = output->start();
        if (outSt != Status::Ok) {
            std::lock_guard<std::mutex> lock(mutex_);
            state_ = EngineState::Idle;
            return outSt;
        }

        // Rate must match — no resampler in 2.2.
        if (decoder->format().sampleRate != output->sampleRate()) {
            (void)output->stop();
            std::lock_guard<std::mutex> lock(mutex_);
            state_ = EngineState::Idle;
            return Status::UnsupportedFormat;
        }

        const Status wSt = worker->start(std::move(decoder), ring.get());
        if (wSt != Status::Ok) {
            (void)output->stop();
            std::lock_guard<std::mutex> lock(mutex_);
            state_ = EngineState::Idle;
            return wSt;
        }
    } else {
        output->setOutputMode(OutputMode::Sine, nullptr);
        const Status outSt = output->start();
        if (outSt != Status::Ok) {
            std::lock_guard<std::mutex> lock(mutex_);
            state_ = EngineState::Idle;
            return outSt;
        }
    }

    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (state_ == EngineState::Prepared || state_ == EngineState::Idle) {
            ring_ = std::move(ring);
            worker_ = std::move(worker);
            output_ = std::move(output);
            state_ = EngineState::Running;
            return Status::Ok;
        }
    }

    worker->stop();
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
    std::unique_ptr<DecodeWorker> toJoin;
    std::unique_ptr<AudioOutput> toStop;
    std::unique_ptr<PcmRingBuffer> toDrop;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (state_ == EngineState::Idle && !output_ && !worker_) {
            return Status::Ok;
        }
        state_ = EngineState::Stopping;
        toJoin = std::move(worker_);
        toStop = std::move(output_);
        toDrop = std::move(ring_);
    }

    // Join worker first so it cannot push after ring is destroyed.
    if (toJoin) {
        toJoin->stop();
        toJoin.reset();
    }
    if (toStop) {
        (void)toStop->stop();
        toStop.reset();
    }
    toDrop.reset();

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

EngineDiagnostics Engine::diagnostics() const {
    EngineDiagnostics out{};
#if defined(__ANDROID__)
    std::lock_guard<std::mutex> lock(mutex_);
    if (output_) {
        output_->copyDiagnostics(out);
    }
#endif
    return out;
}

const char* Engine::version() {
    return "howl_engine/0.4.0-pcm-ring";
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

int32_t howl_engine_set_play_mode(HowlEngineHandle* engine, int32_t mode) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->setPlayMode(
        static_cast<howl::PlayMode>(mode)));
}

int32_t howl_engine_load(HowlEngineHandle* engine, const char* path) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->load(path));
}

int32_t howl_engine_play_mode(HowlEngineHandle* engine) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::PlayMode::Sine);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->playMode());
}

int32_t howl_engine_get_diagnostics(HowlEngineHandle* engine, HowlEngineDiagnostics* out) {
    if (engine == nullptr || out == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    const howl::EngineDiagnostics snap =
        reinterpret_cast<howl::Engine*>(engine)->diagnostics();
    out->sample_rate = snap.sample_rate;
    out->channel_count = snap.channel_count;
    out->frames_per_burst = snap.frames_per_burst;
    out->frames_per_callback = snap.frames_per_callback;
    out->callback_count = snap.callback_count;
    out->underrun_count = snap.underrun_count;
    out->error_count = snap.error_count;
    return static_cast<int32_t>(howl::Status::Ok);
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
