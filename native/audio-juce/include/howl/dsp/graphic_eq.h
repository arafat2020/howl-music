#pragma once

#include <array>
#include <atomic>
#include <cstddef>
#include <cstdint>
#include <vector>

namespace howl {

class GraphicEq {
public:
    static constexpr std::array<double, 10> kBandCentersHz{
        31.25, 62.5, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0,
    };

    /** Control-thread update. Finite gains clamp to [-12, +12] dB. */
    bool setBandGainDb(int32_t band, float gainDb) noexcept;

    /** Call while processing is stopped. Band settings survive preparation. */
    void prepare(double sampleRate, int32_t channelCount);
    void reset() noexcept;

    /** Audio thread. Coefficients and channel histories are updated in place. */
    void process(float* interleaved, int32_t frames) noexcept;

private:
    struct FilterState {
        double z1 = 0.0;
        double z2 = 0.0;
    };

    struct Band {
        std::atomic<float> targetDb{0.0f};
        float appliedDb = 0.0f;
        // Normalized b0, b1, b2, a1, a2.
        std::array<double, 5> coefficients{};
        std::vector<FilterState> channels;
    };

    static_assert(std::atomic<float>::is_always_lock_free);
    std::array<Band, kBandCentersHz.size()> bands_;
    double sampleRate_ = 0.0;
};

}  // namespace howl
