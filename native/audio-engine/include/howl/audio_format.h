#pragma once

#include <cstdint>

namespace howl {

/** PCM sample representation after decode (Phase 2.1). */
enum class SampleType : int32_t {
    Int16 = 0,
    Float32 = 1,
};

/**
 * Stream format descriptor — filled by IDecoder::open.
 * Decoders always deliver interleaved Float32 to the engine via read().
 * sampleType/bitsPerSample describe the *source* file encoding.
 */
enum class SourceCodec : int32_t { Unknown, Wav, Flac };

struct AudioFormat {
    int32_t sampleRate{0};
    int32_t channelCount{0};
    int32_t bitsPerSample{0};
    SampleType sampleType{SampleType::Int16};
    SourceCodec codec{SourceCodec::Unknown};
};

}  // namespace howl
