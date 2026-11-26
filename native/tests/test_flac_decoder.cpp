#include "howl/decoder.h"
#include "howl/engine.h"

#include <gtest/gtest.h>

#include <string>
#include <vector>

namespace {

std::string fixture(const char* name) {
    return std::string(HOWL_FIXTURES_DIR) + "/" + name;
}

}  // namespace

TEST(FlacDecoder, OpenSineStereo) {
    auto dec = howl::createFlacDecoder();
    ASSERT_EQ(dec->open(fixture("sine_440_16bit_stereo.flac").c_str()), howl::Status::Ok);
    EXPECT_TRUE(dec->isOpen());
    const howl::AudioFormat fmt = dec->format();
    EXPECT_EQ(fmt.sampleRate, 48000);
    EXPECT_EQ(fmt.channelCount, 2);
    EXPECT_EQ(dec->frameCount(), 12000);
    dec->close();
}

TEST(FlacDecoder, ReadAllMatchesFrameCount) {
    auto dec = howl::createFlacDecoder();
    ASSERT_EQ(dec->open(fixture("sine_440_16bit_stereo.flac").c_str()), howl::Status::Ok);
    const int64_t total = dec->frameCount();
    int64_t got = 0;
    std::vector<float> buf(512 * 2);
    while (got < total) {
        int32_t n = 0;
        ASSERT_EQ(dec->read(buf.data(), 512, &n), howl::Status::Ok);
        if (n == 0) {
            break;
        }
        got += n;
    }
    EXPECT_EQ(got, total);
    int32_t n = -1;
    EXPECT_EQ(dec->read(buf.data(), 512, &n), howl::Status::Ok);
    EXPECT_EQ(n, 0);
}

TEST(FlacDecoder, SeekRoundTrip) {
    auto dec = howl::createFlacDecoder();
    ASSERT_EQ(dec->open(fixture("sine_880_16bit_stereo.flac").c_str()), howl::Status::Ok);
    ASSERT_EQ(dec->seek(1000), howl::Status::Ok);
    EXPECT_EQ(dec->positionFrames(), 1000);
    std::vector<float> buf(64 * 2);
    int32_t n = 0;
    ASSERT_EQ(dec->read(buf.data(), 64, &n), howl::Status::Ok);
    EXPECT_EQ(n, 64);
    EXPECT_EQ(dec->positionFrames(), 1064);
}

TEST(FlacDecoder, RejectCorrupt) {
    auto dec = howl::createFlacDecoder();
    EXPECT_NE(dec->open(fixture("corrupt_not_flac.flac").c_str()), howl::Status::Ok);
    EXPECT_FALSE(dec->isOpen());
}

TEST(DecoderFactory, OpensWavAndFlac) {
    howl::Status st = howl::Status::Ok;
    auto wav = howl::openDecoder(fixture("sine_440_16bit_stereo.wav").c_str(), &st);
    ASSERT_NE(wav, nullptr);
    EXPECT_EQ(st, howl::Status::Ok);
    EXPECT_EQ(wav->format().channelCount, 2);

    auto flac = howl::openDecoder(fixture("sine_440_16bit_stereo.flac").c_str(), &st);
    ASSERT_NE(flac, nullptr);
    EXPECT_EQ(st, howl::Status::Ok);
    EXPECT_EQ(flac->frameCount(), 12000);
}

TEST(DecoderFactory, UnsupportedFailsCleanly) {
    howl::Status st = howl::Status::Ok;
    auto dec = howl::openDecoder(fixture("unsupported_pcm8.wav").c_str(), &st);
    EXPECT_EQ(dec, nullptr);
    EXPECT_EQ(st, howl::Status::UnsupportedFormat);
}

TEST(DecoderFactory, MissingFile) {
    howl::Status st = howl::Status::Ok;
    auto dec = howl::openDecoder(fixture("no_such_file.flac").c_str(), &st);
    EXPECT_EQ(dec, nullptr);
    EXPECT_EQ(st, howl::Status::FileOpenFailed);
}

TEST(EngineQueue, EnqueueFlac) {
    auto engine = howl::Engine::create();
    const std::string a = fixture("sine_440_16bit_stereo.flac");
    const std::string b = fixture("sine_880_16bit_stereo.flac");
    ASSERT_EQ(engine->enqueue(a.c_str()), howl::Status::Ok);
    ASSERT_EQ(engine->enqueue(b.c_str()), howl::Status::Ok);
    EXPECT_EQ(engine->queueSize(), 2);
    EXPECT_EQ(engine->next(), howl::Status::Ok);
    EXPECT_EQ(engine->currentIndex(), 1);
}
