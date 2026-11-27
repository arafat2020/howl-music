#include "howl/dsp/process_graph.h"

#include "gain_node.h"

#include <cmath>

namespace howl {

struct ProcessGraph::Impl {
    bool prepared = false;
    bool bypassed = false;
    double sampleRate = 0.0;
    int32_t maxBlockSize = 0;
    int32_t channelCount = 0;

    GainNode gain;
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

    impl_->gain.prepare(sampleRate);

    impl_->prepared = true;
}

void ProcessGraph::reset() {
    if (impl_ == nullptr) {
        return;
    }
    impl_->gain.reset();
}

bool ProcessGraph::setTargetGainDb(float gainDb) noexcept {
    return impl_->gain.setTargetGainDb(gainDb);
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

    impl_->gain.process(interleaved, frames, impl_->channelCount);
}

const char* ProcessGraph::backendName() {
#if HOWL_WITH_JUCE
    return "juce_dsp";
#else
    return "native";
#endif
}

}  // namespace howl
