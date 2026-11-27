#include "gain_node.h"

#include <algorithm>
#include <cmath>
#include <limits>

namespace howl {
namespace {
constexpr float kMinGainDb = -24.0f;
constexpr float kMaxGainDb = 12.0f;
constexpr double kRampSeconds = 0.020;
}  // namespace

bool GainNode::setTargetGainDb(float gainDb) noexcept {
    if (!std::isfinite(gainDb)) {
        return false;
    }
    const float clamped = std::clamp(gainDb, kMinGainDb, kMaxGainDb);
    target_.store(std::pow(10.0f, clamped / 20.0f), std::memory_order_relaxed);
    return true;
}

void GainNode::prepare(double sampleRate) noexcept {
    rampFrames_ = static_cast<int32_t>(std::clamp(
        std::ceil(sampleRate * kRampSeconds), 1.0,
        static_cast<double>(std::numeric_limits<int32_t>::max())));
    reset();
}

void GainNode::reset() noexcept {
    current_ = destination_ = target_.load(std::memory_order_relaxed);
    framesRemaining_ = 0;
}

void GainNode::process(float* samples, int32_t frames, int32_t channels) noexcept {
    const float target = target_.load(std::memory_order_relaxed);
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
