# Phase Specifications

Each file is a complete mentoring-phase spec. Do not start a phase until the previous one is accepted.

Master index with dependencies: [../05-phased-roadmap.md](../05-phased-roadmap.md)

## Quick list

### 0 — Foundation
- [0.1 Dev environment](phase-00.1-dev-environment.md)
- [0.2 Android skeleton](phase-00.2-android-skeleton.md)
- [0.3 Native JNI smoke](phase-00.3-native-jni-smoke.md)
- [0.4 Module wiring](phase-00.4-module-wiring.md)

### 1 — Audio foundation
- [1.1 Engine lifecycle](phase-01.1-engine-lifecycle.md)
- [1.2 Oboe test signal](phase-01.2-oboe-test-signal.md)
- [1.3 Diagnostics](phase-01.3-audio-diagnostics.md)
- [1.4 GoogleTest](phase-01.4-native-gtest.md)

### 2 — Playback pipeline
- [2.1 WAV decoder](phase-02.1-wav-decoder.md)
- [2.2 PCM ring buffer](phase-02.2-pcm-ring-buffer.md)
- [2.3 Transport](phase-02.3-transport-controls.md)
- [2.4 Format expansion](phase-02.4-format-expansion.md)
- [2.5 Queue handoff](phase-02.5-queue-handoff.md)

### 3 — DSP
- [3.1 JUCE graph](phase-03.1-juce-graph-skeleton.md)
- [3.2 Preamp](phase-03.2-preamp-gain.md)
- [3.3 Graphic EQ](phase-03.3-graphic-eq.md)
- [3.4 Param apply API](phase-03.4-param-smoothing.md)

### 4 — Android integration
- [4.1 JNI API](phase-04.1-jni-control-api.md)
- [4.2 Media3 adapter](phase-04.2-media3-adapter.md)
- [4.3 MediaSessionService](phase-04.3-mediasession-service.md)
- [4.4 Focus / interruptions](phase-04.4-focus-interruptions.md)

### 5 — Library & data
- [5.1 Room schema](phase-05.1-room-schema.md)
- [5.2 Media scan](phase-05.2-media-scan.md)
- [5.3 Playlists & prefs](phase-05.3-playlists-prefs.md)
- [5.4 Artwork](phase-05.4-artwork-cache.md)

### 6 — Visual product
- [6.1 Design system](phase-06.1-design-system.md)
- [6.2 Nav + mini-player](phase-06.2-nav-miniplayer.md)
- [6.3 Library UI](phase-06.3-library-ui.md)
- [6.4 Now Playing](phase-06.4-now-playing.md)
- [6.5 Equalizer UI](phase-06.5-equalizer-ui.md)
- [6.6 Queue UI](phase-06.6-queue-ui.md)

### 7 — Visualizer & polish
- [7.1 Analysis feed](phase-07.1-analysis-feed.md)
- [7.2 Visualizer v1](phase-07.2-visualizer-v1.md)
- [7.3 Motion + a11y](phase-07.3-motion-a11y.md)
- [7.4 Perf / battery](phase-07.4-perf-battery.md)

### 8 — Validation
- [8.1 Gapless](phase-08.1-gapless.md)
- [8.2 Output diagnostics](phase-08.2-output-diagnostics.md)
- [8.3 USB DAC](phase-08.3-usb-dac.md)
- [8.4 Release checklist](phase-08.4-release-checklist.md)
