#pragma once

#include <cstdint>

namespace howl {

class GainNode {
public:
    // Called by the graph with a validated target.
    void setTargetGainDb(float gainDb) noexcept;
    void prepare(double sampleRate) noexcept;
    void reset() noexcept;
    void process(float* samples, int32_t frames, int32_t channels) noexcept;

private:
    float targetDb_ = 0.0f;
    float target_ = 1.0f;
    float current_ = 1.0f;
    float destination_ = 1.0f;
    float step_ = 0.0f;
    int32_t rampFrames_ = 1;
    int32_t framesRemaining_ = 0;
};

}  // namespace howl
