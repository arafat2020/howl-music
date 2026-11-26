#pragma once

#include <cmath>
#include <cstdint>

namespace howl {

/**
 * Deterministic sine oscillator (host-testable, Phase 1.4).
 *
 * Pure math — no heap, no I/O. Safe to call from an audio callback once
 * constructed on the control thread.
 */
class SineOsc {
public:
    static constexpr double kTwoPi = 6.28318530717958647692;

    SineOsc() = default;

    void configure(double frequencyHz, int32_t sampleRate) {
        phase_ = 0.0;
        if (sampleRate > 0 && frequencyHz > 0.0) {
            phaseIncrement_ = (kTwoPi * frequencyHz) / static_cast<double>(sampleRate);
        } else {
            phaseIncrement_ = 0.0;
        }
    }

    void reset() { phase_ = 0.0; }

    /** Advance one sample and return amplitude * sin(phase). */
    float next(float amplitude) {
        const float sample = amplitude * static_cast<float>(std::sin(phase_));
        phase_ += phaseIncrement_;
        if (phase_ >= kTwoPi) {
            phase_ -= kTwoPi;
        }
        return sample;
    }

    double phase() const { return phase_; }
    double phaseIncrement() const { return phaseIncrement_; }

private:
    double phase_{0.0};
    double phaseIncrement_{0.0};
};

}  // namespace howl
