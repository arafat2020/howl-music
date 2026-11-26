# Phase 2.1 — Decoder Abstraction + WAV/PCM

**Milestone:** 2 — Playback pipeline  
**Priority:** Foundational  
**Depends on:** 1.2  
**Status:** Implemented — awaiting your approval

## Objective

Introduce a decoder interface and a correct WAV/PCM implementation that produces interleaved PCM frames on a **worker/control path**, not in the audio callback.

## Why it matters

WAV is the simplest container to prove format handling, errors, and ownership before FFmpeg/MediaCodec complexity.

## Architecture

```text
IDecoder
  open(path/uri) → format (rate, channels, bits)
  read(pcm frames) → frames read / EOF / error
  seek(frame)
  close()

WavDecoder : IDecoder
```

Engine will later pull from decoder via ring buffer (Phase 2.2).

## Learning objectives

- PCM format descriptors.
- Why decoding off the audio thread is mandatory for file codecs.
- Interface segregation for future decoders.

## Scope

**Included**
- `IDecoder` + `AudioFormat` structs.
- WAV reader for common PCM variants (at least 16-bit mono/stereo).
- Host tests with tiny fixture WAVs.
- Explicit error for unsupported WAV formats.

**Excluded**
- MP3/AAC/FLAC (Phase 2.4).
- Ring buffer integration (2.2).
- Seeking UI.

**Must not implement yet**
- Reading files inside the Oboe callback.

## Implementation steps

1. Define decoder interface in `audio-engine`.
2. Implement WavDecoder.
3. Add fixture files under `native/tests/fixtures/`.
4. GoogleTest: open, read all, check sample count; reject bad headers.

## Expected project structure

```text
native/audio-engine/
  include/howl/decoder.h
  include/howl/audio_format.h
  include/howl/wav_decoder.h
  src/wav_decoder.cpp
native/tests/fixtures/sine_440_16bit_stereo.wav
native/tests/test_wav_decoder.cpp
```

## Testing strategy

- Unit tests with known sine WAV.
- Truncated/corrupt file → clean error.

## Acceptance criteria

- [x] WAV PCM decode works in tests.
- [x] Interface is usable for a second decoder later.
- [x] No audio-callback file I/O.
- [x] Unsupported formats fail explicitly.

## Review checklist

- [ ] Understand header parsing vs sample data.
- [ ] Know ownership of file handles (RAII).

## Potential pitfalls

- Assuming all WAVs are 16-bit PCM.
- Endianness mistakes.
- Blocking UI thread with large reads (keep on worker once integrated).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | `IDecoder` + `AudioFormat`; `WavDecoder` (16-bit PCM + float32, mono/stereo) outputting interleaved Float32; `createWavDecoder()` factory; status codes `FileOpenFailed` / `UnsupportedFormat` / `IoError`. |
| Files created/modified | `decoder.h`, `audio_format.h`, `wav_decoder.h/.cpp`, `status.h`, fixtures, `test_wav_decoder.cpp`, CMake/docs. |
| Tests executed | `./scripts/run-host-native-tests.sh` → **21/21 PASS** (8 new WavDecoder cases). `:android-app:assembleDebug` SUCCESS. |
| Results | Open/read/seek/EOF + reject corrupt/PCM8/missing file all green. |
| Known limitations | Not wired into Engine/Oboe yet (Phase 2.2). No MP3/AAC/FLAC. Max 2 channels. |
| Decisions made | Always convert to **Float32 interleaved** on `read()`. RAII `ifstream` in `WavDecoder`. Decoder stays off the audio callback by API contract. |
| Next phase | 2.2 — PCM ring buffer |

**Approval required before Phase 2.2.**
