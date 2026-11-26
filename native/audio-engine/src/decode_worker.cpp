#include "howl/decode_worker.h"

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
    ring_ = nullptr;
    scratch_.clear();
}

void DecodeWorker::setPushEnabled(bool enabled) {
    pushEnabled_.store(enabled, std::memory_order_release);
}

Status DecodeWorker::seekToFrame(int64_t frameIndex) {
    if (!decoder_ || !decoder_->isOpen()) {
        return Status::NotRunning;
    }
    const Status st = decoder_->seek(frameIndex);
    if (st != Status::Ok) {
        return st;
    }
    eof_.store(false, std::memory_order_release);
    return Status::Ok;
}

int64_t DecodeWorker::decoderPositionFrames() const {
    if (!decoder_ || !decoder_->isOpen()) {
        return 0;
    }
    return decoder_->positionFrames();
}

int64_t DecodeWorker::decoderDurationFrames() const {
    if (!decoder_ || !decoder_->isOpen()) {
        return 0;
    }
    return decoder_->frameCount();
}

int32_t DecodeWorker::decoderSampleRate() const {
    if (!decoder_ || !decoder_->isOpen()) {
        return 0;
    }
    return decoder_->format().sampleRate;
}

Status DecodeWorker::prefill(int32_t maxChunks) {
    if (!decoder_ || !ring_ || !decoder_->isOpen()) {
        return Status::NotRunning;
    }
    for (int32_t i = 0; i < maxChunks; ++i) {
        int32_t n = 0;
        const Status st = decoder_->read(scratch_.data(), kReadChunkFrames, &n);
        if (st != Status::Ok || n <= 0) {
            if (n <= 0) {
                eof_.store(true, std::memory_order_release);
            }
            break;
        }
        int32_t offset = 0;
        while (offset < n) {
            const int32_t pushed =
                ring_->push(scratch_.data() + offset * ring_->channelCount(), n - offset);
            if (pushed == 0) {
                return Status::Ok;  // ring full
            }
            offset += pushed;
        }
    }
    return Status::Ok;
}

void DecodeWorker::threadMain() {
    while (running_.load(std::memory_order_acquire)) {
        if (!pushEnabled_.load(std::memory_order_acquire)) {
            std::this_thread::sleep_for(std::chrono::milliseconds(2));
            continue;
        }
        if (eof_.load(std::memory_order_acquire)) {
            std::this_thread::sleep_for(std::chrono::milliseconds(5));
            continue;
        }

        int32_t framesRead = 0;
        const Status st =
            decoder_->read(scratch_.data(), kReadChunkFrames, &framesRead);
        if (st != Status::Ok) {
            eof_.store(true, std::memory_order_release);
            continue;
        }
        if (framesRead <= 0) {
            eof_.store(true, std::memory_order_release);
            continue;
        }

        int32_t offset = 0;
        while (offset < framesRead && running_.load(std::memory_order_acquire) &&
               pushEnabled_.load(std::memory_order_acquire)) {
            const int32_t pushed = ring_->push(
                scratch_.data() + offset * ring_->channelCount(),
                framesRead - offset);
            if (pushed == 0) {
                std::this_thread::sleep_for(std::chrono::milliseconds(1));
                continue;
            }
            offset += pushed;
        }
    }
}

}  // namespace howl
