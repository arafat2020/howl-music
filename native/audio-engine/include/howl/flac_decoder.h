#pragma once

#include "howl/decoder.h"

#include <cstdint>
#include <string>

namespace howl {

/**
 * FLAC reader via vendored dr_flac (Phase 2.4).
 *
 * Supports 1–2 channel native FLAC; always outputs interleaved Float32.
 * Worker/control thread only.
 */
class FlacDecoder final : public IDecoder {
public:
    FlacDecoder() = default;
    ~FlacDecoder() override;

    Status open(const char* path) override;
    void close() override;
    bool isOpen() const override;

    AudioFormat format() const override;
    int64_t frameCount() const override;
    int64_t positionFrames() const override;

    Status read(float* interleavedOut, int32_t maxFrames, int32_t* framesRead) override;
    Status seek(int64_t frameIndex) override;

private:
    void* flac_{nullptr};  // owns drflac*
    std::string path_;
    AudioFormat format_{};
    int64_t frameCount_{0};
    int64_t positionFrames_{0};
};

}  // namespace howl
