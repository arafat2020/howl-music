#include "howl/pcm_ring_buffer.h"

#include <algorithm>
#include <cstring>

namespace howl {

int32_t PcmRingBuffer::nextPow2(int32_t v) {
    if (v < 2) {
        return 2;
    }
    uint32_t u = static_cast<uint32_t>(v - 1);
    u |= u >> 1;
    u |= u >> 2;
    u |= u >> 4;
    u |= u >> 8;
    u |= u >> 16;
    return static_cast<int32_t>(u + 1);
}

PcmRingBuffer::PcmRingBuffer(int32_t capacityFrames, int32_t channelCount)
    : capacity_(nextPow2(capacityFrames)),
      mask_(capacity_ - 1),
      channels_(channelCount > 0 ? channelCount : 1),
      storage_(static_cast<size_t>(capacity_) * static_cast<size_t>(channels_), 0.0f) {}

int32_t PcmRingBuffer::availableRead() const {
    const uint32_t w = writeIndex_.load(std::memory_order_acquire);
    const uint32_t r = readIndex_.load(std::memory_order_relaxed);
    return static_cast<int32_t>(w - r);
}

int32_t PcmRingBuffer::availableWrite() const {
    return capacity_ - availableRead();
}

int32_t PcmRingBuffer::push(const float* interleavedIn, int32_t frames) {
    if (interleavedIn == nullptr || frames <= 0) {
        return 0;
    }
    const uint32_t w = writeIndex_.load(std::memory_order_relaxed);
    const uint32_t r = readIndex_.load(std::memory_order_acquire);
    const int32_t free = capacity_ - static_cast<int32_t>(w - r);
    const int32_t toWrite = std::min(frames, free);
    if (toWrite <= 0) {
        return 0;
    }

    for (int32_t i = 0; i < toWrite; ++i) {
        const uint32_t frameIdx = (w + static_cast<uint32_t>(i)) & static_cast<uint32_t>(mask_);
        const size_t dst = static_cast<size_t>(frameIdx) * static_cast<size_t>(channels_);
        const size_t src = static_cast<size_t>(i) * static_cast<size_t>(channels_);
        std::memcpy(storage_.data() + dst, interleavedIn + src,
                    static_cast<size_t>(channels_) * sizeof(float));
    }

    writeIndex_.store(w + static_cast<uint32_t>(toWrite), std::memory_order_release);
    return toWrite;
}

int32_t PcmRingBuffer::pop(float* interleavedOut, int32_t frames) {
    if (interleavedOut == nullptr || frames <= 0) {
        return 0;
    }
    const uint32_t r = readIndex_.load(std::memory_order_relaxed);
    const uint32_t w = writeIndex_.load(std::memory_order_acquire);
    const int32_t avail = static_cast<int32_t>(w - r);
    const int32_t toRead = std::min(frames, avail);
    if (toRead <= 0) {
        return 0;
    }

    for (int32_t i = 0; i < toRead; ++i) {
        const uint32_t frameIdx = (r + static_cast<uint32_t>(i)) & static_cast<uint32_t>(mask_);
        const size_t src = static_cast<size_t>(frameIdx) * static_cast<size_t>(channels_);
        const size_t dst = static_cast<size_t>(i) * static_cast<size_t>(channels_);
        std::memcpy(interleavedOut + dst, storage_.data() + src,
                    static_cast<size_t>(channels_) * sizeof(float));
    }

    readIndex_.store(r + static_cast<uint32_t>(toRead), std::memory_order_release);
    return toRead;
}

void PcmRingBuffer::clear() {
    writeIndex_.store(0, std::memory_order_relaxed);
    readIndex_.store(0, std::memory_order_relaxed);
}

}  // namespace howl
