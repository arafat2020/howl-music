# Phase 2.4 — Format Expansion (Decision + Implementation)

**Milestone:** 2 — Playback pipeline  
**Priority:** Foundational  
**Depends on:** 2.3  
**Status:** Not started

## Objective

Choose and implement the next decoder strategy for common local formats (beyond WAV), including licensing notes, binary size impact, and a small supported-format test corpus.

## Why it matters

A music player that only plays WAV is not shippable. This phase makes the **decoder decision** that the architecture review left open — explicitly, not silently.

## Architecture

Preserve `IDecoder`. Add one or more implementations behind a factory:

```text
DecoderFactory.open(path) → WavDecoder | FfDecoder | MediaCodecDecoder | ...
```

Resampling: if rates differ, prefer a dedicated resampler on the worker side feeding the ring at the engine/stream rate.

## Learning objectives

- Codec licensing (FFmpeg LGPL/GPL, patent codecs).
- Binary size vs coverage trade-offs.
- Why a factory keeps the engine ignorant of container details.

## Scope

**Included**
- Written decision record (in completion report + architecture review checkbox).
- Implementation for the chosen path covering at least one compressed format used in testing (e.g. FLAC or AAC/MP3 — based on decision).
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
native/audio-engine/src/*_decoder.cpp
docs/decisions/decoder-strategy.md   # create when deciding
testdata/audio/corpus/...
```

## Testing strategy

- Corpus smoke decode on host where possible.
- Device playback checks.
- Memory: open/close many files without leaks (basic).

## Acceptance criteria

- [ ] Decoder strategy decision documented and approved.
- [ ] At least one compressed format plays via engine path.
- [ ] Unsupported files fail cleanly.
- [ ] License/notice follow-ups listed (even if notices file lands later).

## Review checklist

- [ ] Can explain why the chosen approach beats alternatives for *this* product.
- [ ] Understand where resampling happens.

## Potential pitfalls

- Pulling full FFmpeg “everything” → huge APK / GPL surprise.
- Decoding on the audio thread “just for MediaCodec.”
- Ignoring UTF-8 path / content URI differences (Android content URIs may need Phase 5 bridging).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 2.5 if not done; else 3.1 JUCE |

**Approval required before next phase.**
