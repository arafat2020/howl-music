# Decoder strategy (Phase 2.4)

**Status:** Chosen for Howl v1 path  
**Date:** 2026-11-26

## Decision

**Hybrid, phased:**

| Tier | Formats | Implementation | When |
|------|---------|----------------|------|
| A (now) | WAV (PCM16 / float32), **FLAC** | `WavDecoder` + `FlacDecoder` (`dr_flac`) behind `openDecoder(path)` | Phase 2.4 |
| B (later) | MP3 / AAC / ALAC / etc. | Prefer **Android MediaCodec + MediaExtractor** on a decode worker thread; optional thin FFmpeg only if coverage gaps remain | After Media3 / library scan |

Engine stays codec-agnostic via `IDecoder`. No decode on the audio callback.

## Why not full FFmpeg now

- APK size and build complexity dominate early milestones.
- LGPL/GPL diligence and patent codecs need a notices process we are not ready to own yet.
- Host unit tests and Android share one path more easily with header-only FLAC.

## Why not MediaCodec-only now

- Host GoogleTest cannot exercise MediaCodec.
- MediaCodec + extractor quirks belong beside content-URI / MediaStore work (Milestone 5).
- We still need a first-party lossless path for listening tests and gapless work (8.1).

## Why FLAC via `dr_flac`

- Single-file C library (public domain / MIT-0 style — see vendored header).
- Works on host and NDK with the same `IDecoder` contract.
- Small binary delta vs shipping libFLAC or FFmpeg.
- Stereo interleaved Float32 output matches the existing ring path.

## Resampling

Oboe still opens at device rate. Phase 2.4 **does not** ship a resampler: files whose sample rate does not match the open stream fail with `UnsupportedFormat` on `start` / track change (same as WAV today), or trigger Oboe reopen on handoff when rates differ and the new stream accepts the file rate.

**Follow-up:** worker-side resampler feeding the ring at stream rate (architecture review § resampling) before claiming broad library support.

## License / notices follow-ups

1. Keep `native/third_party/dr_libs/dr_flac.h` with its upstream license block intact.
2. Add a root `THIRD_PARTY_NOTICES` (or Play “open source notices”) entry for dr_flac before store release (Phase 8.4).
3. When MediaCodec path lands: document Android SDK terms only (no extra codec binary).
4. If FFmpeg is ever linked: pin LGPL build flags, disable GPL components, and ship corresponding notices + source offer process.

## Supported formats (Phase 2.4)

| Extension / container | Decoder | Channels | Notes |
|-----------------------|---------|----------|-------|
| `.wav` | `WavDecoder` | 1–2 (engine requires 2) | PCM16, float32 |
| `.flac` | `FlacDecoder` | 1–2 (engine requires 2) | Via `dr_flac` |
| other | — | — | `UnsupportedFormat` / open failure |

Probe order: magic bytes (`fLaC`, `RIFF….WAVE`), then extension fallback.
