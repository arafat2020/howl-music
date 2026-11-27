#include "gain_node.h"

#include <algorithm>
#include <cmath>
#include <limits>

namespace howl {
namespace {
constexpr double kRampSeconds = 0.020;
}  // namespace

void GainNode::setTargetGainDb(float gainDb) noexcept {
    if (targetDb_ != gainDb) {
        targetDb_ = gainDb;
        target_ = std::pow(10.0f, gainDb / 20.0f);
    }
}

void GainNode::prepare(double sampleRate) noexcept {
    rampFrames_ = static_cast<int32_t>(std::clamp(
        std::ceil(sampleRate * kRampSeconds), 1.0,
        static_cast<double>(std::numeric_limits<int32_t>::max())));
    reset();
}

void GainNode::reset() noexcept {
    current_ = destination_ = target_;
    framesRemaining_ = 0;
}

void GainNode::process(float* samples, int32_t frames, int32_t channels) noexcept {
    const float target = target_;
    if (target != destination_) {
        destination_ = target;
        framesRemaining_ = rampFrames_;
        step_ = (destination_ - current_) / static_cast<float>(rampFrames_);
    }

    const int32_t ramping = std::min(frames, framesRemaining_);
    for (int32_t frame = 0; frame < ramping; ++frame) {
        current_ += step_;
        if (--framesRemaining_ == 0) {
            current_ = destination_;
        }
        for (int32_t channel = 0; channel < channels; ++channel) {
            *samples++ *= current_;
        }
    }

    if (current_ != 1.0f) {
        for (int32_t frame = ramping; frame < frames; ++frame) {
            for (int32_t channel = 0; channel < channels; ++channel) {
                *samples++ *= current_;
            }
        }
    }
}

}  // namespace howl
