#include "howl/analysis_snapshot.h"
#include "howl/engine_api.h"

#include <gtest/gtest.h>

#include <atomic>
#include <limits>
#include <thread>

TEST(LevelMeter, ReadsBothChannelsAndSilence) {
    howl::LevelMeter meter;
    EXPECT_FLOAT_EQ(meter.snapshot().peak, 0.0f);
    EXPECT_FLOAT_EQ(meter.snapshot().rms, 0.0f);

    const float output[]{-0.5f, 0.25f, 1.25f, 0.0f};
    meter.publish(output, 4);
    const auto result = meter.snapshot();
    EXPECT_FLOAT_EQ(result.peak, 1.25f);
    EXPECT_FLOAT_EQ(result.rms, std::sqrt(1.875f / 4.0f));

    const float silent[4]{};
    meter.publish(silent, 4);
    EXPECT_FLOAT_EQ(meter.snapshot().peak, 0.0f);
    EXPECT_FLOAT_EQ(meter.snapshot().rms, 0.0f);
}

TEST(LevelMeter, IgnoresNonFiniteSamplesAndClears) {
    howl::LevelMeter meter;
    const float output[]{std::numeric_limits<float>::infinity(),
        std::numeric_limits<float>::quiet_NaN(), -0.5f, 0.5f};
    meter.publish(output, 4);
    EXPECT_FLOAT_EQ(meter.snapshot().peak, 0.5f);
    EXPECT_FLOAT_EQ(meter.snapshot().rms, std::sqrt(0.125f));
    meter.clear();
    EXPECT_FLOAT_EQ(meter.snapshot().peak, 0.0f);
    EXPECT_FLOAT_EQ(meter.snapshot().rms, 0.0f);
}

TEST(LevelMeter, PublishesCoherentPairsAcrossThreads) {
    howl::LevelMeter meter;
    std::atomic<bool> done{false};
    auto writer = std::thread([&] {
        const float quiet[]{0.25f, -0.25f};
        const float loud[]{0.75f, -0.75f};
        for (int i = 0; i < 100'000; ++i) {
            meter.publish(i % 2 == 0 ? quiet : loud, 2);
        }
        done.store(true);
    });
    bool coherent = true;
    do {
        const auto snapshot = meter.snapshot();
        coherent = coherent && snapshot.peak == snapshot.rms;
    } while (!done.load());
    writer.join();
    EXPECT_TRUE(coherent);
}

TEST(LevelMeter, CApiValidatesArgumentsAndReadsIdleSilence) {
    HowlAnalysisSnapshot snapshot{-1.0f, -1.0f};
    EXPECT_NE(howl_engine_get_analysis(nullptr, &snapshot), 0);
    auto* engine = howl_engine_create();
    ASSERT_NE(engine, nullptr);
    EXPECT_NE(howl_engine_get_analysis(engine, nullptr), 0);
    EXPECT_NE(howl_engine_set_analysis_enabled(engine, 2), 0);
    EXPECT_EQ(howl_engine_set_analysis_enabled(engine, 1), 0);
    EXPECT_EQ(howl_engine_get_analysis(engine, &snapshot), 0);
    EXPECT_FLOAT_EQ(snapshot.peak, 0.0f);
    EXPECT_FLOAT_EQ(snapshot.rms, 0.0f);
    howl_engine_destroy(engine);
}
