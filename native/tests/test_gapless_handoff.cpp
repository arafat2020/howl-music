#include "howl/decode_worker.h"

#include <gtest/gtest.h>

#include <algorithm>
#include <vector>

namespace {

class Segment final : public howl::IDecoder {
public:
    Segment(int frames, float first, int sampleRate = 48000)
        : frames_(frames), first_(first), sampleRate_(sampleRate) {}

    howl::Status open(const char*) override { return howl::Status::Ok; }
    void close() override {}
    bool isOpen() const override { return true; }
    howl::AudioFormat format() const override { return {sampleRate_, 2}; }
    int64_t frameCount() const override { return frames_; }
    int64_t positionFrames() const override { return position_; }

    howl::Status read(float* output, int32_t count, int32_t* read) override {
        *read = std::min(count, frames_ - position_);
        for (int i = 0; i < *read; ++i) {
            output[i * 2] = first_ + position_ + i;
            output[i * 2 + 1] = -output[i * 2];
        }
        position_ += *read;
        return howl::Status::Ok;
    }

    howl::Status seek(int64_t frame) override {
        position_ = static_cast<int>(frame);
        return howl::Status::Ok;
    }

private:
    int frames_;
    float first_;
    int sampleRate_;
    int position_{0};
};

TEST(GaplessHandoff, ConsecutivePcmFramesHaveNoInsertedSamples) {
    howl::PcmRingBuffer ring(2048, 2);
    howl::DecodeWorker worker;
    ASSERT_EQ(worker.start(std::make_unique<Segment>(517, 1.0f), &ring), howl::Status::Ok);
    ASSERT_EQ(worker.setNext(std::make_unique<Segment>(413, 518.0f), 42, 0), howl::Status::Ok);
    ASSERT_EQ(worker.prefill(8), howl::Status::Ok);

    EXPECT_EQ(worker.playback(516).track.token, 0);
    EXPECT_EQ(worker.playback(517).track.token, 42);
    EXPECT_EQ(worker.playback(517).mode, howl::GaplessMode::Joined);
    EXPECT_EQ(worker.playback(517).track.startFrame, 517);
    EXPECT_EQ(worker.setNext(nullptr, 0, 0), howl::Status::InvalidState);

    std::vector<float> output(930 * 2);
    ASSERT_EQ(ring.pop(output.data(), 930), 930);
    for (int i = 0; i < 930; ++i) {
        EXPECT_EQ(output[i * 2], i + 1);
        EXPECT_EQ(output[i * 2 + 1], -(i + 1));
    }
    EXPECT_EQ(worker.setNext(nullptr, 0, 42), howl::Status::Ok);
}

TEST(GaplessHandoff, ReplacingAnUncommittedSuccessorDiscardsItsHead) {
    howl::PcmRingBuffer ring(1024, 2);
    howl::DecodeWorker worker;
    ASSERT_EQ(worker.start(std::make_unique<Segment>(17, 1.0f), &ring), howl::Status::Ok);
    ASSERT_EQ(worker.setNext(std::make_unique<Segment>(21, 100.0f), 10, 0), howl::Status::Ok);
    ASSERT_EQ(worker.setNext(std::make_unique<Segment>(21, 200.0f), 11, 0), howl::Status::Ok);
    ASSERT_EQ(worker.prefill(8), howl::Status::Ok);
    float output[38 * 2]{};
    ASSERT_EQ(ring.pop(output, 38), 38);
    EXPECT_EQ(output[17 * 2], 200.0f);
    EXPECT_EQ(worker.playback(17).track.token, 11);
}

TEST(GaplessHandoff, MismatchedRateLeavesOnlyCurrentPcm) {
    howl::PcmRingBuffer ring(1024, 2);
    howl::DecodeWorker worker;
    ASSERT_EQ(worker.start(std::make_unique<Segment>(257, 1.0f), &ring), howl::Status::Ok);
    ASSERT_EQ(worker.setNext(std::make_unique<Segment>(261, 258.0f, 44100), 42, 0), howl::Status::Ok);
    ASSERT_EQ(worker.prefill(8), howl::Status::Ok);
    EXPECT_EQ(worker.playback(257).mode, howl::GaplessMode::Fallback);
    EXPECT_EQ(worker.playback(257).track.token, 0);
    EXPECT_EQ(ring.availableRead(), 257);
}

TEST(GaplessHandoff, SeekBeforeCommittedBoundaryRestoresOriginalDecoder) {
    howl::PcmRingBuffer ring(1024, 2);
    howl::DecodeWorker worker;
    ASSERT_EQ(worker.start(std::make_unique<Segment>(257, 1.0f), &ring), howl::Status::Ok);
    ASSERT_EQ(worker.setNext(std::make_unique<Segment>(261, 258.0f), 42, 0), howl::Status::Ok);
    ASSERT_EQ(worker.prefill(8), howl::Status::Ok);
    ASSERT_EQ(worker.seekToFrame(100, 0), howl::Status::Ok);
    ring.clear();
    ASSERT_EQ(worker.prefill(8), howl::Status::Ok);
    std::vector<float> output(418 * 2);
    ASSERT_EQ(ring.pop(output.data(), 418), 418);
    for (int i = 0; i < 418; ++i) EXPECT_EQ(output[i * 2], i + 101);
    EXPECT_EQ(worker.playback(257).track.token, 42);
}

}  // namespace
