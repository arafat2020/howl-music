#define DR_FLAC_IMPLEMENTATION
#include "dr_flac.h"

#include "howl/flac_decoder.h"

#include <cstring>
#include <memory>

namespace howl {

std::unique_ptr<IDecoder> createFlacDecoder() {
    return std::make_unique<FlacDecoder>();
}

FlacDecoder::~FlacDecoder() {
    close();
}

void FlacDecoder::close() {
    if (flac_ != nullptr) {
        drflac_close(static_cast<drflac*>(flac_));
        flac_ = nullptr;
    }
    path_.clear();
    format_ = {};
    frameCount_ = 0;
    positionFrames_ = 0;
}

bool FlacDecoder::isOpen() const {
    return flac_ != nullptr;
}

Status FlacDecoder::open(const char* path) {
    if (path == nullptr || path[0] == '\0') {
        return Status::NullHandle;
    }
    close();

    drflac* handle = drflac_open_file(path, nullptr);
    if (handle == nullptr) {
        return Status::FileOpenFailed;
    }

    if (handle->channels < 1 || handle->channels > 2 || handle->sampleRate == 0) {
        drflac_close(handle);
        return Status::UnsupportedFormat;
    }

    format_.sampleRate = static_cast<int32_t>(handle->sampleRate);
    format_.channelCount = static_cast<int32_t>(handle->channels);
    format_.codec = SourceCodec::Flac;
    format_.bitsPerSample = static_cast<int32_t>(handle->bitsPerSample);
    format_.sampleType = SampleType::Int16;  // source is integer PCM in FLAC
    frameCount_ = static_cast<int64_t>(handle->totalPCMFrameCount);
    positionFrames_ = 0;
    path_ = path;
    flac_ = handle;
    return Status::Ok;
}

AudioFormat FlacDecoder::format() const {
    return format_;
}

int64_t FlacDecoder::frameCount() const {
    return frameCount_;
}

int64_t FlacDecoder::positionFrames() const {
    return positionFrames_;
}

Status FlacDecoder::read(float* interleavedOut, int32_t maxFrames, int32_t* framesRead) {
    if (framesRead != nullptr) {
        *framesRead = 0;
    }
    if (flac_ == nullptr || interleavedOut == nullptr || maxFrames < 0) {
        return Status::NullHandle;
    }
    if (maxFrames == 0) {
        return Status::Ok;
    }

    auto* handle = static_cast<drflac*>(flac_);
    const drflac_uint64 got =
        drflac_read_pcm_frames_f32(handle, static_cast<drflac_uint64>(maxFrames), interleavedOut);
    positionFrames_ += static_cast<int64_t>(got);
    if (framesRead != nullptr) {
        *framesRead = static_cast<int32_t>(got);
    }
    return Status::Ok;
}

Status FlacDecoder::seek(int64_t frameIndex) {
    if (flac_ == nullptr) {
        return Status::InvalidState;
    }
    if (frameIndex < 0 || (frameCount_ > 0 && frameIndex > frameCount_)) {
        return Status::InvalidState;
    }
    if (!drflac_seek_to_pcm_frame(static_cast<drflac*>(flac_),
                                  static_cast<drflac_uint64>(frameIndex))) {
        return Status::IoError;
    }
    positionFrames_ = frameIndex;
    return Status::Ok;
}

}  // namespace howl
