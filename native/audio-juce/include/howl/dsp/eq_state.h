#pragma once

#include <array>
#include <cstddef>

namespace howl {

inline constexpr std::size_t kEqBandCount = 10;

struct EqState {
    float preampDb;
    std::array<float, kEqBandCount> bandsDb;
};

}  // namespace howl
