#include "howl/sine_osc.h"

#include <gtest/gtest.h>

#include <cmath>

TEST(SineOsc, ConfigureSetsPhaseIncrement) {
    howl::SineOsc osc;
    osc.configure(440.0, 48000);
    const double expected = (howl::SineOsc::kTwoPi * 440.0) / 48000.0;
    EXPECT_DOUBLE_EQ(osc.phaseIncrement(), expected);
    EXPECT_DOUBLE_EQ(osc.phase(), 0.0);
}

TEST(SineOsc, NextSampleDeterministic) {
    howl::SineOsc a;
    howl::SineOsc b;
    a.configure(440.0, 48000);
    b.configure(440.0, 48000);

    constexpr float kAmp = 0.5f;
    for (int i = 0; i < 2048; ++i) {
        EXPECT_FLOAT_EQ(a.next(kAmp), b.next(kAmp)) << "mismatch at sample " << i;
    }
}

TEST(SineOsc, MatchesStdSinFormula) {
    howl::SineOsc osc;
    osc.configure(440.0, 48000);

    double phase = 0.0;
    const double inc = (howl::SineOsc::kTwoPi * 440.0) / 48000.0;
    constexpr float kAmp = 0.25f;

    for (int i = 0; i < 512; ++i) {
        const float expected = kAmp * static_cast<float>(std::sin(phase));
        EXPECT_FLOAT_EQ(osc.next(kAmp), expected) << "sample " << i;
        phase += inc;
        if (phase >= howl::SineOsc::kTwoPi) {
            phase -= howl::SineOsc::kTwoPi;
        }
    }
}

TEST(SineOsc, ZeroSampleRateIsSilent) {
    howl::SineOsc osc;
    osc.configure(440.0, 0);
    EXPECT_FLOAT_EQ(osc.next(1.0f), 0.0f);
    EXPECT_DOUBLE_EQ(osc.phaseIncrement(), 0.0);
}
