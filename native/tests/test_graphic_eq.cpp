#include "howl/dsp/graphic_eq.h"
#include "howl/dsp/process_graph.h"
#include "howl/engine.h"

#include <gtest/gtest.h>

#include <algorithm>
#include <array>
#include <cmath>
#include <limits>
#include <numbers>
#include <vector>

namespace {

constexpr int32_t kOneKhzBand = 5;
constexpr int32_t kHighestBand = 9;

double measureLeftGain(howl::ProcessGraph& graph, double frequency) {
    constexpr int32_t kFrames = 256;
    std::array<float, kFrames * 2> block{};
    double inputEnergy = 0.0;
    double outputEnergy = 0.0;
    double maxRight = 0.0;
    for (int32_t i = 0; i < 128; ++i) {
        for (int32_t frame = 0; frame < kFrames; ++frame) {
            const double time = (i * kFrames + frame) / graph.sampleRate();
            const float input = 0.1f * std::sin(2.0 * std::numbers::pi * frequency * time);
            block[frame * 2] = input;
            block[frame * 2 + 1] = 0.0f;
            if (i >= 64) inputEnergy += input * input;
        }
        graph.process(block.data(), kFrames);
        for (int32_t frame = 0; frame < kFrames; ++frame) {
            const float output = block[frame * 2];
            if (i >= 64) outputEnergy += output * output;
            maxRight = std::max(maxRight, std::abs(static_cast<double>(block[frame * 2 + 1])));
        }
    }
    EXPECT_EQ(maxRight, 0.0);
    return std::sqrt(outputEnergy / inputEnergy);
}

}  // namespace

TEST(GraphicEq, BoostsCenterAndLeavesDistantFrequenciesNearUnity) {
    howl::ProcessGraph graph;
    graph.prepare(48000.0, 256, 2);
    ASSERT_TRUE(graph.setEqBandGainDb(kOneKhzBand, 12.0f));

    EXPECT_NEAR(measureLeftGain(graph, 1000.0), 3.981, 0.01);
    EXPECT_LT(measureLeftGain(graph, 250.0), 1.2);
    EXPECT_LT(measureLeftGain(graph, 4000.0), 1.2);
}

TEST(GraphicEq, ClampsCutAndRejectsInvalidUpdates) {
    howl::ProcessGraph graph;
    graph.prepare(44100.0, 256, 2);
    ASSERT_TRUE(graph.setEqBandGainDb(kOneKhzBand, -100.0f));
    EXPECT_FALSE(graph.setEqBandGainDb(kOneKhzBand, std::numeric_limits<float>::infinity()));
    EXPECT_FALSE(graph.setEqBandGainDb(-1, 0.0f));
    EXPECT_FALSE(graph.setEqBandGainDb(kHighestBand + 1, 0.0f));

    EXPECT_NEAR(measureLeftGain(graph, 1000.0), 0.2512, 0.001);
    ASSERT_TRUE(graph.setEqBandGainDb(kOneKhzBand, 0.0f));
    EXPECT_DOUBLE_EQ(measureLeftGain(graph, 1000.0), 1.0);
}

TEST(GraphicEq, RepreparesAndSkipsBandsAtOrAboveNyquist) {
    howl::ProcessGraph graph;
    ASSERT_TRUE(graph.setEqBandGainDb(kHighestBand, 100.0f));

    graph.prepare(48000.0, 256, 2);
    EXPECT_NEAR(measureLeftGain(graph, 16000.0), 3.981, 0.01);

    for (auto rate : {32000.0, 24000.0}) {
        graph.prepare(rate, 256, 2);
        EXPECT_DOUBLE_EQ(measureLeftGain(graph, 10000.0), 1.0);
    }

    graph.prepare(44100.0, 256, 2);
    EXPECT_NEAR(measureLeftGain(graph, 16000.0), 3.981, 0.01);
}

TEST(GraphicEq, EngineApiRejectsInvalidBandAndNullHandle) {
    auto engine = howl::Engine::create();
    EXPECT_EQ(engine->setEqBandGainDb(kOneKhzBand, 6.0f), howl::Status::Ok);
    EXPECT_EQ(engine->setEqBandGainDb(-1, 6.0f), howl::Status::InvalidArgument);
    EXPECT_EQ(howl_engine_set_eq_band_gain_db(nullptr, kOneKhzBand, 6.0f),
              static_cast<int32_t>(howl::Status::NullHandle));
}

TEST(GraphicEq, AllBandsAtExtremesStayFinite) {
    howl::ProcessGraph graph;
    graph.prepare(48000.0, 256, 2);
    for (int32_t band = 0; band <= kHighestBand; ++band) {
        ASSERT_TRUE(graph.setEqBandGainDb(band, 12.0f));
    }

    std::vector<float> samples(48000 * 2, 0.0f);
    samples.front() = 0.1f;
    for (std::size_t first = 0; first < samples.size(); first += 512) {
        const auto count = std::min<std::size_t>(512, samples.size() - first);
        graph.process(samples.data() + first, static_cast<int32_t>(count / 2));
    }
    EXPECT_TRUE(std::all_of(samples.begin(), samples.end(), [](float sample) {
        return std::isfinite(sample);
    }));
    EXPECT_NEAR(samples[samples.size() - 2], 0.0f, 1.0e-6f);
}
