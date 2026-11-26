# Decoder test fixtures (Phase 2.1 / 2.4)

| File | Purpose |
|------|---------|
| `sine_440_16bit_stereo.wav` | 440 Hz, 16-bit PCM, stereo, 48 kHz, 12000 frames |
| `sine_440_float32_stereo.wav` | 440 Hz, IEEE float32, stereo, 48 kHz, 1000 frames |
| `sine_880_16bit_stereo.wav` | 880 Hz, 16-bit PCM, stereo, 48 kHz, 24000 frames |
| `sine_440_16bit_stereo.flac` | FLAC encode of the 440 Hz WAV fixture |
| `sine_880_16bit_stereo.flac` | FLAC encode of the 880 Hz WAV fixture |
| `corrupt_truncated.wav` | Truncated RIFF — must open as `UnsupportedFormat` |
| `corrupt_not_flac.flac` | Non-FLAC bytes — FLAC open must fail |
| `unsupported_pcm8.wav` | 8-bit PCM — explicitly unsupported |

Regenerate FLAC (optional):

```bash
flac -f -o sine_440_16bit_stereo.flac sine_440_16bit_stereo.wav
flac -f -o sine_880_16bit_stereo.flac sine_880_16bit_stereo.wav
```
