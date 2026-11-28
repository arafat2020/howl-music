#pragma once

#include "howl/decoder.h"

#include <cstdint>
#include <memory>
#include <vector>

namespace howl {

enum class GaplessMode : int32_t { Idle, Primed, Joined, Fallback };

struct GaplessTrack {
    int64_t token{0};
    int64_t startFrame{0};
    int64_t durationFrames{0};
};

struct GaplessPlayback {
    GaplessTrack track;
    GaplessMode mode{GaplessMode::Idle};
    int64_t writtenThrough{0};
};

/** Accessed under the decoder worker's mutex. Positions count real PCM frames, not zeros. */
class GaplessHandoff {
public:
    void reset(int64_t durationFrames);
    Status configure(std::unique_ptr<IDecoder> next, std::vector<float> head,
                     int64_t token, int64_t acknowledged, const AudioFormat& format);
    Status read(IDecoder& decoder, float* output, int32_t capacity, int32_t* frames);
    bool stitch(std::unique_ptr<IDecoder>& decoder, int64_t boundary, bool bufferedTail);
    Status seek(std::unique_ptr<IDecoder>& decoder, int64_t frame, int64_t consumed);
    GaplessPlayback playback(int64_t consumed) const;

private:
    GaplessTrack current_;
    GaplessTrack previous_;
    std::unique_ptr<IDecoder> next_;
    std::unique_ptr<IDecoder> previousDecoder_;
    std::vector<float> nextHead_;
    std::vector<float> currentHead_;
    std::size_t headOffset_{0};
    int64_t nextToken_{0};
    GaplessMode mode_{GaplessMode::Idle};
};

}  // namespace howl
