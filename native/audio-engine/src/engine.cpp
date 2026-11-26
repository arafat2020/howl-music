#include "howl/engine.h"
#include "howl/wav_decoder.h"

#if defined(__ANDROID__)
#include <thread>
#include <vector>
#endif

#include <algorithm>
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

    auto probe = createWavDecoder();
    const Status opened = probe->open(path);
    if (opened != Status::Ok) {
        return opened;
    }
    const AudioFormat fmt = probe->format();
    durationFrames_ = probe->frameCount();
    fileSampleRate_ = fmt.sampleRate;
    probe->close();

    if (fmt.channelCount != 2) {
        durationFrames_ = 0;
        fileSampleRate_ = 0;
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
    positionFrames_.store(0, std::memory_order_relaxed);
    paused_.store(false, std::memory_order_relaxed);

    if (mode == PlayMode::File) {
        auto decoder = createWavDecoder();
        const Status opened = decoder->open(path.c_str());
        if (opened != Status::Ok) {
            std::lock_guard<std::mutex> lock(mutex_);
            state_ = EngineState::Idle;
            return opened;
        }
        durationFrames_ = decoder->frameCount();
        fileSampleRate_ = decoder->format().sampleRate;

        output->setOutputMode(OutputMode::Ring, ring.get());
        output->setPositionFrames(&positionFrames_);
        const Status outSt = output->start();
        if (outSt != Status::Ok) {
            std::lock_guard<std::mutex> lock(mutex_);
            state_ = EngineState::Idle;
            return outSt;
        }

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
        (void)worker->prefill(8);
        worker->setPushEnabled(true);
    } else {
        output->setOutputMode(OutputMode::Sine, nullptr);
        output->setPositionFrames(nullptr);
        const Status outSt = output->start();
        if (outSt != Status::Ok) {
            std::lock_guard<std::mutex> lock(mutex_);
            state_ = EngineState::Idle;
            return outSt;
        }
        durationFrames_ = 0;
        fileSampleRate_ = output->sampleRate();
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
            paused_.store(false, std::memory_order_relaxed);
            return Status::Ok;
        case EngineState::Prepared:
            state_ = EngineState::Running;
            paused_.store(false, std::memory_order_relaxed);
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
        paused_.store(false, std::memory_order_relaxed);
    }

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
    positionFrames_.store(0, std::memory_order_relaxed);
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
            paused_.store(false, std::memory_order_relaxed);
            return Status::Ok;
        case EngineState::Stopping:
            state_ = EngineState::Idle;
            return Status::Ok;
    }
    return Status::InvalidState;
#endif
}

Status Engine::pause() {
#if defined(__ANDROID__)
    std::lock_guard<std::mutex> lock(mutex_);
    if (state_ != EngineState::Running || !output_) {
        return Status::NotRunning;
    }
    paused_.store(true, std::memory_order_release);
    output_->setPaused(true);
    if (worker_) {
        worker_->setPushEnabled(false);
    }
    return Status::Ok;
#else
    std::lock_guard<std::mutex> lock(mutex_);
    if (state_ != EngineState::Running) {
        return Status::NotRunning;
    }
    paused_.store(true, std::memory_order_release);
    return Status::Ok;
#endif
}

Status Engine::resume() {
#if defined(__ANDROID__)
    std::lock_guard<std::mutex> lock(mutex_);
    if (state_ != EngineState::Running || !output_) {
        return Status::NotRunning;
    }
    if (worker_) {
        worker_->setPushEnabled(true);
    }
    output_->setPaused(false);
    paused_.store(false, std::memory_order_release);
    return Status::Ok;
#else
    std::lock_guard<std::mutex> lock(mutex_);
    if (state_ != EngineState::Running) {
        return Status::NotRunning;
    }
    paused_.store(false, std::memory_order_release);
    return Status::Ok;
#endif
}

Status Engine::seekFrames(int64_t frameIndex) {
#if defined(__ANDROID__)
    // Pause pop + push, flush ring, seek decoder, prefill, restore pause state.
    bool wasPaused = false;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (state_ != EngineState::Running || playMode_ != PlayMode::File || !worker_ ||
            !ring_ || !output_) {
            return Status::InvalidState;
        }
        wasPaused = paused_.load(std::memory_order_acquire);
        const int64_t dur = durationFrames_;
        if (frameIndex < 0 || (dur > 0 && frameIndex > dur)) {
            return Status::InvalidState;
        }

        output_->setPaused(true);
        worker_->setPushEnabled(false);
    }

    // Let in-flight callback observe pause before we clear the ring.
    std::this_thread::sleep_for(std::chrono::milliseconds(5));

    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (state_ != EngineState::Running || !worker_ || !ring_ || !output_) {
            return Status::InvalidState;
        }
        ring_->clear();
        const Status st = worker_->seekToFrame(frameIndex);
        if (st != Status::Ok) {
            if (!wasPaused) {
                worker_->setPushEnabled(true);
                output_->setPaused(false);
                paused_.store(false, std::memory_order_release);
            }
            return st;
        }
        positionFrames_.store(frameIndex, std::memory_order_relaxed);
        (void)worker_->prefill(8);
        if (!wasPaused) {
            worker_->setPushEnabled(true);
            output_->setPaused(false);
            paused_.store(false, std::memory_order_release);
        } else {
            paused_.store(true, std::memory_order_release);
        }
    }
    return Status::Ok;
#else
    (void)frameIndex;
    return Status::InvalidState;
#endif
}

Status Engine::seekMs(int64_t positionMs) {
    int32_t rate = 0;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        rate = fileSampleRate_;
#if defined(__ANDROID__)
        if (rate <= 0 && output_) {
            rate = output_->sampleRate();
        }
#endif
    }
    return seekFrames(msToFrames(positionMs, rate));
}

bool Engine::isPaused() const {
    return paused_.load(std::memory_order_acquire);
}

TransportSnapshot Engine::transport() const {
    TransportSnapshot snap{};
    std::lock_guard<std::mutex> lock(mutex_);
    snap.playing = (state_ == EngineState::Running);
    snap.paused = paused_.load(std::memory_order_acquire);
    snap.positionFrames = positionFrames_.load(std::memory_order_relaxed);
    snap.durationFrames = durationFrames_;
    snap.sampleRate = fileSampleRate_;
#if defined(__ANDROID__)
    if (snap.sampleRate <= 0 && output_) {
        snap.sampleRate = output_->sampleRate();
    }
#endif
    if (snap.durationFrames > 0) {
        snap.positionFrames = std::min(snap.positionFrames, snap.durationFrames);
    }
    return snap;
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
    return "howl_engine/0.5.0-transport";
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

int32_t howl_engine_pause(HowlEngineHandle* engine) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->pause());
}

int32_t howl_engine_resume(HowlEngineHandle* engine) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->resume());
}

int32_t howl_engine_seek_frames(HowlEngineHandle* engine, int64_t frame) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->seekFrames(frame));
}

int32_t howl_engine_seek_ms(HowlEngineHandle* engine, int64_t ms) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->seekMs(ms));
}

int32_t howl_engine_get_transport(HowlEngineHandle* engine, HowlTransportSnapshot* out) {
    if (engine == nullptr || out == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    const howl::TransportSnapshot snap =
        reinterpret_cast<howl::Engine*>(engine)->transport();
    out->position_frames = snap.positionFrames;
    out->duration_frames = snap.durationFrames;
    out->sample_rate = snap.sampleRate;
    out->paused = snap.paused ? 1 : 0;
    out->playing = snap.playing ? 1 : 0;
    return static_cast<int32_t>(howl::Status::Ok);
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
