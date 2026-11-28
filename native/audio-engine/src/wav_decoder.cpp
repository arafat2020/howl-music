#include "howl/wav_decoder.h"

#include <algorithm>
#include <cstring>
#include <vector>

namespace howl {
namespace {

constexpr uint16_t kWaveFormatPcm = 1;
constexpr uint16_t kWaveFormatIeeeFloat = 3;
constexpr uint16_t kWaveFormatExtensible = 0xFFFE;

uint32_t readU32LE(const uint8_t* p) {
    return static_cast<uint32_t>(p[0]) |
           (static_cast<uint32_t>(p[1]) << 8) |
           (static_cast<uint32_t>(p[2]) << 16) |
           (static_cast<uint32_t>(p[3]) << 24);
}

uint16_t readU16LE(const uint8_t* p) {
    return static_cast<uint16_t>(p[0] | (p[1] << 8));
}

bool tagEquals(const uint8_t* p, const char* tag4) {
    return std::memcmp(p, tag4, 4) == 0;
}

float int16ToFloat(int16_t s) {
    // Map full-scale int16 into [-1, 1].
    return static_cast<float>(s) / 32768.0f;
}

}  // namespace

std::unique_ptr<IDecoder> createWavDecoder() {
    return std::make_unique<WavDecoder>();
}

WavDecoder::~WavDecoder() {
    close();
}

void WavDecoder::close() {
    if (file_.is_open()) {
        file_.close();
    }
    path_.clear();
    format_ = {};
    dataOffset_ = 0;
    dataSizeBytes_ = 0;
    frameCount_ = 0;
    positionFrames_ = 0;
    bytesPerFrame_ = 0;
    open_ = false;
}

bool WavDecoder::isOpen() const {
    return open_;
}

AudioFormat WavDecoder::format() const {
    return format_;
}

int64_t WavDecoder::frameCount() const {
    return frameCount_;
}

int64_t WavDecoder::positionFrames() const {
    return positionFrames_;
}

Status WavDecoder::open(const char* path) {
    close();
    if (path == nullptr || path[0] == '\0') {
        return Status::NullHandle;
    }

    path_ = path;
    file_.open(path_, std::ios::binary);
    if (!file_) {
        close();
        return Status::FileOpenFailed;
    }

    const Status parsed = parseHeader();
    if (parsed != Status::Ok) {
        close();
        return parsed;
    }

    open_ = true;
    positionFrames_ = 0;
    return Status::Ok;
}

Status WavDecoder::parseHeader() {
    uint8_t riff[12];
    file_.read(reinterpret_cast<char*>(riff), 12);
    if (file_.gcount() != 12 || !tagEquals(riff, "RIFF") || !tagEquals(riff + 8, "WAVE")) {
        return Status::UnsupportedFormat;
    }

    bool haveFmt = false;
    bool haveData = false;
    uint16_t audioFormat = 0;
    uint16_t channels = 0;
    uint32_t sampleRate = 0;
    uint16_t bitsPerSample = 0;
    uint16_t blockAlign = 0;

    // Walk chunks until we have fmt + data (data may appear after others).
    while (file_ && !(haveFmt && haveData)) {
        uint8_t chunkHdr[8];
        file_.read(reinterpret_cast<char*>(chunkHdr), 8);
        if (file_.gcount() != 8) {
            break;
        }
        const uint32_t chunkSize = readU32LE(chunkHdr + 4);
        const std::streamoff next =
            static_cast<std::streamoff>(file_.tellg()) + static_cast<std::streamoff>(chunkSize) +
            static_cast<std::streamoff>(chunkSize & 1u);  // word-align pad

        if (tagEquals(chunkHdr, "fmt ")) {
            if (chunkSize < 16) {
                return Status::UnsupportedFormat;
            }
            std::vector<uint8_t> fmt(chunkSize);
            file_.read(reinterpret_cast<char*>(fmt.data()), static_cast<std::streamsize>(chunkSize));
            if (static_cast<uint32_t>(file_.gcount()) != chunkSize) {
                return Status::IoError;
            }

            audioFormat = readU16LE(fmt.data());
            channels = readU16LE(fmt.data() + 2);
            sampleRate = readU32LE(fmt.data() + 4);
            blockAlign = readU16LE(fmt.data() + 12);
            bitsPerSample = readU16LE(fmt.data() + 14);

            // WAVE_FORMAT_EXTENSIBLE: actual format lives at cbSize offset.
            if (audioFormat == kWaveFormatExtensible && chunkSize >= 40) {
                audioFormat = readU16LE(fmt.data() + 24);  // SubFormat first 2 bytes (PCM/float GUID)
            }

            haveFmt = true;
        } else if (tagEquals(chunkHdr, "data")) {
            dataOffset_ = static_cast<int64_t>(file_.tellg());
            dataSizeBytes_ = static_cast<int64_t>(chunkSize);
            haveData = true;
            // Stay positioned at start of PCM; do not skip yet.
            break;
        } else {
            file_.seekg(next);
        }
    }

    if (!haveFmt || !haveData) {
        return Status::UnsupportedFormat;
    }

    if (channels < 1 || channels > 2 || sampleRate == 0) {
        return Status::UnsupportedFormat;
    }

    SampleType sampleType;
    if (audioFormat == kWaveFormatPcm && bitsPerSample == 16) {
        sampleType = SampleType::Int16;
    } else if (audioFormat == kWaveFormatIeeeFloat && bitsPerSample == 32) {
        sampleType = SampleType::Float32;
    } else {
        return Status::UnsupportedFormat;
    }

    const int32_t expectedAlign =
        static_cast<int32_t>(channels) * (static_cast<int32_t>(bitsPerSample) / 8);
    if (blockAlign != 0 && blockAlign != expectedAlign) {
        return Status::UnsupportedFormat;
    }

    bytesPerFrame_ = expectedAlign;
    if (bytesPerFrame_ <= 0 || dataSizeBytes_ < bytesPerFrame_) {
        return Status::UnsupportedFormat;
    }

    frameCount_ = dataSizeBytes_ / bytesPerFrame_;
    format_.sampleRate = static_cast<int32_t>(sampleRate);
    format_.channelCount = static_cast<int32_t>(channels);
    format_.codec = SourceCodec::Wav;
    format_.bitsPerSample = static_cast<int32_t>(bitsPerSample);
    format_.sampleType = sampleType;

    file_.clear();
    file_.seekg(dataOffset_);
    if (!file_) {
        return Status::IoError;
    }
    return Status::Ok;
}

Status WavDecoder::seek(int64_t frameIndex) {
    if (!open_) {
        return Status::NotRunning;
    }
    if (frameIndex < 0 || frameIndex > frameCount_) {
        return Status::InvalidState;
    }
    const int64_t bytePos = dataOffset_ + frameIndex * bytesPerFrame_;
    file_.clear();
    file_.seekg(bytePos);
    if (!file_) {
        return Status::IoError;
    }
    positionFrames_ = frameIndex;
    return Status::Ok;
}

Status WavDecoder::read(float* interleavedOut, int32_t maxFrames, int32_t* framesRead) {
    if (framesRead != nullptr) {
        *framesRead = 0;
    }
    if (!open_) {
        return Status::NotRunning;
    }
    if (interleavedOut == nullptr || framesRead == nullptr) {
        return Status::NullHandle;
    }
    if (maxFrames <= 0) {
        return Status::Ok;
    }

    const int64_t remaining = frameCount_ - positionFrames_;
    if (remaining <= 0) {
        return Status::Ok;  // EOF
    }

    const int32_t toRead =
        static_cast<int32_t>(std::min<int64_t>(remaining, static_cast<int64_t>(maxFrames)));
    const size_t byteCount = static_cast<size_t>(toRead) * static_cast<size_t>(bytesPerFrame_);
    std::vector<uint8_t> raw(byteCount);
    file_.read(reinterpret_cast<char*>(raw.data()), static_cast<std::streamsize>(byteCount));
    const auto got = static_cast<size_t>(file_.gcount());
    if (got != byteCount) {
        // Truncated data chunk — report what we can, then error.
        const int32_t partialFrames = static_cast<int32_t>(got / static_cast<size_t>(bytesPerFrame_));
        if (partialFrames > 0) {
            // Fall through convert for partialFrames only.
            // (handled below by adjusting toRead conceptually — simpler: fail hard)
        }
        return Status::IoError;
    }

    const int32_t ch = format_.channelCount;
    if (format_.sampleType == SampleType::Int16) {
        for (int32_t f = 0; f < toRead; ++f) {
            for (int32_t c = 0; c < ch; ++c) {
                const size_t off = static_cast<size_t>(f * ch + c) * 2u;
                const int16_t s = static_cast<int16_t>(
                    raw[off] | (static_cast<uint16_t>(raw[off + 1]) << 8));
                interleavedOut[f * ch + c] = int16ToFloat(s);
            }
        }
    } else {
        // IEEE float32 little-endian
        for (int32_t f = 0; f < toRead; ++f) {
            for (int32_t c = 0; c < ch; ++c) {
                const size_t off = static_cast<size_t>(f * ch + c) * 4u;
                float sample = 0.0f;
                std::memcpy(&sample, raw.data() + off, sizeof(float));
                interleavedOut[f * ch + c] = sample;
            }
        }
    }

    positionFrames_ += toRead;
    *framesRead = toRead;
    return Status::Ok;
}

}  // namespace howl
