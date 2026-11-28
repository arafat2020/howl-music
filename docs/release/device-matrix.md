# Release device matrix

| Target | Android / build | Howl variant | Coverage | Result |
|---|---|---|---|---|
| Pixel ARM64 emulator (Google sdk_gphone64_arm64) | Android 16 / API 36, BE2A.250530.026.D1 | Local non-debuggable release profile, 0.1.0-dev | Engine and JNI, scanner and Room, transport and Media3 service, focus, EQ, queue, gapless, output diagnostics | Automated regression. Prior emulator UI checks and profiling are linked below. |
| Same emulator | Android 16 / API 36 | Debug | UI smoke from prior phases and repeat performance runs | Known emulator frame-time limitations documented. |
| Primary physical Android phone | Record exact OEM, model, build and route | Production-signed candidate | Full manual regression, lock screen, focus/route recovery, sustained playback and battery | Pending |
| Secondary physical Android phone | Different OEM, Android version or chipset | Production-signed candidate | Installation, library, transport, focus and output diagnostics smoke | Pending |
| x86_64 Android | Emulator or compatible device | Release candidate | Packaged ABI audit and runtime smoke | Native libraries included. Runtime smoke pending. |
| Physical Android + USB DAC | Record model, firmware and adapter | Production-signed candidate | USB routes, rates and hot-plug | Optional matrix pending hardware. |

The connected emulator uses a built-in speaker route. It is evidence for its tested app and engine paths. Physical playback, screen-off behavior and audio routing remain separate gates.

- [Performance and battery baseline](../perf/baseline.md)
- [Gapless implementation and caveats](../decisions/gapless-status.md)
- [Output diagnostics verification](../phases/phase-08.2-output-diagnostics.md)
- [USB DAC matrix](../perf/usb-dac-matrix.md)
