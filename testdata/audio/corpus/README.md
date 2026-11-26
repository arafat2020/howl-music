# Audio corpus (Phase 2.4)

Small supported-format smoke set used by host tests and device checks.

| File | Format | Notes |
|------|--------|-------|
| `sine_440_16bit_stereo.flac` | FLAC 48 kHz stereo | Short fixture (same PCM as host `native/tests/fixtures`) |
| `sine_880_16bit_stereo.flac` | FLAC 48 kHz stereo | Second track for queue/handoff |

**Supported by engine factory today:** WAV (PCM16 / float32), FLAC.  
**Not supported yet:** MP3, AAC, ALAC, Ogg Vorbis — fail cleanly via `openDecoder`.

Host fixtures (including WAV + corrupt cases) live under `native/tests/fixtures/`.
