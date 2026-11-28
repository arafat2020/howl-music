#pragma once

#include <algorithm>
#include <atomic>
#include <bit>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <limits>

namespace howl {

/** Linear amplitudes across all output channels. 1.0 is digital full scale. */
struct AnalysisSnapshot {
    float peak{0.0f};
    float rms{0.0f};
};

/** One output callback publishes a coherent block; readers never wait or retry. */
class LevelMeter {
public:
    void publish(const float* samples, std::size_t count) noexcept {
        float peak = 0.0f;
        double squared = 0.0;
        for (std::size_t i = 0; i < count; ++i) {
            const float magnitude = std::abs(samples[i]);
            if (magnitude <= std::numeric_limits<float>::max()) {
                peak = std::max(peak, magnitude);
                squared += static_cast<double>(magnitude) * magnitude;
            }
        }
        const float meanSquare = count == 0 ? 0.0f : static_cast<float>(
            std::min(squared / static_cast<double>(count), static_cast<double>(std::numeric_limits<float>::max())));
        const uint64_t packed = (static_cast<uint64_t>(std::bit_cast<uint32_t>(peak)) << 32) |
            std::bit_cast<uint32_t>(meanSquare);
        block_.store(packed, std::memory_order_relaxed);
    }

    void clear() noexcept { block_.store(0, std::memory_order_relaxed); }

    AnalysisSnapshot snapshot() const noexcept {
        const uint64_t packed = block_.load(std::memory_order_relaxed);
        return {
            std::bit_cast<float>(static_cast<uint32_t>(packed >> 32)),
            std::sqrt(std::bit_cast<float>(static_cast<uint32_t>(packed))),
        };
    }

private:
    // Both values travel in one word. No other data depends on this publication.
    static_assert(std::atomic<uint64_t>::is_always_lock_free);
    std::atomic<uint64_t> block_{0};
};

}  // namespace howl
