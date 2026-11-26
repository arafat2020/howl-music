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
    scratch_.assign(
        static_cast<size_t>(kReadChunkFrames) * static_cast<size_t>(ring_->channelCount()),
        0.0f);
    running_.store(true, std::memory_order_release);
    thread_ = std::thread([this] { threadMain(); });
    return Status::Ok;
}

void DecodeWorker::stop() {
    running_.store(false, std::memory_order_release);
    if (thread_.joinable()) {
        thread_.join();
    }
    decoder_.reset();
    ring_ = nullptr;
    scratch_.clear();
}

void DecodeWorker::threadMain() {
    while (running_.load(std::memory_order_acquire)) {
        if (eof_.load(std::memory_order_acquire)) {
            // Keep thread alive until stop so Engine can join cleanly; sleep lightly.
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
        while (offset < framesRead && running_.load(std::memory_order_acquire)) {
            const int32_t pushed = ring_->push(
                scratch_.data() + offset * ring_->channelCount(),
                framesRead - offset);
            if (pushed == 0) {
                // Ring full — back off; never block the audio thread.
                std::this_thread::sleep_for(std::chrono::milliseconds(1));
                continue;
            }
            offset += pushed;
        }
    }
}

}  // namespace howl
