# Phase 2.4 — Format Expansion (Decision + Implementation)

**Milestone:** 2 — Playback pipeline  
**Priority:** Foundational  
**Depends on:** 2.3  
**Status:** Approved

## Objective

Choose and implement the next decoder strategy for common local formats (beyond WAV), including licensing notes, binary size impact, and a small supported-format test corpus.

## Why it matters

A music player that only plays WAV is not shippable. This phase makes the **decoder decision** that the architecture review left open — explicitly, not silently.

## Architecture

Preserve `IDecoder`. Add one or more implementations behind a factory:

```text
openDecoder(path) → WavDecoder | FlacDecoder | (later MediaCodec / FFmpeg)
```

Resampling: deferred — mismatched rates still fail or reopen Oboe on handoff (documented). Worker-side resampler is a follow-up before broad library support.

## Learning objectives

- Codec licensing (FFmpeg LGPL/GPL, patent codecs).
- Binary size vs coverage trade-offs.
- Why a factory keeps the engine ignorant of container details.

## Scope

**Included**
- Written decision record (`docs/decisions/decoder-strategy.md`).
- Implementation covering FLAC via `dr_flac`.
- Test corpus folder + documented supported list.
- Error handling for unsupported files.

**Excluded**
- Every niche codec.
- Streaming/network playback.
- DRM/content providers.

**Must not implement yet**
- Claiming bit-perfect hi-res output.

## Implementation steps

1. Compare FFmpeg vs MediaCodec vs hybrid; pick one.
2. Record license obligations and notices process.
3. Implement decoder + factory.
4. Add corpus tests (decode duration/smoke).
5. Device play of at least two non-WAV files.

## Expected project structure

```text
native/audio-engine/src/flac_decoder.cpp
native/audio-engine/src/decoder_factory.cpp
native/third_party/dr_libs/dr_flac.h
docs/decisions/decoder-strategy.md
testdata/audio/corpus/...
```

## Testing strategy

- Corpus smoke decode on host where possible.
- Device playback checks.
- Memory: open/close many files without leaks (basic).

## Acceptance criteria

- [x] Decoder strategy decision documented and approved.
- [x] At least one compressed format plays via engine path. *(FLAC)*
- [x] Unsupported files fail cleanly.
- [x] License/notice follow-ups listed (even if notices file lands later).

## Review checklist

- [ ] Can explain why the chosen approach beats alternatives for *this* product.
- [ ] Understand where resampling happens. *(today: nowhere — match stream rate or reopen)*

## Potential pitfalls

- Pulling full FFmpeg “everything” → huge APK / GPL surprise.
- Decoding on the audio thread “just for MediaCodec.”
- Ignoring UTF-8 path / content URI differences (Android content URIs may need Phase 5 bridging).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | `openDecoder` factory; `FlacDecoder` (dr_flac); Engine load/enqueue/start/changeTrack use factory; two FLAC assets in smoke UI; corpus + decision doc. |
| Files created/modified | `flac_decoder.*`, `decoder_factory.cpp`, `decoder.h`, `engine.cpp`, CMake, tests, `dr_flac.h`, `decoder-strategy.md`, corpus, MainActivity |
| Tests executed | Host **45/45**. Android `:android-app:installDebug`. |
| Results | FLAC open/read/seek + factory rejection pass on host; app installed with 2× FLAC queue. |
| Known limitations | No MP3/AAC yet. No resampler. Engine still requires stereo. |
| Decisions made | Hybrid: WAV+FLAC now; MediaCodec later. Version `howl_engine/0.7.0-formats`. |
| Next phase | **3.1** JUCE graph skeleton (Milestone 2 complete after approval) |

**Approval required before next phase.**
