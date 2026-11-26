#include "howl/decode_worker.h"
#include "howl/pcm_ring_buffer.h"
#include "howl/wav_decoder.h"

#include <gtest/gtest.h>

#include <chrono>
#include <cmath>
#include <string>
#include <thread>
#include <vector>

namespace {

std::string fixture(const char* name) {
#ifdef HOWL_FIXTURES_DIR
    return std::string(HOWL_FIXTURES_DIR) + "/" + name;
#else
    return std::string("fixtures/") + name;
#endif
}

}  // namespace

TEST(PcmRingBuffer, PushPopIntegrity) {
    howl::PcmRingBuffer ring(64, 2);
    std::vector<float> in(16);
    for (int i = 0; i < 16; ++i) {
        in[i] = static_cast<float>(i);
    }
    EXPECT_EQ(ring.push(in.data(), 8), 8);
    EXPECT_EQ(ring.availableRead(), 8);

    std::vector<float> out(16, -1.0f);
    EXPECT_EQ(ring.pop(out.data(), 8), 8);
    for (int i = 0; i < 16; ++i) {
        EXPECT_FLOAT_EQ(out[i], static_cast<float>(i));
    }
    EXPECT_EQ(ring.availableRead(), 0);
}

TEST(PcmRingBuffer, OverflowReturnsPartial) {
    howl::PcmRingBuffer ring(8, 1);  // capacity rounds to 8
    std::vector<float> in(16, 1.0f);
    EXPECT_EQ(ring.push(in.data(), 16), 8);
    EXPECT_EQ(ring.availableWrite(), 0);
    EXPECT_EQ(ring.push(in.data(), 1), 0);
}

TEST(PcmRingBuffer, UnderflowReturnsZero) {
    howl::PcmRingBuffer ring(16, 2);
    std::vector<float> out(8, 42.0f);
    EXPECT_EQ(ring.pop(out.data(), 4), 0);
    EXPECT_FLOAT_EQ(out[0], 42.0f);  // untouched
}

TEST(PcmRingBuffer, WrapAround) {
    howl::PcmRingBuffer ring(8, 1);
    std::vector<float> a{1, 2, 3, 4, 5, 6};
    ASSERT_EQ(ring.push(a.data(), 6), 6);
    std::vector<float> b(4);
    ASSERT_EQ(ring.pop(b.data(), 4), 4);
    EXPECT_FLOAT_EQ(b[0], 1);
    EXPECT_FLOAT_EQ(b[3], 4);
    std::vector<float> c{7, 8, 9, 10};
    EXPECT_EQ(ring.push(c.data(), 4), 4);  // wraps
    std::vector<float> d(6);
    EXPECT_EQ(ring.pop(d.data(), 6), 6);
    EXPECT_FLOAT_EQ(d[0], 5);
    EXPECT_FLOAT_EQ(d[1], 6);
    EXPECT_FLOAT_EQ(d[2], 7);
    EXPECT_FLOAT_EQ(d[5], 10);
}

TEST(DecodeWorker, FillsRingFromWav) {
    auto decoder = howl::createWavDecoder();
    ASSERT_EQ(decoder->open(fixture("sine_440_16bit_stereo.wav").c_str()), howl::Status::Ok);

    howl::PcmRingBuffer ring(4096, 2);
    howl::DecodeWorker worker;
    ASSERT_EQ(worker.start(std::move(decoder), &ring), howl::Status::Ok);

    // Wait until some audio is available.
    for (int i = 0; i < 200 && ring.availableRead() < 512; ++i) {
        std::this_thread::sleep_for(std::chrono::milliseconds(5));
    }
    EXPECT_GE(ring.availableRead(), 512);

    std::vector<float> out(1024);
    const int32_t got = ring.pop(out.data(), 512);
    EXPECT_EQ(got, 512);
    float peak = 0.0f;
    for (int i = 0; i < 1024; ++i) {
        peak = std::max(peak, std::fabs(out[i]));
    }
    EXPECT_GT(peak, 0.1f);

    worker.stop();
    EXPECT_FALSE(worker.isRunning());
}
