#pragma once

#include "howl/dsp/eq_state.h"

#include <array>
#include <cstdint>
#include <vector>

namespace howl {

class GraphicEq {
public:
    static constexpr std::array<double, kEqBandCount> kBandCentersHz{
        31.25, 62.5, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0,
    };

    /** Called by the graph with validated targets at a block boundary. */
    void setBandGainsDb(const std::array<float, kEqBandCount>& gainsDb) noexcept;
    void prepare(double sampleRate, int32_t channelCount);
    void reset() noexcept;
    void process(float* interleaved, int32_t frames) noexcept;

private:
    struct FilterState {
        double x1 = 0.0;
        double x2 = 0.0;
        double y1 = 0.0;
        double y2 = 0.0;
    };

    // Residual mix, a1, a2. Only the mix goes to zero at flat.
    using Coefficients = std::array<double, 3>;

    struct Band {
        float targetDb = 0.0f;
        float appliedDb = 0.0f;
        Coefficients current{};
        Coefficients destination{};
        Coefficients step{};
        int32_t framesRemaining = 0;
        std::vector<FilterState> channels;
    };

    static Coefficients makePeak(double sampleRate, double frequency, float gainDb) noexcept;
    std::array<Band, kEqBandCount> bands_;
    double sampleRate_ = 0.0;
    int32_t rampFrames_ = 1;
};

}  // namespace howl
