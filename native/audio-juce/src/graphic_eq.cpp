#include "howl/dsp/graphic_eq.h"

#include <algorithm>
#include <cmath>
#include <limits>
#include <numbers>

#if HOWL_WITH_JUCE
#include <juce_dsp/juce_dsp.h>
#endif

namespace howl {
namespace {

constexpr double kBandQ = std::numbers::sqrt2;
constexpr double kRampSeconds = 0.020;

}  // namespace

GraphicEq::Coefficients GraphicEq::makePeak(
    double sampleRate, double frequency, float gainDb) noexcept {
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
    // A normalized peaking biquad equals the dry signal plus this band-pass residual.
    // Keeping its mix separate lets the residual fade to exact identity at 0 dB.
    return {(c[0] - c[3]) / c[3], c[4] / c[3], c[5] / c[3]};
}

void GraphicEq::setBandGainsDb(const std::array<float, kEqBandCount>& gainsDb) noexcept {
    for (std::size_t i = 0; i < bands_.size(); ++i) {
        bands_[i].targetDb = gainsDb[i];
    }
}

void GraphicEq::prepare(double sampleRate, int32_t channelCount) {
    if (!std::isfinite(sampleRate) || sampleRate <= 0.0 || channelCount <= 0) {
        sampleRate_ = 0.0;
        return;
    }
    sampleRate_ = sampleRate;
    rampFrames_ = static_cast<int32_t>(std::clamp(
        std::ceil(sampleRate * kRampSeconds), 1.0,
        static_cast<double>(std::numeric_limits<int32_t>::max())));
    for (auto& band : bands_) {
        band.channels.resize(static_cast<std::size_t>(channelCount));
    }
    reset();
}

void GraphicEq::reset() noexcept {
    for (std::size_t i = 0; i < bands_.size(); ++i) {
        auto& band = bands_[i];
        band.appliedDb = band.targetDb;
        const double frequency = kBandCentersHz[i];
        if (frequency < sampleRate_ * 0.5) {
            band.current = band.destination = makePeak(sampleRate_, frequency, band.targetDb);
        }
        band.framesRemaining = 0;
        std::fill(band.channels.begin(), band.channels.end(), FilterState{});
    }
}

void GraphicEq::process(float* interleaved, int32_t frames) noexcept {
    if (interleaved == nullptr || frames <= 0) {
        return;
    }
    for (std::size_t i = 0; i < bands_.size(); ++i) {
        const double frequency = kBandCentersHz[i];
        if (frequency >= sampleRate_ * 0.5) {
            break;
        }

        auto& band = bands_[i];
        if (band.targetDb != band.appliedDb) {
            band.appliedDb = band.targetDb;
            band.destination = makePeak(sampleRate_, frequency, band.targetDb);
            band.framesRemaining = rampFrames_;
            // The stability inequalities for a normalized second-order denominator are linear.
            // Interpolating between these stable endpoints keeps each intermediate pole set stable.
            for (std::size_t j = 0; j < band.step.size(); ++j) {
                band.step[j] = (band.destination[j] - band.current[j]) / rampFrames_;
            }
        }

        float* sample = interleaved;
        auto processFrame = [&](const Coefficients& coefficients) {
            const auto [mix, a1, a2] = coefficients;
            for (auto& state : band.channels) {
                const double input = *sample;
                const double residual = input - state.x2 - a1 * state.y1 - a2 * state.y2;
                *sample++ = static_cast<float>(input + mix * residual);
                state.x2 = state.x1;
                state.x1 = input;
                state.y2 = state.y1;
                state.y1 = residual;
            }
        };

        const int32_t ramping = std::min(frames, band.framesRemaining);
        for (int32_t frame = 0; frame < ramping; ++frame) {
            for (std::size_t j = 0; j < band.step.size(); ++j) {
                band.current[j] += band.step[j];
            }
            if (--band.framesRemaining == 0) {
                band.current = band.destination;
            }
            processFrame(band.current);
        }

        if (band.framesRemaining == 0 && band.current[0] == 0.0) {
            if (ramping > 0) {
                std::fill(band.channels.begin(), band.channels.end(), FilterState{});
            }
            continue;
        }

        for (int32_t frame = ramping; frame < frames; ++frame) {
            processFrame(band.current);
        }

        for (auto& state : band.channels) {
            if (std::abs(state.y1) < 1.0e-30) state.y1 = 0.0;
            if (std::abs(state.y2) < 1.0e-30) state.y2 = 0.0;
        }
    }
}

}  // namespace howl
