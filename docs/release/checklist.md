# Release checklist and recommendation

**No-go for public release.** Use the current build for internal testing. Complete the physical-device matrix, sustained screen-off and battery measurements, and production-signed candidate verification before returning for explicit approval to ship.

| Area | Evidence | Result |
|---|---|---|
| Native engine | JUCE-enabled host suite covers lifecycle, PCM ring, WAV/FLAC, DSP, analysis and PCM gapless handoff. | 70 tests passed |
| Android JVM | `:android-app:testDebugUnitTest` | Passed |
| Android release regression | Local non-debuggable release-profile build on the ARM64 API 36 emulator. | Passed after the packaging changes |
| Scan and library | Instrumented scanner, Room schema and persistence, real MediaStore source, prior Library UI checks. | Emulator passed |
| Playback and queue | Play/pause/seek, service reconnect after Activity teardown, stop/restart, rapid next, queue removal and automatic advance. | Emulator passed |
| Background and focus | MediaSession service playback after Activity teardown, transient, ducking and permanent focus-loss behavior. Short repeat profiling windows add background evidence. | Emulator passed. Physical noisy-route and lock-screen checks pending. |
| EQ | Complete settings apply, bypass/restore, saved preset persistence, live state in diagnostics. | Emulator passed |
| Gapless | Exact ring PCM boundary and same-rate WAV/FLAC live joins. | Host and emulator passed. Physical listening pending. |
| Notices | Runtime classpath, APK packaged native libraries and notice assets checked. | Packaged notice assets matched their sources |
| Claims | Settings and release notes scope the tested codecs, rate reporting, motion, and USB status. | Reviewed |

## Artifact and notice audit

The [third-party index](../../THIRD_PARTY_NOTICES.txt) describes the release runtime families. The release builds copy the index, permissive license texts, Barlow OFL, OkHttp Public Suffix List license, and matching NDK notices into `assets/third-party/`. Review the resolved release classpath and APK together when dependencies change. Re-run licensing review for an Android build with JUCE enabled. The audited build uses `HOWL_WITH_JUCE=OFF`. FFmpeg and test harness libraries are outside the shipped runtime.

The pre-audit APK had AndroidX native libraries for 32-bit ABIs alongside a playback engine built only for 64-bit ABIs. The app package now targets ARM64 and x86_64, so every packaged native ABI has Howl's engine, Oboe, and the C++ runtime. APK ZIP alignment passed the 16 KB check, and every native library had 16 KB-aligned ELF load segments.

The final emulator run reported eight cumulative app underruns in its 48 kHz service-playback snapshot, zero in its 44.1 kHz snapshot, and zero platform XRuns in both. The automated functional suite passed. Sustained playback on physical hardware remains a release gate.

## Remaining release gates

- Complete the [device matrix](device-matrix.md) on a primary and secondary physical device. Record manual transport, queue, EQ, Music permission, rotation, lock-screen, interruptions, and recovery checks. Run an x86_64 smoke test if distributing that ABI.
- Run the physical [performance and battery protocol](../perf/baseline.md). Resolve repeatable dropouts, sustained frame-budget misses, and physical background-playback failures before approving the release.
- Build the intended release artifact with production signing and the intended package/version. Install it cleanly, verify upgrade behavior, and recheck ABIs, notices, manifest permissions, and the final device regression. The standard unsigned release build passed. The locally signed profiling package is for testing.
- Review the [known limitations](known-limitations.md). Keep USB and unsupported-codec claims within the tested evidence. USB extras remain optional.
- Verify the empty-library Play action. The current release UI can invoke an internal sample tone when the queue is empty.
- Confirm the distribution channel and obtain explicit approval before publishing.
