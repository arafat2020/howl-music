#include "howl/decoder.h"
#include "howl/status.h"
#include "howl/wav_decoder.h"

#include <gtest/gtest.h>

#include <cmath>
#include <string>
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

TEST(WavDecoder, OpenSine16Stereo) {
    auto dec = howl::createWavDecoder();
    ASSERT_EQ(dec->open(fixture("sine_440_16bit_stereo.wav").c_str()), howl::Status::Ok);
    EXPECT_TRUE(dec->isOpen());

    const auto fmt = dec->format();
    EXPECT_EQ(fmt.sampleRate, 48000);
    EXPECT_EQ(fmt.channelCount, 2);
    EXPECT_EQ(fmt.bitsPerSample, 16);
    EXPECT_EQ(fmt.sampleType, howl::SampleType::Int16);
    EXPECT_EQ(dec->frameCount(), 12000);
    EXPECT_EQ(dec->positionFrames(), 0);
}

TEST(WavDecoder, ReadAllMatchesFrameCount) {
    auto dec = howl::createWavDecoder();
    ASSERT_EQ(dec->open(fixture("sine_440_16bit_stereo.wav").c_str()), howl::Status::Ok);

    const int64_t total = dec->frameCount();
    std::vector<float> buf(static_cast<size_t>(total) * 2u);
    int64_t got = 0;
    while (got < total) {
        int32_t n = 0;
        const howl::Status st = dec->read(buf.data() + got * 2, 256, &n);
        ASSERT_EQ(st, howl::Status::Ok);
        if (n == 0) {
            break;
        }
        got += n;
    }
    EXPECT_EQ(got, total);
    EXPECT_EQ(dec->positionFrames(), total);

    // EOF read returns Ok with 0 frames.
    int32_t n = -1;
    EXPECT_EQ(dec->read(buf.data(), 16, &n), howl::Status::Ok);
    EXPECT_EQ(n, 0);

    // Spot-check: first sample near 0 (sin(0)), peak region non-silent.
    EXPECT_NEAR(buf[0], 0.0f, 1e-3f);
    float peak = 0.0f;
    for (float s : buf) {
        peak = std::max(peak, std::fabs(s));
    }
    EXPECT_GT(peak, 0.4f);
    EXPECT_LE(peak, 1.0f);
}

TEST(WavDecoder, SeekRoundTrip) {
    auto dec = howl::createWavDecoder();
    ASSERT_EQ(dec->open(fixture("sine_440_16bit_stereo.wav").c_str()), howl::Status::Ok);

    std::vector<float> a(64);
    int32_t n = 0;
    ASSERT_EQ(dec->read(a.data(), 32, &n), howl::Status::Ok);
    ASSERT_EQ(n, 32);

    ASSERT_EQ(dec->seek(0), howl::Status::Ok);
    std::vector<float> b(64);
    ASSERT_EQ(dec->read(b.data(), 32, &n), howl::Status::Ok);
    ASSERT_EQ(n, 32);
    for (int i = 0; i < 64; ++i) {
        EXPECT_FLOAT_EQ(a[i], b[i]) << "sample " << i;
    }
}

TEST(WavDecoder, Float32Stereo) {
    auto dec = howl::createWavDecoder();
    ASSERT_EQ(dec->open(fixture("sine_440_float32_stereo.wav").c_str()), howl::Status::Ok);
    EXPECT_EQ(dec->format().sampleType, howl::SampleType::Float32);
    EXPECT_EQ(dec->format().bitsPerSample, 32);
    EXPECT_EQ(dec->frameCount(), 1000);

    std::vector<float> buf(2000);
    int32_t n = 0;
    ASSERT_EQ(dec->read(buf.data(), 1000, &n), howl::Status::Ok);
    EXPECT_EQ(n, 1000);
    EXPECT_NEAR(buf[0], 0.0f, 1e-6f);
}

TEST(WavDecoder, RejectCorruptTruncated) {
    auto dec = howl::createWavDecoder();
    EXPECT_EQ(dec->open(fixture("corrupt_truncated.wav").c_str()), howl::Status::UnsupportedFormat);
    EXPECT_FALSE(dec->isOpen());
}

TEST(WavDecoder, RejectUnsupportedPcm8) {
    auto dec = howl::createWavDecoder();
    EXPECT_EQ(dec->open(fixture("unsupported_pcm8.wav").c_str()), howl::Status::UnsupportedFormat);
    EXPECT_FALSE(dec->isOpen());
}

TEST(WavDecoder, MissingFile) {
    auto dec = howl::createWavDecoder();
    EXPECT_EQ(dec->open(fixture("does_not_exist.wav").c_str()), howl::Status::FileOpenFailed);
}

TEST(WavDecoder, InterfaceUsableAsIDecoder) {
    std::unique_ptr<howl::IDecoder> dec = howl::createWavDecoder();
    ASSERT_EQ(dec->open(fixture("sine_440_16bit_stereo.wav").c_str()), howl::Status::Ok);
    dec->close();
    EXPECT_FALSE(dec->isOpen());
}
