#pragma once

#include <atomic>
#include <cstdint>
#include <vector>

namespace howl {

/**
 * Single-producer / single-consumer interleaved Float32 PCM ring (Phase 2.2).
 *
 * - Capacity is fixed at construction (power-of-two frame count).
 * - push(): worker/control only — never blocks; returns frames written (may be 0 if full).
 * - pop(): audio callback only — never blocks; returns frames read (0 if empty).
 * - No heap allocation after construction. No mutexes.
 *
 * SPSC assumption: exactly one thread calls push, exactly one calls pop.
 */
class PcmRingBuffer {
public:
    /**
     * @param capacityFrames power-of-two frame slots (e.g. 16384).
     * @param channelCount interleaved channels (1 or 2).
     */
    PcmRingBuffer(int32_t capacityFrames, int32_t channelCount);

    PcmRingBuffer(const PcmRingBuffer&) = delete;
    PcmRingBuffer& operator=(const PcmRingBuffer&) = delete;

    int32_t channelCount() const { return channels_; }
    int32_t capacityFrames() const { return capacity_; }

    /** Frames available for the consumer to read. */
    int32_t availableRead() const;

    /** Frames the producer can write without discarding. */
    int32_t availableWrite() const;

    /** Non-blocking push. Returns frames actually written. */
    int32_t push(const float* interleavedIn, int32_t frames);

    /** Non-blocking pop. Returns frames actually read (remainder untouched). */
    int32_t pop(float* interleavedOut, int32_t frames);

    /** Control-thread reset when idle (not while push/pop run). */
    void clear();

private:
    static int32_t nextPow2(int32_t v);

    int32_t capacity_{0};
    int32_t mask_{0};
    int32_t channels_{0};
    std::vector<float> storage_;
    // Indices in *frames* (not samples). write - read = filled.
    alignas(64) std::atomic<uint32_t> writeIndex_{0};
    alignas(64) std::atomic<uint32_t> readIndex_{0};
};

}  // namespace howl
