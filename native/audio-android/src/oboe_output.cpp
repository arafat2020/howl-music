#include "howl/audio_output.h"

#include <oboe/Oboe.h>

#include <cmath>
#include <cstdint>

namespace howl {
namespace {

constexpr double kTwoPi = 6.28318530717958647692;
constexpr double kSineHz = 440.0;
// ~ -6 dBFS: easier to hear on quiet emulators; still below full-scale.
constexpr float kAmplitude = 0.5f;

}  // namespace

struct AudioOutput::Impl : public oboe::AudioStreamDataCallback {
    std::shared_ptr<oboe::AudioStream> stream;
    double phase = 0.0;
    double phaseIncrement = 0.0;
    int32_t channels = 2;

    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* /*audioStream*/,
        void* audioData,
        int32_t numFrames) override {
        // REALTIME: no alloc, no log, no JNI, no locks.
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

    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Shared)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(oboe::ChannelCount::Stereo)
        ->setUsage(oboe::Usage::Media)
        ->setContentType(oboe::ContentType::Music)
        ->setDataCallback(impl_.get());

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
    impl_->channels = channelCount_;
    impl_->phase = 0.0;
    impl_->phaseIncrement = (kTwoPi * kSineHz) / static_cast<double>(sampleRate_);

    result = impl_->stream->requestStart();
    if (result != oboe::Result::OK) {
        impl_->stream->close();
        impl_->stream.reset();
        sampleRate_ = 0;
        channelCount_ = 0;
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
    impl_->phase = 0.0;
    impl_->phaseIncrement = 0.0;
    return Status::Ok;
}

bool AudioOutput::isRunning() const {
    return impl_->stream && impl_->stream->getState() == oboe::StreamState::Started;
}

}  // namespace howl
