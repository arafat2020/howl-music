#include "howl/engine.h"

#include <gtest/gtest.h>

#include <algorithm>
#include <array>
#include <atomic>
#include <cmath>
#include <cstdlib>
#include <limits>
#include <new>
#include <numbers>
#include <thread>
#include <type_traits>

namespace {

thread_local std::size_t* allocationCount = nullptr;

void* trackedAllocate(std::size_t size) {
    if (allocationCount != nullptr) {
        ++*allocationCount;
    }
    if (void* allocation = std::malloc(size == 0 ? 1 : size)) {
        return allocation;
    }
    throw std::bad_alloc{};
}

class AllocationTracker {
public:
    explicit AllocationTracker(std::size_t& count) { allocationCount = &count; }
    ~AllocationTracker() { allocationCount = nullptr; }
};

constexpr int32_t kFrames = 256;
using Block = std::array<float, kFrames * 2>;

Block tone(int32_t block) {
    Block samples{};
    for (int32_t frame = 0; frame < kFrames; ++frame) {
        const auto angle = (block * kFrames + frame) * (2.0 * std::numbers::pi / 48.0);
        samples[2 * frame] = samples[2 * frame + 1] = static_cast<float>(0.02 * std::sin(angle));
    }
    return samples;
}

howl::EqState vShape() {
    return {-6.0f, {9.0f, 7.0f, 4.0f, 0.0f, -3.0f, -6.0f, -3.0f, 0.0f, 4.0f, 7.0f}};
}

}  // namespace

void* operator new(std::size_t size) { return trackedAllocate(size); }
void* operator new[](std::size_t size) { return trackedAllocate(size); }
void operator delete(void* p) noexcept { std::free(p); }
void operator delete[](void* p) noexcept { std::free(p); }
void operator delete(void* p, std::size_t) noexcept { std::free(p); }
void operator delete[](void* p, std::size_t) noexcept { std::free(p); }

static_assert(std::is_trivial_v<howl::EqState> && std::is_standard_layout_v<howl::EqState>);

TEST(EqState, BatchMatchesIndividualTargetsAndRejectsPartialInvalidUpdate) {
    howl::ProcessGraph batch;
    howl::ProcessGraph individual;
    batch.prepare(48000.0, kFrames, 2);
    individual.prepare(48000.0, kFrames, 2);
    const auto state = vShape();
    ASSERT_TRUE(batch.applyEqState(state));
    ASSERT_TRUE(individual.setTargetGainDb(state.preampDb));
    for (int32_t i = 0; i < static_cast<int32_t>(state.bandsDb.size()); ++i) {
        ASSERT_TRUE(individual.setEqBandGainDb(i, state.bandsDb[i]));
    }

    auto invalid = state;
    invalid.preampDb = 12.0f;
    invalid.bandsDb.back() = std::numeric_limits<float>::quiet_NaN();
    EXPECT_FALSE(batch.applyEqState(invalid));

    for (int32_t i = 0; i < 16; ++i) {
        auto a = tone(i);
        auto b = a;
        batch.process(a.data(), kFrames);
        individual.process(b.data(), kFrames);
        EXPECT_EQ(a, b);
        if (i == 15) EXPECT_NE(a, tone(i));
    }
}

TEST(EqState, RampsPresetInAndBackToExactFlat) {
    howl::ProcessGraph graph;
    graph.prepare(48000.0, kFrames, 2);
    ASSERT_TRUE(graph.applyEqState(vShape()));

    float previous = tone(0).front();
    for (int32_t i = 0; i < 30; ++i) {
        if (i == 15) graph.resetEqToFlat();
        auto block = tone(i);
        const auto dry = block;
        graph.process(block.data(), kFrames);
        for (int32_t frame = 0; frame < kFrames; ++frame) {
            EXPECT_LT(std::abs(block[2 * frame] - previous), 0.006f);
            previous = block[2 * frame];
        }
        if (i >= 19) EXPECT_EQ(block, dry);
    }

    // An active ramp advances by frames, independent of how a callback is split.
    howl::ProcessGraph split;
    split.prepare(48000.0, kFrames, 2);
    ASSERT_TRUE(split.applyEqState(vShape()));
    ASSERT_TRUE(graph.applyEqState(vShape()));
    for (int32_t i = 0; i < 5; ++i) {
        auto a = tone(i);
        auto b = a;
        graph.process(a.data(), kFrames);
        split.process(b.data(), 120);
        split.process(b.data() + 240, kFrames - 120);
        EXPECT_NEAR(a.back(), b.back(), 1.0e-7f);
    }
}

TEST(EqState, ConcurrentUpdatesStayFiniteAndProcessDoesNotAllocate) {
    howl::ProcessGraph graph;
    graph.prepare(48000.0, kFrames, 2);
    ASSERT_TRUE(graph.applyEqState(vShape()));
    std::atomic<bool> processing{false};
    std::atomic<bool> valid{true};
    std::thread control([&] {
        while (!processing.load(std::memory_order_acquire)) {
            std::this_thread::yield();
        }
        for (int32_t i = 0; i < 12000; ++i) {
            auto state = vShape();
            state.preampDb = i % 2 == 0 ? 12.0f : -24.0f;
            for (auto& gain : state.bandsDb) gain = i % 2 == 0 ? -12.0f : 12.0f;
            if (!graph.applyEqState(state)) {
                valid.store(false, std::memory_order_relaxed);
            }
        }
    });

    std::size_t allocations = 0;
    processing.store(true, std::memory_order_release);
    for (int32_t i = 0; i < 1500; ++i) {
        auto block = tone(i);
        {
            AllocationTracker track(allocations);
            graph.process(block.data(), kFrames);
        }
        EXPECT_TRUE(std::all_of(block.begin(), block.end(), [](float x) {
            return std::isfinite(x) && std::abs(x) < 100.0f;
        }));
    }
    control.join();
    EXPECT_TRUE(valid.load(std::memory_order_relaxed));
    EXPECT_EQ(allocations, 0u);

    graph.resetEqToFlat();
    for (int32_t i = 0; i < 5; ++i) {
        auto block = tone(i);
        graph.process(block.data(), kFrames);
        if (i == 4) EXPECT_EQ(block, tone(i));
    }
}

TEST(EqState, CApiValidatesCountAndResets) {
    auto* handle = howl_engine_create();
    ASSERT_NE(handle, nullptr);
    const auto state = vShape();
    const auto count = static_cast<int32_t>(state.bandsDb.size());
    const auto ok = static_cast<int32_t>(howl::Status::Ok);
    const auto invalid = static_cast<int32_t>(howl::Status::InvalidArgument);
    const auto null = static_cast<int32_t>(howl::Status::NullHandle);
    EXPECT_EQ(howl_engine_apply_eq_state(handle, state.preampDb, state.bandsDb.data(), count), ok);
    EXPECT_EQ(howl_engine_apply_eq_state(handle, state.preampDb, state.bandsDb.data(), count - 1),
              invalid);
    EXPECT_EQ(howl_engine_apply_eq_state(handle, state.preampDb, nullptr, count), invalid);
    EXPECT_EQ(howl_engine_reset_eq_to_flat(handle), ok);
    EXPECT_EQ(howl_engine_reset_eq_to_flat(nullptr), null);
    howl_engine_destroy(handle);
}
