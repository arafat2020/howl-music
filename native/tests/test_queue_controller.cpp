#include "howl/engine.h"
#include "howl/queue.h"

#include <gtest/gtest.h>

#include <string>

using howl::Engine;
using howl::QueueController;
using howl::RepeatMode;
using howl::Status;

namespace {

std::string fixture(const char* name) {
    return std::string(HOWL_FIXTURES_DIR) + "/" + name;
}

}  // namespace

TEST(QueueController, EnqueueAndAdvance) {
    QueueController q;
    EXPECT_TRUE(q.empty());
    EXPECT_EQ(q.enqueue("a.wav"), Status::Ok);
    EXPECT_EQ(q.enqueue("b.wav"), Status::Ok);
    EXPECT_EQ(q.size(), 2u);
    EXPECT_EQ(q.currentIndex(), 0);
    ASSERT_NE(q.current(), nullptr);
    EXPECT_EQ(q.current()->path, "a.wav");

    EXPECT_TRUE(q.advanceNext());
    EXPECT_EQ(q.currentIndex(), 1);
    EXPECT_EQ(q.current()->path, "b.wav");

    EXPECT_FALSE(q.advanceNext());  // Repeat::None at end
    EXPECT_EQ(q.currentIndex(), 1);
}

TEST(QueueController, RepeatAllWraps) {
    QueueController q;
    q.setRepeatMode(RepeatMode::All);
    ASSERT_EQ(q.enqueue("a.wav"), Status::Ok);
    ASSERT_EQ(q.enqueue("b.wav"), Status::Ok);
    EXPECT_TRUE(q.advanceNext());
    EXPECT_EQ(q.currentIndex(), 1);
    EXPECT_TRUE(q.advanceNext());
    EXPECT_EQ(q.currentIndex(), 0);
}

TEST(QueueController, RepeatOneStaysOnEof) {
    QueueController q;
    q.setRepeatMode(RepeatMode::One);
    ASSERT_EQ(q.enqueue("a.wav"), Status::Ok);
    ASSERT_EQ(q.enqueue("b.wav"), Status::Ok);
    EXPECT_TRUE(q.advanceNext(/*honorRepeatOne=*/true));
    EXPECT_EQ(q.currentIndex(), 0);

    EXPECT_TRUE(q.advanceNext(/*honorRepeatOne=*/false));
    EXPECT_EQ(q.currentIndex(), 1);
}

TEST(QueueController, PreviousWrapsWithRepeatAll) {
    QueueController q;
    q.setRepeatMode(RepeatMode::All);
    ASSERT_EQ(q.enqueue("a.wav"), Status::Ok);
    ASSERT_EQ(q.enqueue("b.wav"), Status::Ok);
    EXPECT_TRUE(q.advancePrevious());
    EXPECT_EQ(q.currentIndex(), 1);
}

TEST(QueueController, ClearResetsIndex) {
    QueueController q;
    ASSERT_EQ(q.enqueue("a.wav"), Status::Ok);
    q.clear();
    EXPECT_TRUE(q.empty());
    EXPECT_EQ(q.currentIndex(), -1);
}

TEST(EngineQueue, LoadSeedsQueue) {
    auto engine = Engine::create();
    const std::string a = fixture("sine_440_16bit_stereo.wav");
    ASSERT_EQ(engine->load(a.c_str()), Status::Ok);
    EXPECT_EQ(engine->queueSize(), 1);
    EXPECT_EQ(engine->currentIndex(), 0);
}

TEST(EngineQueue, EnqueueTwoAndNavigate) {
    auto engine = Engine::create();
    const std::string a = fixture("sine_440_16bit_stereo.wav");
    const std::string b = fixture("sine_880_16bit_stereo.wav");

    ASSERT_EQ(engine->enqueue(a.c_str()), Status::Ok);
    ASSERT_EQ(engine->enqueue(b.c_str()), Status::Ok);
    EXPECT_EQ(engine->queueSize(), 2);
    EXPECT_EQ(engine->currentIndex(), 0);

    EXPECT_EQ(engine->next(), Status::Ok);
    EXPECT_EQ(engine->currentIndex(), 1);

    EXPECT_EQ(engine->previous(), Status::Ok);
    EXPECT_EQ(engine->currentIndex(), 0);

    EXPECT_EQ(engine->playAt(1), Status::Ok);
    EXPECT_EQ(engine->currentIndex(), 1);

    EXPECT_EQ(engine->setRepeatMode(RepeatMode::All), Status::Ok);
    EXPECT_EQ(engine->repeatMode(), RepeatMode::All);
    EXPECT_EQ(engine->next(), Status::Ok);  // wraps 1 → 0
    EXPECT_EQ(engine->currentIndex(), 0);
}

TEST(EngineQueue, ClearQueue) {
    auto engine = Engine::create();
    const std::string a = fixture("sine_440_16bit_stereo.wav");
    ASSERT_EQ(engine->enqueue(a.c_str()), Status::Ok);
    EXPECT_EQ(engine->clearQueue(), Status::Ok);
    EXPECT_EQ(engine->queueSize(), 0);
    EXPECT_EQ(engine->currentIndex(), -1);
}

TEST(EngineQueue, CApiEnqueueNext) {
    HowlEngineHandle* eng = howl_engine_create();
    ASSERT_NE(eng, nullptr);
    const std::string a = fixture("sine_440_16bit_stereo.wav");
    const std::string b = fixture("sine_880_16bit_stereo.wav");
    EXPECT_EQ(howl_engine_enqueue(eng, a.c_str()), static_cast<int32_t>(Status::Ok));
    EXPECT_EQ(howl_engine_enqueue(eng, b.c_str()), static_cast<int32_t>(Status::Ok));
    EXPECT_EQ(howl_engine_queue_size(eng), 2);
    EXPECT_EQ(howl_engine_next(eng), static_cast<int32_t>(Status::Ok));
    EXPECT_EQ(howl_engine_current_index(eng), 1);
    howl_engine_destroy(eng);
}
