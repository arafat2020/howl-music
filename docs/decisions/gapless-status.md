# Gapless playback status

The Media3 playlist remains the queue authority. On start and on an eligible queue edit, it gives the native engine one staged successor. The engine opens its decoder and decodes a head block of up to 512 frames before handing it to the worker. At EOF the worker appends the head and subsequent PCM to the same ring, while the outgoing track still has PCM buffered. One output callback can consume frames from both tracks. The Oboe stream and EQ graph stay active, preserving the filter state across the join.

The worker records the exact boundary in rendered PCM frames. Media3 advances its current item when the output reaches that position and exposes the successor's relative timeline. The Kotlin polling rate increases near a queued boundary only. The audio handoff runs independently of those polls. The original native diagnostic queue keeps its legacy transport behavior.

## Eligibility and fallback

| Source pair | Behavior |
|---|---|
| Stereo WAV to WAV, or FLAC to FLAC, at equal decoded sample rates | PCM joins in the same output stream when the next source is ready in time. |
| Stereo WAV and FLAC at equal decoded sample rates | The same compatibility check applies because both produce interleaved Float32 PCM. |
| Different rates or channel layouts, or no decoded successor ready | The existing load/prepare path handles the next item. An eligible file starts with a gap when the stream must reopen. Unsupported files surface the existing playback error. |
| Manual next, previous, and play-now, or repeat-one | The existing transport change or loop path applies. |
| Other codecs | The engine currently decodes WAV and FLAC. Encoder delay and padding metadata for codecs such as MP3 would need separate support. |

A queue edit may replace an uncommitted successor. Once its PCM is in the ring, an edit that would change that immediate successor can be rejected until the output reaches the boundary. This keeps the displayed queue consistent with the buffered audio. The preceding decoder stays available until Media3 acknowledges the handoff, so a seek before the boundary can restore the outgoing track and re-prime the successor. Source silence at the end or beginning of a file remains part of that file.

## Diagnostics and verification

The app-only analysis diagnostics command returns the `gapless` field. It reports `Idle`, `Primed`, `Joined`, or `Fallback`. Native transport also reports the handoff token and track-relative frame position. For a seamless join, callback counts keep increasing and the ring underrun count stays flat across the boundary.

The host PCM test joins two asymmetric stereo segments at a non-buffer-aligned frame and compares every output sample with the expected concatenation. Other host cases cover replacement before commitment, late edit rejection, seek before the boundary, and mismatched rates. Android instrumentation uses WAV and FLAC fixtures to check a live output boundary. The Android queue test checks Media3 metadata advancement and repeated Next commands. Hardware listening or a loopback waveform capture remains pending.
