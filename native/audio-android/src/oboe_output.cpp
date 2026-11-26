#include "howl/audio_output.h"

#include "howl/pcm_ring_buffer.h"
#include "howl/sine_osc.h"

#include <oboe/Oboe.h>

#include <atomic>
#include <cstdint>
#include <cstring>
#include <memory>

namespace {

constexpr double kSineHz = 440.0;
constexpr float kAmplitude = 0.5f;

}  // namespace

namespace howl {

struct AudioOutput::Impl : public oboe::AudioStreamDataCallback,
                           public oboe::AudioStreamErrorCallback {
    std::shared_ptr<oboe::AudioStream> stream;
    SineOsc osc;
    int32_t channels = 2;
    OutputMode mode = OutputMode::Sine;
    PcmRingBuffer* ring = nullptr;  // non-owning

    std::atomic<int64_t> callbackCount{0};
    std::atomic<int32_t> lastFramesPerCallback{0};
    std::atomic<int64_t> underrunCount{0};  // ring underrun events (or Oboe xruns in sine)
    std::atomic<int64_t> errorCount{0};
    std::atomic<bool> useRingUnderruns{false};

    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* audioStream,
        void* audioData,
        int32_t numFrames) override {
        // REALTIME: no alloc, no log, no JNI, no locks — atomics + ring pop only.
        callbackCount.fetch_add(1, std::memory_order_relaxed);
        lastFramesPerCallback.store(numFrames, std::memory_order_relaxed);

        auto* out = static_cast<float*>(audioData);

        if (mode == OutputMode::Ring && ring != nullptr) {
            const int32_t got = ring->pop(out, numFrames);
            if (got < numFrames) {
                const int32_t samples = (numFrames - got) * channels;
                std::memset(
                    out + got * channels,
                    0,
                    static_cast<size_t>(samples) * sizeof(float));
                underrunCount.fetch_add(1, std::memory_order_relaxed);
            }
            return oboe::DataCallbackResult::Continue;
        }

        // Sine mode — also track Oboe xruns for diagnostics.
        if (audioStream != nullptr) {
            auto xruns = audioStream->getXRunCount();
            if (xruns) {
                underrunCount.store(
                    static_cast<int64_t>(xruns.value()),
                    std::memory_order_relaxed);
            }
        }
        for (int32_t frame = 0; frame < numFrames; ++frame) {
            const float sample = osc.next(kAmplitude);
            for (int32_t ch = 0; ch < channels; ++ch) {
                out[frame * channels + ch] = sample;
            }
        }
        return oboe::DataCallbackResult::Continue;
    }

    void onErrorAfterClose(oboe::AudioStream* /*stream*/, oboe::Result /*error*/) override {
        errorCount.fetch_add(1, std::memory_order_relaxed);
    }
};

AudioOutput::AudioOutput() : impl_(std::make_unique<Impl>()) {}

AudioOutput::~AudioOutput() {
    (void)stop();
}

void AudioOutput::setOutputMode(OutputMode mode, PcmRingBuffer* ring) {
    impl_->mode = mode;
    impl_->ring = ring;
    impl_->useRingUnderruns.store(mode == OutputMode::Ring, std::memory_order_relaxed);
}

Status AudioOutput::start() {
    if (impl_->stream && impl_->stream->getState() == oboe::StreamState::Started) {
        return Status::AlreadyRunning;
    }

    if (impl_->mode == OutputMode::Ring && impl_->ring == nullptr) {
        return Status::InvalidState;
    }

    (void)stop();

    impl_->callbackCount.store(0, std::memory_order_relaxed);
    impl_->lastFramesPerCallback.store(0, std::memory_order_relaxed);
    impl_->underrunCount.store(0, std::memory_order_relaxed);
    impl_->errorCount.store(0, std::memory_order_relaxed);

    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Shared)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(oboe::ChannelCount::Stereo)
        ->setUsage(oboe::Usage::Media)
        ->setContentType(oboe::ContentType::Music)
        ->setDataCallback(impl_.get())
        ->setErrorCallback(impl_.get());

    oboe::Result result = builder.openStream(impl_->stream);
    if (result != oboe::Result::OK) {
        builder.setPerformanceMode(oboe::PerformanceMode::None);
        result = builder.openStream(impl_->stream);
        if (result != oboe::Result::OK) {
            impl_->stream.reset();
            return Status::DeviceOpenFailed;
        }
    }

    sampleRate_ = impl_->stream->getSampleRate();
    channelCount_ = impl_->stream->getChannelCount();
    framesPerBurst_ = impl_->stream->getFramesPerBurst();
    impl_->channels = channelCount_;

    if (impl_->mode == OutputMode::Ring) {
        if (impl_->ring->channelCount() != channelCount_) {
            impl_->stream->close();
            impl_->stream.reset();
            sampleRate_ = 0;
            channelCount_ = 0;
            framesPerBurst_ = 0;
            return Status::UnsupportedFormat;
        }
    } else {
        impl_->osc.configure(kSineHz, sampleRate_);
    }

    result = impl_->stream->requestStart();
    if (result != oboe::Result::OK) {
        impl_->stream->close();
        impl_->stream.reset();
        sampleRate_ = 0;
        channelCount_ = 0;
        framesPerBurst_ = 0;
        return Status::DeviceOpenFailed;
    }

    return Status::Ok;
}

Status AudioOutput::stop() {
    if (!impl_->stream) {
        return Status::Ok;
    }

    (void)impl_->stream->requestStop();
    (void)impl_->stream->close();
    impl_->stream.reset();
    sampleRate_ = 0;
    channelCount_ = 0;
    framesPerBurst_ = 0;
    impl_->osc = SineOsc{};
    return Status::Ok;
}

bool AudioOutput::isRunning() const {
    return impl_->stream && impl_->stream->getState() == oboe::StreamState::Started;
}

void AudioOutput::copyDiagnostics(EngineDiagnostics& out) const {
    out.sample_rate = sampleRate_;
    out.channel_count = channelCount_;
    out.frames_per_burst = framesPerBurst_;
    out.frames_per_callback =
        impl_->lastFramesPerCallback.load(std::memory_order_relaxed);
    out.callback_count = impl_->callbackCount.load(std::memory_order_relaxed);
    out.underrun_count = impl_->underrunCount.load(std::memory_order_relaxed);
    out.error_count = impl_->errorCount.load(std::memory_order_relaxed);

    // Sine mode: prefer live Oboe xrun count. Ring mode: keep our underrun events.
    if (!impl_->useRingUnderruns.load(std::memory_order_relaxed) && impl_->stream) {
        auto xruns = impl_->stream->getXRunCount();
        if (xruns) {
            out.underrun_count = static_cast<int64_t>(xruns.value());
        }
    }
}

}  // namespace howl
