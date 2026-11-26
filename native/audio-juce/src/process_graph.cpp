#include "howl/dsp/process_graph.h"

#include <algorithm>
#include <cmath>
#include <cstring>

#if HOWL_WITH_JUCE
#include <juce_dsp/juce_dsp.h>
#endif

namespace howl {

struct ProcessGraph::Impl {
    bool prepared = false;
    bool bypassed = false;
    double sampleRate = 0.0;
    int32_t maxBlockSize = 0;
    int32_t channelCount = 0;

#if HOWL_WITH_JUCE
    juce::dsp::Gain<float> gain;
#endif
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
    if (!(sampleRate > 0.0) || maxBlockSize <= 0 || channelCount <= 0) {
        impl_->prepared = false;
        return;
    }

    impl_->sampleRate = sampleRate;
    impl_->maxBlockSize = maxBlockSize;
    impl_->channelCount = channelCount;

#if HOWL_WITH_JUCE
    juce::dsp::ProcessSpec spec;
    spec.sampleRate = sampleRate;
    spec.maximumBlockSize = static_cast<juce::uint32>(maxBlockSize);
    spec.numChannels = static_cast<juce::uint32>(channelCount);
    impl_->gain.prepare(spec);
    impl_->gain.setGainLinear(1.0f);  // identity node for Phase 3.1
    impl_->gain.reset();
#endif

    impl_->prepared = true;
}

void ProcessGraph::reset() {
    if (impl_ == nullptr) {
        return;
    }
#if HOWL_WITH_JUCE
    impl_->gain.reset();
#endif
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

    const int32_t ch = impl_->channelCount;
    if (ch <= 0) {
        return;
    }

#if HOWL_WITH_JUCE
    // Unity Gain applied sample-wise — proves juce_dsp is linked; bit-stable at 1.0.
    const float g = impl_->gain.getGainLinear();
    if (std::fabs(g - 1.0f) < 1.0e-7f) {
        return;  // exact identity fast path
    }
    const int32_t n = frames * ch;
    for (int32_t i = 0; i < n; ++i) {
        interleaved[i] *= g;
    }
#else
    (void)ch;
    (void)interleaved;
#endif
}

const char* ProcessGraph::backendName() {
#if HOWL_WITH_JUCE
    return "juce_dsp";
#else
    return "identity";
#endif
}

}  // namespace howl
