# Decoder test fixtures (Phase 2.1)

| File | Purpose |
|------|---------|
| `sine_440_16bit_stereo.wav` | 440 Hz, 16-bit PCM, stereo, 48 kHz, 12000 frames |
| `sine_440_float32_stereo.wav` | 440 Hz, IEEE float32, stereo, 48 kHz, 1000 frames |
| `corrupt_truncated.wav` | Truncated RIFF — must open as `UnsupportedFormat` |
| `unsupported_pcm8.wav` | 8-bit PCM — explicitly unsupported |

Regenerate (optional):

```bash
python3 - <<'PY'
# see phase completion notes / prior generation script in git history
PY
```
