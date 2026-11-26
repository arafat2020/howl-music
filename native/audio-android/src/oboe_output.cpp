#include "howl/audio_output.h"

#include <oboe/Oboe.h>

#include <atomic>
#include <cmath>
#include <cstdint>

namespace howl {
namespace {

constexpr double kTwoPi = 6.28318530717958647692;
constexpr double kSineHz = 440.0;
// ~ -6 dBFS: easier to hear on quiet emulators; still below full-scale.
constexpr float kAmplitude = 0.5f;

}  // namespace

struct AudioOutput::Impl : public oboe::AudioStreamDataCallback,
                           public oboe::AudioStreamErrorCallback {
    std::shared_ptr<oboe::AudioStream> stream;
    double phase = 0.0;
    double phaseIncrement = 0.0;
    int32_t channels = 2;

    std::atomic<int64_t> callbackCount{0};
    std::atomic<int32_t> lastFramesPerCallback{0};
    std::atomic<int64_t> underrunCount{0};
    std::atomic<int64_t> errorCount{0};

    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* audioStream,
        void* audioData,
        int32_t numFrames) override {
        // REALTIME: no alloc, no log, no JNI, no locks — atomics only.
        callbackCount.fetch_add(1, std::memory_order_relaxed);
        lastFramesPerCallback.store(numFrames, std::memory_order_relaxed);

        if (audioStream != nullptr) {
            auto xruns = audioStream->getXRunCount();
            if (xruns) {
                underrunCount.store(
                    static_cast<int64_t>(xruns.value()),
                    std::memory_order_relaxed);
            }
        }

        auto* out = static_cast<float*>(audioData);
        for (int32_t frame = 0; frame < numFrames; ++frame) {
            const float sample = kAmplitude * static_cast<float>(std::sin(phase));
            phase += phaseIncrement;
            if (phase >= kTwoPi) {
                phase -= kTwoPi;
            }
            for (int32_t ch = 0; ch < channels; ++ch) {
                out[frame * channels + ch] = sample;
            }
        }
        return oboe::DataCallbackResult::Continue;
    }

    void onErrorAfterClose(oboe::AudioStream* /*stream*/, oboe::Result /*error*/) override {
        // Not on the audio rendering path; still keep it allocation-light.
        errorCount.fetch_add(1, std::memory_order_relaxed);
    }
};

AudioOutput::AudioOutput() : impl_(std::make_unique<Impl>()) {}

AudioOutput::~AudioOutput() {
    (void)stop();
}

Status AudioOutput::start() {
    if (impl_->stream && impl_->stream->getState() == oboe::StreamState::Started) {
        return Status::AlreadyRunning;
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
        // Fall back: some devices reject LowLatency; try None.
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
    impl_->phase = 0.0;
    impl_->phaseIncrement = (kTwoPi * kSineHz) / static_cast<double>(sampleRate_);

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

    // Stop/close without taking Engine locks (callback never locks).
    (void)impl_->stream->requestStop();
    (void)impl_->stream->close();
    impl_->stream.reset();
    sampleRate_ = 0;
    channelCount_ = 0;
    framesPerBurst_ = 0;
    impl_->phase = 0.0;
    impl_->phaseIncrement = 0.0;
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

    // Prefer live Oboe xrun count when the stream is still open.
    if (impl_->stream) {
        auto xruns = impl_->stream->getXRunCount();
        if (xruns) {
            out.underrun_count = static_cast<int64_t>(xruns.value());
        }
    }
}

}  // namespace howl
