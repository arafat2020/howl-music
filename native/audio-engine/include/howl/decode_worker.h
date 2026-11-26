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
 * Worker thread: IDecoder::read → PcmRingBuffer::push (Phase 2.2/2.3).
 *
 * Control thread may pause pushes and seek the decoder while the audio
 * callback is also paused (no concurrent pop) — see Engine::seek.
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

    bool reachedEof() const {
        return eof_.load(std::memory_order_acquire);
    }

    /** When false, worker does not read/push (seek/pause coordination). */
    void setPushEnabled(bool enabled);

    bool pushEnabled() const {
        return pushEnabled_.load(std::memory_order_acquire);
    }

    /**
     * Seek decoder. Call only while push is disabled and the audio callback
     * is not popping (output paused). Clears EOF so decoding can continue.
     */
    Status seekToFrame(int64_t frameIndex);

    int64_t decoderPositionFrames() const;
    int64_t decoderDurationFrames() const;
    int32_t decoderSampleRate() const;

    /** Prefill ring from current decoder position (control thread, push disabled). */
    Status prefill(int32_t maxChunks);

private:
    void threadMain();

    std::unique_ptr<IDecoder> decoder_;
    PcmRingBuffer* ring_{nullptr};
    std::thread thread_;
    std::atomic<bool> running_{false};
    std::atomic<bool> eof_{false};
    std::atomic<bool> pushEnabled_{true};
    std::vector<float> scratch_;
};

}  // namespace howl
