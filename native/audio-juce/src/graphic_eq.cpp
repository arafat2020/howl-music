#include "howl/dsp/graphic_eq.h"

#include <algorithm>
#include <cmath>
#include <numbers>

#if HOWL_WITH_JUCE
#include <juce_dsp/juce_dsp.h>
#endif

namespace howl {
namespace {

constexpr double kBandQ = std::numbers::sqrt2;
constexpr float kMaxGainDb = 12.0f;

std::array<double, 5> makePeak(double sampleRate, double frequency, float gainDb) noexcept {
#if HOWL_WITH_JUCE
    const double gain = std::pow(10.0, gainDb / 20.0);
    const auto c = juce::dsp::IIR::ArrayCoefficients<double>::makePeakFilter(
        sampleRate, frequency, kBandQ, gain);
#else
    // The same peaking biquad design when JUCE is disabled.
    const double a = std::pow(10.0, gainDb / 40.0);
    const double omega = 2.0 * std::numbers::pi * frequency / sampleRate;
    const double alpha = std::sin(omega) / (2.0 * kBandQ);
    const double c1 = -2.0 * std::cos(omega);
    const std::array c{1.0 + alpha * a, c1, 1.0 - alpha * a,
                       1.0 + alpha / a, c1, 1.0 - alpha / a};
#endif
    return {c[0] / c[3], c[1] / c[3], c[2] / c[3], c[4] / c[3], c[5] / c[3]};
}

}  // namespace

bool GraphicEq::setBandGainDb(int32_t band, float gainDb) noexcept {
    if (band < 0 || static_cast<std::size_t>(band) >= bands_.size() || !std::isfinite(gainDb)) {
        return false;
    }
    bands_[band].targetDb.store(std::clamp(gainDb, -kMaxGainDb, kMaxGainDb),
                                std::memory_order_relaxed);
    return true;
}

void GraphicEq::prepare(double sampleRate, int32_t channelCount) {
    if (!std::isfinite(sampleRate) || sampleRate <= 0.0 || channelCount <= 0) {
        sampleRate_ = 0.0;
        return;
    }
    for (auto& band : bands_) {
        band.channels.assign(static_cast<std::size_t>(channelCount), {});
        band.appliedDb = 0.0f;
    }
    sampleRate_ = sampleRate;
}

void GraphicEq::reset() noexcept {
    for (auto& band : bands_) {
        std::fill(band.channels.begin(), band.channels.end(), FilterState{});
    }
}

void GraphicEq::process(float* interleaved, int32_t frames) noexcept {
    if (sampleRate_ <= 0.0 || interleaved == nullptr || frames <= 0) {
        return;
    }

    for (std::size_t i = 0; i < bands_.size(); ++i) {
        const double frequency = kBandCentersHz[i];
        if (frequency >= sampleRate_ * 0.5) {
            break;
        }

        auto& band = bands_[i];
        const float targetDb = band.targetDb.load(std::memory_order_relaxed);
        if (targetDb != band.appliedDb) {
            band.appliedDb = targetDb;
            if (targetDb == 0.0f) {
                std::fill(band.channels.begin(), band.channels.end(), FilterState{});
            } else {
                band.coefficients = makePeak(sampleRate_, frequency, targetDb);
            }
        }
        if (targetDb == 0.0f) {
            continue;
        }

        const auto [b0, b1, b2, a1, a2] = band.coefficients;
        float* sample = interleaved;
        for (int32_t frame = 0; frame < frames; ++frame) {
            for (auto& state : band.channels) {
                const double input = *sample;
                const double output = b0 * input + state.z1;
                state.z1 = b1 * input - a1 * output + state.z2;
                state.z2 = b2 * input - a2 * output;
                *sample++ = static_cast<float>(output);
            }
        }
        // Flush inaudible tails so sustained silence cannot produce subnormal states.
        for (auto& state : band.channels) {
            if (std::abs(state.z1) < 1.0e-30) state.z1 = 0.0;
            if (std::abs(state.z2) < 1.0e-30) state.z2 = 0.0;
        }
    }
}

}  // namespace howl
