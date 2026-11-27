#include "howl/dsp/process_graph.h"
#include "howl/dsp/graphic_eq.h"

#include "gain_node.h"

#include <algorithm>
#include <atomic>
#include <cmath>
#include <mutex>

namespace howl {
namespace {

constexpr unsigned kPending = 1u << 2;
constexpr unsigned kSlotMask = kPending - 1;
static_assert(std::atomic<unsigned>::is_always_lock_free);

}  // namespace

struct ProcessGraph::Impl {
    bool prepared = false;
    bool bypassed = false;
    double sampleRate = 0.0;
    int32_t maxBlockSize = 0;
    int32_t channelCount = 0;

    GainNode gain;
    GraphicEq eq;

    std::mutex targetMutex;  // Serializes control writers only.
    EqState target{};
    std::array<EqState, 3> slots{};
    unsigned writeSlot = 0;
    unsigned readSlot = 1;
    std::atomic<unsigned> exchangeSlot{2};

    void publish() noexcept {
        // Each side owns its slot until handing it off through exchangeSlot.
        slots[writeSlot] = target;
        writeSlot = exchangeSlot.exchange(writeSlot | kPending, std::memory_order_acq_rel)
                    & kSlotMask;
    }

    void consume() noexcept {
        if ((exchangeSlot.load(std::memory_order_acquire) & kPending) == 0) {
            return;
        }
        readSlot = exchangeSlot.exchange(readSlot, std::memory_order_acq_rel) & kSlotMask;
        const auto& state = slots[readSlot];
        gain.setTargetGainDb(state.preampDb);
        eq.setBandGainsDb(state.bandsDb);
    }
};

ProcessGraph::ProcessGraph() : impl_(new Impl) {}

ProcessGraph::~ProcessGraph() {
    delete impl_;
    impl_ = nullptr;
}

void ProcessGraph::prepare(double sampleRate, int32_t maxBlockSize, int32_t channelCount) {
    if (impl_ == nullptr) {
        return;
    }
    if (!std::isfinite(sampleRate) || sampleRate <= 0.0 || maxBlockSize <= 0 || channelCount <= 0) {
        impl_->prepared = false;
        return;
    }

    impl_->sampleRate = sampleRate;
    impl_->maxBlockSize = maxBlockSize;
    impl_->channelCount = channelCount;

    impl_->consume();
    impl_->gain.prepare(sampleRate);
    impl_->eq.prepare(sampleRate, channelCount);

    impl_->prepared = true;
}

void ProcessGraph::reset() {
    if (impl_ == nullptr) {
        return;
    }
    impl_->consume();
    impl_->gain.reset();
    impl_->eq.reset();
}

bool ProcessGraph::setTargetGainDb(float gainDb) noexcept {
    if (!std::isfinite(gainDb)) {
        return false;
    }
    std::lock_guard lock(impl_->targetMutex);
    impl_->target.preampDb = std::clamp(gainDb, -24.0f, 12.0f);
    impl_->publish();
    return true;
}

bool ProcessGraph::setEqBandGainDb(int32_t band, float gainDb) noexcept {
    if (band < 0 || static_cast<std::size_t>(band) >= kEqBandCount || !std::isfinite(gainDb)) {
        return false;
    }
    std::lock_guard lock(impl_->targetMutex);
    impl_->target.bandsDb[band] = std::clamp(gainDb, -12.0f, 12.0f);
    impl_->publish();
    return true;
}

bool ProcessGraph::applyEqState(const EqState& state) noexcept {
    if (!std::isfinite(state.preampDb) || !std::all_of(
            state.bandsDb.begin(), state.bandsDb.end(), [](float gain) { return std::isfinite(gain); })) {
        return false;
    }
    std::lock_guard lock(impl_->targetMutex);
    impl_->target.preampDb = std::clamp(state.preampDb, -24.0f, 12.0f);
    std::transform(state.bandsDb.begin(), state.bandsDb.end(), impl_->target.bandsDb.begin(),
                   [](float gain) { return std::clamp(gain, -12.0f, 12.0f); });
    impl_->publish();
    return true;
}

void ProcessGraph::resetEqToFlat() noexcept {
    std::lock_guard lock(impl_->targetMutex);
    impl_->target = {};
    impl_->publish();
}

void ProcessGraph::setBypassed(bool bypassed) {
    if (impl_ != nullptr) {
        impl_->bypassed = bypassed;
    }
}

bool ProcessGraph::isBypassed() const {
    return impl_ != nullptr && impl_->bypassed;
}

bool ProcessGraph::isPrepared() const {
    return impl_ != nullptr && impl_->prepared;
}

double ProcessGraph::sampleRate() const {
    return impl_ != nullptr ? impl_->sampleRate : 0.0;
}

int32_t ProcessGraph::maxBlockSize() const {
    return impl_ != nullptr ? impl_->maxBlockSize : 0;
}

int32_t ProcessGraph::channelCount() const {
    return impl_ != nullptr ? impl_->channelCount : 0;
}

void ProcessGraph::process(float* interleaved, int32_t frames) {
    // REALTIME: no alloc / lock / I/O.
    if (impl_ == nullptr || interleaved == nullptr || frames <= 0) {
        return;
    }
    if (!impl_->prepared || impl_->bypassed) {
        return;
    }

    impl_->consume();
    impl_->gain.process(interleaved, frames, impl_->channelCount);
    impl_->eq.process(interleaved, frames);
}

const char* ProcessGraph::backendName() {
#if HOWL_WITH_JUCE
    return "juce_dsp";
#else
    return "native";
#endif
}

}  // namespace howl
