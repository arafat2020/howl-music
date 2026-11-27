#include "howl/engine.h"
#include "howl/wav_decoder.h"

#if defined(__ANDROID__)
#include <chrono>
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

Status Engine::probeAndStore(const char* path) {
    Status opened = Status::UnsupportedFormat;
    auto probe = openDecoder(path, &opened);
    if (!probe) {
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

Status Engine::syncLoadedFromQueue() {
    const QueueItem* item = queue_.current();
    if (item == nullptr) {
        loadedPath_.clear();
        durationFrames_ = 0;
        fileSampleRate_ = 0;
        return Status::InvalidState;
    }
    return probeAndStore(item->path.c_str());
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

    const Status probed = probeAndStore(path);
    if (probed != Status::Ok) {
        return probed;
    }

    queue_.clear();
    (void)queue_.enqueue(path);
    return Status::Ok;
}

Status Engine::enqueue(const char* path) {
    if (path == nullptr || path[0] == '\0') {
        return Status::NullHandle;
    }

    Status opened = Status::UnsupportedFormat;
    auto probe = openDecoder(path, &opened);
    if (!probe) {
        return opened;
    }
    const AudioFormat fmt = probe->format();
    probe->close();
    if (fmt.channelCount != 2) {
        return Status::UnsupportedFormat;
    }

    std::lock_guard<std::mutex> lock(mutex_);
    const Status st = queue_.enqueue(path);
    if (st != Status::Ok) {
        return st;
    }
    if (loadedPath_.empty() && state_ != EngineState::Running &&
        state_ != EngineState::Stopping) {
        (void)syncLoadedFromQueue();
    }
    return Status::Ok;
}

Status Engine::clearQueue() {
    if (state() == EngineState::Running) {
        const Status stopSt = stop();
        if (stopSt != Status::Ok) {
            return stopSt;
        }
    }
    std::lock_guard<std::mutex> lock(mutex_);
    queue_.clear();
    loadedPath_.clear();
    durationFrames_ = 0;
    fileSampleRate_ = 0;
    return Status::Ok;
}

Status Engine::playAt(int index) {
    std::string path;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        const Status st = queue_.setCurrentIndex(index);
        if (st != Status::Ok) {
            return st;
        }
        const Status sync = syncLoadedFromQueue();
        if (sync != Status::Ok) {
            return sync;
        }
        path = loadedPath_;
#if defined(__ANDROID__)
        if (state_ != EngineState::Running || playMode_ != PlayMode::File) {
            return Status::Ok;
        }
#else
        (void)path;
        return Status::Ok;
#endif
    }
#if defined(__ANDROID__)
    return changeTrack(path);
#else
    return Status::Ok;
#endif
}

Status Engine::next() {
    std::string path;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        // Explicit next always moves forward (even under Repeat::One).
        const bool haveItem = queue_.advanceNext(/*honorRepeatOne=*/false);
        if (!haveItem) {
            return Status::InvalidState;
        }
        const Status sync = syncLoadedFromQueue();
        if (sync != Status::Ok) {
            return sync;
        }
        path = loadedPath_;
#if defined(__ANDROID__)
        if (state_ != EngineState::Running || playMode_ != PlayMode::File) {
            return Status::Ok;
        }
#else
        (void)path;
        return Status::Ok;
#endif
    }
#if defined(__ANDROID__)
    return changeTrack(path);
#else
    return Status::Ok;
#endif
}

Status Engine::previous() {
    std::string path;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (!queue_.advancePrevious()) {
            return Status::InvalidState;
        }
        const Status sync = syncLoadedFromQueue();
        if (sync != Status::Ok) {
            return sync;
        }
        path = loadedPath_;
#if defined(__ANDROID__)
        if (state_ != EngineState::Running || playMode_ != PlayMode::File) {
            return Status::Ok;
        }
#else
        (void)path;
        return Status::Ok;
#endif
    }
#if defined(__ANDROID__)
    return changeTrack(path);
#else
    return Status::Ok;
#endif
}

Status Engine::setPreampGainDb(float gainDb) noexcept {
    return processGraph_.setTargetGainDb(gainDb) ? Status::Ok : Status::InvalidArgument;
}

Status Engine::setEqBandGainDb(int32_t band, float gainDb) noexcept {
    return processGraph_.setEqBandGainDb(band, gainDb) ? Status::Ok : Status::InvalidArgument;
}

Status Engine::applyEqState(const EqState& state) noexcept {
    return processGraph_.applyEqState(state) ? Status::Ok : Status::InvalidArgument;
}

void Engine::resetEqToFlat() noexcept {
    processGraph_.resetEqToFlat();
}

Status Engine::setRepeatMode(RepeatMode mode) {
    std::lock_guard<std::mutex> lock(mutex_);
    queue_.setRepeatMode(mode);
    return Status::Ok;
}

int Engine::queueSize() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return static_cast<int>(queue_.size());
}

int Engine::currentIndex() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return queue_.currentIndex();
}

RepeatMode Engine::repeatMode() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return queue_.repeatMode();
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
        if (mode == PlayMode::File) {
            if (!queue_.empty()) {
                if (queue_.current() == nullptr) {
                    (void)queue_.setCurrentIndex(0);
                }
                const Status sync = syncLoadedFromQueue();
                if (sync != Status::Ok) {
                    state_ = EngineState::Idle;
                    return sync;
                }
            }
            path = loadedPath_;
            if (path.empty()) {
                state_ = EngineState::Idle;
                return Status::InvalidState;
            }
        }
    }

    auto ring = std::make_unique<PcmRingBuffer>(kRingCapacityFrames, kRingChannels);
    auto worker = std::make_unique<DecodeWorker>();
    auto output = std::make_unique<AudioOutput>();
    positionFrames_.store(0, std::memory_order_relaxed);
    paused_.store(false, std::memory_order_relaxed);

    if (mode == PlayMode::File) {
        Status opened = Status::UnsupportedFormat;
        auto decoder = openDecoder(path.c_str(), &opened);
        if (!decoder) {
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
            if (mode == PlayMode::File) {
                prepareProcessGraphLocked();
                (void)worker_->prefill(8);
                worker_->setPushEnabled(true);
            }
            state_ = EngineState::Running;
            if (mode == PlayMode::File) {
                startQueueWatch();
            }
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
    stopQueueWatch();

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
    trackChanging_.store(false, std::memory_order_release);
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
    return "howl_engine/0.8.0-dsp-graph";
}

#if defined(__ANDROID__)

void Engine::prepareProcessGraphLocked() {
    if (!output_) {
        return;
    }
    const int32_t rate = output_->sampleRate();
    const int32_t channels = output_->channelCount();
    // Conservative max block — covers typical Oboe bursts without alloc in process().
    constexpr int32_t kMaxBlock = 2048;
    processGraph_.prepare(static_cast<double>(rate), kMaxBlock, channels);
    processGraph_.setBypassed(false);
    output_->setProcessGraph(&processGraph_);
}

#endif

#if defined(__ANDROID__)

Status Engine::changeTrack(const std::string& path) {
    bool expected = false;
    if (!trackChanging_.compare_exchange_strong(expected, true, std::memory_order_acq_rel)) {
        return Status::InvalidState;
    }

    bool wasPaused = false;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (state_ != EngineState::Running || playMode_ != PlayMode::File || !output_ ||
            !ring_) {
            trackChanging_.store(false, std::memory_order_release);
            return Status::InvalidState;
        }
        wasPaused = paused_.load(std::memory_order_acquire);
        output_->setPaused(true);
        if (worker_) {
            worker_->setPushEnabled(false);
        }
    }

    std::this_thread::sleep_for(std::chrono::milliseconds(5));

    std::unique_ptr<DecodeWorker> oldWorker;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (state_ != EngineState::Running || !ring_ || !output_) {
            trackChanging_.store(false, std::memory_order_release);
            return Status::InvalidState;
        }
        oldWorker = std::move(worker_);
        ring_->clear();
    }
    if (oldWorker) {
        oldWorker->stop();
        oldWorker.reset();
    }

    Status opened = Status::UnsupportedFormat;
    auto decoder = openDecoder(path.c_str(), &opened);
    if (!decoder) {
        trackChanging_.store(false, std::memory_order_release);
        (void)stop();
        return opened;
    }

    const AudioFormat fmt = decoder->format();
    if (fmt.channelCount != 2) {
        trackChanging_.store(false, std::memory_order_release);
        (void)stop();
        return Status::UnsupportedFormat;
    }

    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (state_ != EngineState::Running || !ring_ || !output_) {
            trackChanging_.store(false, std::memory_order_release);
            return Status::InvalidState;
        }

        // Reopen Oboe only when rate/channels no longer match the open stream.
        if (fmt.sampleRate != output_->sampleRate() ||
            fmt.channelCount != output_->channelCount()) {
            (void)output_->stop();
            output_ = std::make_unique<AudioOutput>();
            output_->setOutputMode(OutputMode::Ring, ring_.get());
            output_->setPositionFrames(&positionFrames_);
            const Status outSt = output_->start();
            if (outSt != Status::Ok) {
                trackChanging_.store(false, std::memory_order_release);
                state_ = EngineState::Idle;
                return outSt;
            }
            if (fmt.sampleRate != output_->sampleRate()) {
                (void)output_->stop();
                output_.reset();
                trackChanging_.store(false, std::memory_order_release);
                state_ = EngineState::Idle;
                return Status::UnsupportedFormat;
            }
            prepareProcessGraphLocked();
        }

        durationFrames_ = decoder->frameCount();
        fileSampleRate_ = fmt.sampleRate;
        loadedPath_ = path;
        positionFrames_.store(0, std::memory_order_relaxed);

        auto worker = std::make_unique<DecodeWorker>();
        const Status wSt = worker->start(std::move(decoder), ring_.get());
        if (wSt != Status::Ok) {
            trackChanging_.store(false, std::memory_order_release);
            (void)output_->stop();
            output_.reset();
            state_ = EngineState::Idle;
            return wSt;
        }
        (void)worker->prefill(8);
        worker_ = std::move(worker);

        if (!wasPaused) {
            worker_->setPushEnabled(true);
            output_->setPaused(false);
            paused_.store(false, std::memory_order_release);
        } else {
            worker_->setPushEnabled(false);
            output_->setPaused(true);
            paused_.store(true, std::memory_order_release);
        }
    }

    trackChanging_.store(false, std::memory_order_release);
    return Status::Ok;
}

Status Engine::handleEofAdvance() {
    RepeatMode mode;
    std::string path;
    bool playNext = false;
    bool repeatOne = false;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (state_ != EngineState::Running || playMode_ != PlayMode::File) {
            return Status::InvalidState;
        }
        mode = queue_.repeatMode();
        if (mode == RepeatMode::One) {
            repeatOne = true;
        } else {
            playNext = queue_.advanceNext();
            if (playNext) {
                const Status sync = syncLoadedFromQueue();
                if (sync != Status::Ok) {
                    return sync;
                }
                path = loadedPath_;
            }
        }
    }

    if (repeatOne) {
        return seekFrames(0);
    }
    if (playNext) {
        return changeTrack(path);
    }
    // Repeat::None at end of queue — stop cleanly.
    return stop();
}

void Engine::startQueueWatch() {
    stopQueueWatch();
    queueWatchRunning_.store(true, std::memory_order_release);
    queueWatchThread_ = std::thread([this] { queueWatchMain(); });
}

void Engine::stopQueueWatch() {
    queueWatchRunning_.store(false, std::memory_order_release);
    if (queueWatchThread_.joinable()) {
        // EOF → stop() may run on this thread; never join ourselves.
        if (std::this_thread::get_id() != queueWatchThread_.get_id()) {
            queueWatchThread_.join();
        } else {
            queueWatchThread_.detach();
        }
    }
}

void Engine::queueWatchMain() {
    while (queueWatchRunning_.load(std::memory_order_acquire)) {
        bool shouldAdvance = false;
        {
            std::lock_guard<std::mutex> lock(mutex_);
            if (state_ == EngineState::Running && playMode_ == PlayMode::File && worker_ &&
                ring_ && !paused_.load(std::memory_order_acquire) &&
                !trackChanging_.load(std::memory_order_acquire)) {
                if (worker_->reachedEof() && ring_->availableRead() == 0) {
                    shouldAdvance = true;
                }
            }
        }
        if (shouldAdvance) {
            (void)handleEofAdvance();
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(20));
    }
}

#endif  // __ANDROID__

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

int32_t howl_engine_enqueue(HowlEngineHandle* engine, const char* path) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->enqueue(path));
}

int32_t howl_engine_clear_queue(HowlEngineHandle* engine) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->clearQueue());
}

int32_t howl_engine_play_at(HowlEngineHandle* engine, int32_t index) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->playAt(index));
}

int32_t howl_engine_next(HowlEngineHandle* engine) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->next());
}

int32_t howl_engine_previous(HowlEngineHandle* engine) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->previous());
}

int32_t howl_engine_set_preamp_gain_db(HowlEngineHandle* engine, float gain_db) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->setPreampGainDb(gain_db));
}

int32_t howl_engine_set_eq_band_gain_db(HowlEngineHandle* engine, int32_t band, float gain_db) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->setEqBandGainDb(band, gain_db));
}

int32_t howl_engine_apply_eq_state(
    HowlEngineHandle* engine, float preamp_db, const float* bands_db, int32_t band_count) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    if (bands_db == nullptr || band_count != static_cast<int32_t>(howl::kEqBandCount)) {
        return static_cast<int32_t>(howl::Status::InvalidArgument);
    }
    howl::EqState state{preamp_db, {}};
    std::copy_n(bands_db, band_count, state.bandsDb.begin());
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->applyEqState(state));
}

int32_t howl_engine_reset_eq_to_flat(HowlEngineHandle* engine) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    reinterpret_cast<howl::Engine*>(engine)->resetEqToFlat();
    return static_cast<int32_t>(howl::Status::Ok);
}

int32_t howl_engine_set_repeat_mode(HowlEngineHandle* engine, int32_t mode) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::Status::NullHandle);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->setRepeatMode(
        static_cast<howl::RepeatMode>(mode)));
}

int32_t howl_engine_queue_size(HowlEngineHandle* engine) {
    if (engine == nullptr) {
        return 0;
    }
    return reinterpret_cast<howl::Engine*>(engine)->queueSize();
}

int32_t howl_engine_current_index(HowlEngineHandle* engine) {
    if (engine == nullptr) {
        return -1;
    }
    return reinterpret_cast<howl::Engine*>(engine)->currentIndex();
}

int32_t howl_engine_repeat_mode(HowlEngineHandle* engine) {
    if (engine == nullptr) {
        return static_cast<int32_t>(howl::RepeatMode::None);
    }
    return static_cast<int32_t>(reinterpret_cast<howl::Engine*>(engine)->repeatMode());
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
