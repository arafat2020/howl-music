#pragma once

#include "howl/audio_format.h"
#include "howl/status.h"

#include <cstdint>
#include <memory>

namespace howl {

/**
 * File/stream decoder interface (Phase 2.1).
 *
 * Threading: call only from a worker/control thread — never from the audio
 * callback. File I/O and codec work belong here; the callback only consumes
 * already-decoded PCM (Phase 2.2 ring).
 */
class IDecoder {
public:
    virtual ~IDecoder() = default;

    IDecoder(const IDecoder&) = delete;
    IDecoder& operator=(const IDecoder&) = delete;
    IDecoder(IDecoder&&) = delete;
    IDecoder& operator=(IDecoder&&) = delete;

    /** Open path; on failure leaves decoder closed. */
    virtual Status open(const char* path) = 0;

    /** Release file handle; safe if already closed. */
    virtual void close() = 0;

    virtual bool isOpen() const = 0;

    virtual AudioFormat format() const = 0;

    /** Total PCM frames if known; 0 if unknown/not open. */
    virtual int64_t frameCount() const = 0;

    /** Current read position in frames. */
    virtual int64_t positionFrames() const = 0;

    /**
     * Read up to maxFrames interleaved Float32 samples into out
     * (layout: L R L R …). framesRead is set to frames actually produced
     * (0 at EOF with Status::Ok).
     */
    virtual Status read(float* interleavedOut, int32_t maxFrames, int32_t* framesRead) = 0;

    /** Seek to absolute frame index (0 = start). */
    virtual Status seek(int64_t frameIndex) = 0;

protected:
    IDecoder() = default;
};

/** Factory for the Phase 2.1 WAV implementation. */
std::unique_ptr<IDecoder> createWavDecoder();

/** Factory for the Phase 2.4 FLAC implementation (dr_flac). */
std::unique_ptr<IDecoder> createFlacDecoder();

/**
 * Open a decoder by sniffing magic bytes / extension (Phase 2.4).
 * On failure returns nullptr and writes Status to statusOut when non-null.
 */
std::unique_ptr<IDecoder> openDecoder(const char* path, Status* statusOut = nullptr);

}  // namespace howl
