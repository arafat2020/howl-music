#include "howl/decode_worker.h"

#include <algorithm>
#include <chrono>
#include <utility>

namespace howl {
namespace {

constexpr int32_t kReadChunkFrames = 512;

}  // namespace

DecodeWorker::~DecodeWorker() {
    stop();
}

Status DecodeWorker::start(std::unique_ptr<IDecoder> decoder, PcmRingBuffer* ring) {
    stop();
    if (!decoder || !ring || !decoder->isOpen()) {
        return Status::InvalidState;
    }
    if (decoder->format().channelCount != ring->channelCount()) {
        return Status::UnsupportedFormat;
    }

    decoder_ = std::move(decoder);
    ring_ = ring;
    handoff_.reset(decoder_->frameCount());
    writtenFrames_ = 0;
    eof_.store(false, std::memory_order_relaxed);
    // Start with push disabled so the control thread can prefill / seek safely
    // before enabling (Engine::start / seekFrames).
    pushEnabled_.store(false, std::memory_order_relaxed);
    scratch_.assign(
        static_cast<size_t>(kReadChunkFrames) * static_cast<size_t>(ring_->channelCount()),
        0.0f);
    running_.store(true, std::memory_order_release);
    thread_ = std::thread([this] { threadMain(); });
    return Status::Ok;
}

void DecodeWorker::stop() {
    running_.store(false, std::memory_order_release);
    pushEnabled_.store(true, std::memory_order_release);
    if (thread_.joinable()) {
        thread_.join();
    }
    decoder_.reset();
    handoff_.reset(0);
    ring_ = nullptr;
    scratch_.clear();
}

void DecodeWorker::setPushEnabled(bool enabled) {
    std::lock_guard lock(decoderMutex_);
    pushEnabled_.store(enabled, std::memory_order_release);
}

Status DecodeWorker::seekToFrame(int64_t frameIndex, int64_t consumed) {
    std::lock_guard lock(decoderMutex_);
    if (!decoder_ || !decoder_->isOpen()) {
        return Status::NotRunning;
    }
    const auto start = handoff_.playback(consumed).track.startFrame;
    const Status st = handoff_.seek(decoder_, frameIndex, consumed);
    if (st != Status::Ok) {
        return st;
    }
    writtenFrames_ = start + frameIndex;
    eof_.store(false, std::memory_order_release);
    return Status::Ok;
}

Status DecodeWorker::setNext(std::unique_ptr<IDecoder> decoder, int64_t token, int64_t acknowledged) {
    // Decode a head block before acquiring the active worker's lock.
    std::vector<float> head;
    if (decoder && decoder->format().channelCount == 2) {
        head.resize(kReadChunkFrames * 2);
        int32_t frames = 0;
        if (decoder->read(head.data(), kReadChunkFrames, &frames) == Status::Ok && frames > 0) {
            head.resize(static_cast<std::size_t>(frames) * 2);
        } else {
            head.clear();
            decoder.reset();
        }
    }
    std::lock_guard lock(decoderMutex_);
    if (!decoder_) return Status::NotRunning;
    const auto result = handoff_.configure(std::move(decoder), std::move(head),
                                          token, acknowledged, decoder_->format());
    if (result == Status::Ok && token != 0) eof_.store(false, std::memory_order_release);
    return result;
}

GaplessPlayback DecodeWorker::playback(int64_t consumed) const {
    std::lock_guard lock(decoderMutex_);
    auto playback = handoff_.playback(consumed);
    playback.writtenThrough = writtenFrames_;
    return playback;
}

int64_t DecodeWorker::decoderPositionFrames() const {
    std::lock_guard lock(decoderMutex_);
    if (!decoder_ || !decoder_->isOpen()) {
        return 0;
    }
    return decoder_->positionFrames();
}

int64_t DecodeWorker::decoderDurationFrames() const {
    std::lock_guard lock(decoderMutex_);
    if (!decoder_ || !decoder_->isOpen()) {
        return 0;
    }
    return decoder_->frameCount();
}

int32_t DecodeWorker::decoderSampleRate() const {
    std::lock_guard lock(decoderMutex_);
    if (!decoder_ || !decoder_->isOpen()) {
        return 0;
    }
    return decoder_->format().sampleRate;
}

Status DecodeWorker::prefill(int32_t maxChunks) {
    std::lock_guard lock(decoderMutex_);
    if (!decoder_ || !ring_) return Status::NotRunning;
    for (int32_t i = 0; i < maxChunks && fillChunk(); ++i) {}
    return Status::Ok;
}

bool DecodeWorker::fillChunk() {
    const int32_t capacity = std::min(kReadChunkFrames, ring_->availableWrite());
    if (capacity <= 0 || eof_.load(std::memory_order_relaxed)) return false;

    int32_t frames = 0;
    auto status = handoff_.read(*decoder_, scratch_.data(), capacity, &frames);
    if (status == Status::Ok && frames == 0 &&
        handoff_.stitch(decoder_, writtenFrames_, ring_->availableRead() != 0)) {
        status = handoff_.read(*decoder_, scratch_.data(), capacity, &frames);
    }
    if (status != Status::Ok || frames <= 0) {
        eof_.store(true, std::memory_order_release);
        return false;
    }

    // The consumer only increases the room available between the read and push.
    writtenFrames_ += ring_->push(scratch_.data(), frames);
    return true;
}

void DecodeWorker::threadMain() {
    while (running_.load(std::memory_order_acquire)) {
        if (!pushEnabled_.load(std::memory_order_acquire) || reachedEof()) {
            std::this_thread::sleep_for(std::chrono::milliseconds(3));
            continue;
        }
        {
            std::lock_guard lock(decoderMutex_);
            if (pushEnabled_.load(std::memory_order_relaxed) && fillChunk()) continue;
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(1));
    }
}

}  // namespace howl
