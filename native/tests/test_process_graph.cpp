#include "howl/dsp/process_graph.h"

#include <gtest/gtest.h>

#include <chrono>
#include <cstring>
#include <limits>
#include <vector>

TEST(ProcessGraph, IdentityPreservesSamples) {
    howl::ProcessGraph graph;
    graph.prepare(48000.0, 256, 2);
    ASSERT_TRUE(graph.isPrepared());
    graph.setBypassed(false);

    std::vector<float> buf(256 * 2);
    for (size_t i = 0; i < buf.size(); ++i) {
        buf[i] = static_cast<float>(i) * 0.001f;
    }
    std::vector<float> original = buf;

    graph.process(buf.data(), 256);

    for (size_t i = 0; i < buf.size(); ++i) {
        EXPECT_FLOAT_EQ(buf[i], original[i]) << "i=" << i;
    }
}

TEST(ProcessGraph, BypassIsNoOp) {
    howl::ProcessGraph graph;
    graph.prepare(48000.0, 128, 2);
    graph.setBypassed(true);

    std::vector<float> buf(128 * 2, 0.25f);
    std::vector<float> original = buf;
    graph.process(buf.data(), 128);
    EXPECT_EQ(0, std::memcmp(buf.data(), original.data(), buf.size() * sizeof(float)));
}

TEST(ProcessGraph, UnpreparedIsNoOp) {
    howl::ProcessGraph graph;
    EXPECT_FALSE(graph.isPrepared());
    std::vector<float> buf(64, 0.5f);
    std::vector<float> original = buf;
    graph.process(buf.data(), 32);
    EXPECT_EQ(0, std::memcmp(buf.data(), original.data(), buf.size() * sizeof(float)));
}

TEST(ProcessGraph, BackendNameIsNonEmpty) {
    const char* name = howl::ProcessGraph::backendName();
    ASSERT_NE(name, nullptr);
    EXPECT_GT(std::strlen(name), 0u);
    // Host default HOWL_WITH_JUCE=ON → juce_dsp; OFF → native.
    EXPECT_TRUE(std::strcmp(name, "juce_dsp") == 0 || std::strcmp(name, "native") == 0);
}

TEST(ProcessGraph, EmptyGraphCpuCostIsLow) {
    howl::ProcessGraph graph;
    graph.prepare(48000.0, 256, 2);
    graph.setBypassed(false);

    std::vector<float> buf(256 * 2, 0.1f);
    constexpr int kIters = 20000;

    const auto t0 = std::chrono::steady_clock::now();
    for (int i = 0; i < kIters; ++i) {
        graph.process(buf.data(), 256);
    }
    const auto t1 = std::chrono::steady_clock::now();
    const double ms =
        std::chrono::duration<double, std::milli>(t1 - t0).count();

    // Sanity ceiling: identity/unity path should be well under 500 ms for 20k×256
    // frames on a modern host (≈2.5 s of audio).
    EXPECT_LT(ms, 500.0) << "backend=" << howl::ProcessGraph::backendName();
}

TEST(ProcessGraph, PreampScalesAfterStereoRamp) {
    howl::ProcessGraph graph;
    graph.prepare(48000.0, 512, 2);
    ASSERT_TRUE(graph.setTargetGainDb(-6.0f));

    std::vector<float> buf(961 * 2, 1.0f);
    graph.process(buf.data(), 512);
    graph.process(buf.data() + 512 * 2, 449);
    EXPECT_GT(buf.front(), 0.999f);
    for (size_t i = 0; i < buf.size(); i += 2) {
        EXPECT_FLOAT_EQ(buf[i], buf[i + 1]);
        if (i > 0) {
            EXPECT_LE(buf[i], buf[i - 2]);
            EXPECT_LT(buf[i - 2] - buf[i], 0.001f);
        }
    }
    EXPECT_NEAR(buf[959 * 2], 0.501187f, 1.0e-6f);
    EXPECT_FLOAT_EQ(buf[959 * 2], buf[960 * 2]);
}

TEST(ProcessGraph, PreampClampsAndSurvivesPrepare) {
    howl::ProcessGraph graph;
    ASSERT_TRUE(graph.setTargetGainDb(-100.0f));
    graph.prepare(44100.0, 64, 1);

    float sample = 1.0f;
    graph.process(&sample, 1);
    EXPECT_NEAR(sample, 0.0630957f, 1.0e-6f);

    ASSERT_TRUE(graph.setTargetGainDb(100.0f));
    EXPECT_FALSE(graph.setTargetGainDb(std::numeric_limits<float>::quiet_NaN()));
    graph.prepare(48000.0, 64, 1);
    sample = 0.25f;
    graph.process(&sample, 1);
    EXPECT_NEAR(sample, 0.25f * 3.98107f, 1.0e-6f);
}

TEST(ProcessGraph, PreampRetargetsFromCurrentGain) {
    howl::ProcessGraph graph;
    graph.prepare(44100.0, 512, 1);
    ASSERT_TRUE(graph.setTargetGainDb(-24.0f));
    std::vector<float> buf(441, 1.0f);
    graph.process(buf.data(), 441);
    const float midRampGain = buf.back();
    EXPECT_NEAR(midRampGain, 0.531548f, 1.0e-4f);

    ASSERT_TRUE(graph.setTargetGainDb(0.0f));
    float sample = 1.0f;
    graph.process(&sample, 1);
    EXPECT_GT(sample, midRampGain);
    EXPECT_LT(sample - midRampGain, 0.001f);
}
