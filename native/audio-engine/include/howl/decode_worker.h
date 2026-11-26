#pragma once

#include "howl/decoder.h"
#include "howl/pcm_ring_buffer.h"
#include "howl/status.h"

#include <atomic>
#include <cstdint>
#include <memory>
#include <thread>
#include <vector>

namespace howl {

/**
 * Worker thread: IDecoder::read → PcmRingBuffer::push (Phase 2.2).
 *
 * Never runs on the audio callback. stop() signals and joins before the
 * ring or decoder may be destroyed.
 */
class DecodeWorker {
public:
    DecodeWorker() = default;
    ~DecodeWorker();

    DecodeWorker(const DecodeWorker&) = delete;
    DecodeWorker& operator=(const DecodeWorker&) = delete;

    /**
     * Takes ownership of decoder. ring is non-owning and must outlive the worker
     * until stop() returns.
     */
    Status start(std::unique_ptr<IDecoder> decoder, PcmRingBuffer* ring);

    /** Signal exit and join. Idempotent. */
    void stop();

    bool isRunning() const {
        return running_.load(std::memory_order_acquire);
    }

    /** True after decoder hit EOF (ring may still have audio). */
    bool reachedEof() const {
        return eof_.load(std::memory_order_acquire);
    }

private:
    void threadMain();

    std::unique_ptr<IDecoder> decoder_;
    PcmRingBuffer* ring_{nullptr};
    std::thread thread_;
    std::atomic<bool> running_{false};
    std::atomic<bool> eof_{false};
    std::vector<float> scratch_;  // preallocated on start
};

}  // namespace howl
