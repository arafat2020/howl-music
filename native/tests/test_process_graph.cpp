#include "howl/dsp/process_graph.h"

#include <gtest/gtest.h>

#include <chrono>
#include <cstring>
#include <string>
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
    // Host default HOWL_WITH_JUCE=ON → juce_dsp; OFF → identity.
    EXPECT_TRUE(std::strcmp(name, "juce_dsp") == 0 || std::strcmp(name, "identity") == 0);
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
