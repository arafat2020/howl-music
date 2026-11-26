#pragma once

#include "howl/decoder.h"

#include <cstdint>
#include <fstream>
#include <string>

namespace howl {

/**
 * RIFF/WAVE PCM reader (Phase 2.1).
 *
 * Supported source encodings:
 * - PCM integer 16-bit, 1–2 channels
 * - IEEE float 32-bit, 1–2 channels
 *
 * Always outputs interleaved Float32 via IDecoder::read.
 */
class WavDecoder final : public IDecoder {
public:
    WavDecoder() = default;
    ~WavDecoder() override;

    Status open(const char* path) override;
    void close() override;
    bool isOpen() const override;

    AudioFormat format() const override;
    int64_t frameCount() const override;
    int64_t positionFrames() const override;

    Status read(float* interleavedOut, int32_t maxFrames, int32_t* framesRead) override;
    Status seek(int64_t frameIndex) override;

private:
    Status parseHeader();

    std::ifstream file_;
    std::string path_;
    AudioFormat format_{};
    int64_t dataOffset_{0};
    int64_t dataSizeBytes_{0};
    int64_t frameCount_{0};
    int64_t positionFrames_{0};
    int32_t bytesPerFrame_{0};
    bool open_{false};
};

}  // namespace howl
