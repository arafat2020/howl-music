# Known limitations for release notes

- Howl indexes local Android media. Playback currently supports stereo WAV (16-bit integer and 32-bit float) and stereo FLAC. Indexed files in other formats can appear in Library.
- Compatible queued tracks at the same decoded sample rate can join in one output stream. Different-rate tracks, manual skips, and repeat-one use conventional transitions. A late queue edit that would replace already-buffered audio can be rejected until the join.
- Queue history lasts for the playback session. Indexed playback may need temporary cache space to stage a file, and large files can take longer to start.
- Output diagnostics distinguish a file's source rate, Howl's stream rate, and the rate reported by the hardware API. Hardware rate and route can be unavailable. Route matching is best effort and Android may mix or resample downstream.
- The visualizer is optional and offers a static mode with reduced motion. Physical battery cost, frame pacing, and output latency are awaiting testing.
- USB DAC compatibility, hot-plug recovery, exclusive output, and end-to-end bit-perfect playback are unverified.
- The runtime package targets ARM64 and x86_64 Android. The tested runtime target for this gate was ARM64.
