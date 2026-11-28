# Howl playback performance baseline

Phase 7.4 adds a repeatable Android measurement harness. Emulator measurements are a development baseline. Device sign-off requires the physical runs below.

## Device matrix

| Target | Role | Status |
|---|---|---|
| Pixel API 36 ARM64 emulator | Harness smoke test and debug versus local release baseline | Measurements below |
| Mid-range physical Android phone | Primary frame, audio, memory, and battery benchmark | Pending hardware |
| Second physical Android phone on a different chipset or Android version | Cross-device output and battery check | Pending hardware |

## Build and run

Use JDK 17, the project Android SDK, and ADB. Build and install the app and its matching instrumentation APK on the selected device. The opt-in release build has a separate package and uses a local debug certificate. It retains the release variant’s non-debuggable behavior and current minification setting.

```sh
./gradlew :android-app:assembleDebug :android-app:assembleDebugAndroidTest
adb -s "$serial" install -r android-app/build/outputs/apk/debug/android-app-debug.apk
adb -s "$serial" install -r android-app/build/outputs/apk/androidTest/debug/android-app-debug-androidTest.apk

./gradlew -PhowlProfileRelease=true :android-app:assembleRelease :android-app:assembleReleaseAndroidTest
adb -s "$serial" install -r android-app/build/outputs/apk/release/android-app-release.apk
adb -s "$serial" install -r android-app/build/outputs/apk/androidTest/release/android-app-release-androidTest.apk
```

Grant the target app music access. Index the same stereo WAV or FLAC with embedded artwork on each target. Choose a continuous track with varying levels that lasts longer than the full sequence and its setup windows. A 35-minute file covers the defaults with margin. Find its MediaStore ID from the library or via ADB.

```sh
adb -s "$serial" shell content query --uri content://media/external/audio/media --projection _id:_display_name
uri=content://media/external/audio/media/123

bash scripts/profile-android.sh "$serial" "$uri" /tmp/howl-debug-1
HOWL_PROFILE_PACKAGE=com.howl.music.profile \
  bash scripts/profile-android.sh "$serial" "$uri" /tmp/howl-release-1
```

Run each build twice in separate output directories. The runner captures build and device identifiers, initial and final battery dumps, and instrumentation output. Leave the chosen output route, volume, display refresh rate, brightness, and reduced-motion setting fixed. Keep peak playback volume and EQ boosts comfortable. Warm the device and artwork cache consistently and record the warm/cold condition.

| Scenario | Default window | Action |
|---|---:|---|
| Now Playing | 15 s | Expand the mini-player and leave the embedded cover visible. |
| EQ | 15 s | Open EQ. Make six discrete preamp or band changes spread across the window and end at Flat. |
| Visualizer | 15 s | Open Visualizer in Pulse mode with motion enabled. The Now Playing row is the visualizer-off comparison. |
| Background | 30 min | Press Home. After the Activity stops, lock the display for a physical run. Playback should continue. |

Allow the 12-second setup period for navigation. Act on the printed `phase=setup` and `phase=measure` prompts. The runner verifies playback identity and foreground or background state at both ends of the measurement. Confirm each foreground destination from the display before sampling. Adjust the order or windows with `HOWL_PROFILE_SCENARIOS`, `HOWL_PROFILE_SECONDS`, `HOWL_PROFILE_BACKGROUND_SECONDS`, and `HOWL_PROFILE_SETUP_SECONDS`. Put background last for a combined run. For a battery-only run, select just background.

## Metrics and proposed investigation thresholds

| Metric | How to interpret it | Investigate when a physical run shows |
|---|---|---|
| Process CPU | Process CPU time divided by elapsed wall time. 100% is one logical CPU. Includes the harness. | Background exceeds 10% of one core, or Visualizer adds over 10 percentage points versus Now Playing in repeated runs. |
| Memory | Process PSS at both ends, in KiB. It includes proportional shared allocations and reflects process residency at those points. | Background PSS grows over 20 MiB over 30 minutes and the growth repeats. |
| Audio | Native callback and ring-underrun counter deltas across the same window. | Any repeatable underrun on continuous steady-state playback. |
| Frames | Android Window frame-metrics total, draw, and swap p95, with frame count and dropped metric reports. Values cover drawn frames. | At fixed 60 Hz, p95 total repeatedly exceeds the 16.7 ms frame period in an active foreground scenario with an adequate sample. |
| Battery | System battery dumps and per-package Batterystats estimate. Measure power separately from the 15-second frame samples. | Repeatable excess versus a matched screen-off control or unexpected awake time. Set a numerical threshold after the first physical matrix. |

These are investigation thresholds. Treat small frame samples and large variation as diagnostic leads. The harness records `startBootNs` and `endBootNs` to align sample windows with a system trace.

## Physical traces and power

Record a short trace during each active scenario in a separate pass. Compare scheduling, frame timeline, and audio-thread runnable time alongside underruns. An audio thread’s CPU slice is a scheduling proxy. To measure callback duration, use a profiling build with callback start/end markers or an equivalent audio trace. For output latency, use a timed hardware loopback or an external microphone capture with a known pulse and report the route and method.

```sh
adb -s "$serial" shell perfetto -o /data/misc/perfetto-traces/howl.pftrace \
  -t 30s sched freq idle am wm gfx view audio
adb -s "$serial" pull /data/misc/perfetto-traces/howl.pftrace /tmp/howl.pftrace
```

Keep trace binaries outside Git and record the build, route, window, and artifact identifier for physical results. [Perfetto Android trace recording](https://perfetto.dev/docs/learning-more/android)

For power, run uninstrumented playback and a matched idle screen-off control for 30 minutes each, twice. Use wireless ADB on battery power, equal starting charge and temperature bands, and the same network and display settings. Capture `dumpsys battery` and `dumpsys batterystats --charged PACKAGE` before and after without resetting system-wide stats. Report charge-counter delta and per-UID estimate separately when the device exposes them. Use an external power meter when the charge-counter resolution is insufficient. [Android dumpsys and Batterystats](https://developer.android.com/tools/dumpsys)

A separate 25-second emulator Perfetto smoke capture covered a Now Playing measurement and produced a non-empty trace artifact outside the repository. The table below uses runs without trace capture.

## Emulator results

Two runs per build on the Pixel API 36 ARM64 emulator used the same 150-second, 48 kHz stereo FLAC with a varying level and 1,024 × 1,024 embedded cover. Foreground windows lasted 8 seconds each and background windows lasted 20 seconds with the launcher visible. The order was Now Playing, EQ, Visualizer, then background. Each cell reports runs A and B. The confirmed second release run used a 9-second setup window. The other runs used 5 seconds. CPU is a percentage of one logical CPU. PSS is the end-of-window value.

| Build | Scenario | CPU % | PSS MiB | Total frame p95 ms | Frames | Audio callbacks | Underruns |
|---|---|---:|---:|---:|---:|---:|---:|
| Debug | Now Playing | 6.96 / 6.43 | 127.6 / 128.1 | 33.7 / 25.1 | 32 / 31 | 796 / 796 | 0 / 0 |
| Debug | EQ adjustment | 3.76 / 4.39 | 131.0 / 131.2 | 33.7 / 35.0 | 6 / 8 | 796 / 797 | 0 / 0 |
| Debug | Visualizer | 4.72 / 4.96 | 133.6 / 133.9 | 18.1 / 18.1 | 122 / 121 | 796 / 797 | 0 / 0 |
| Debug | Background | 2.05 / 2.13 | 129.5 / 129.8 | — / — | 0 / 0 | 1991 / 1991 | 0 / 0 |
| Local release | Now Playing | 4.64 / 4.67 | 74.9 / 61.5 | 24.2 / 26.0 | 32 / 31 | 796 / 797 | 0 / 0 |
| Local release | EQ adjustment | 2.45 / 1.71 | 64.8 / 64.9 | 54.5 / 33.7 | 6 / 6 | 796 / 797 | 0 / 0 |
| Local release | Visualizer | 3.60 / 2.54 | 67.6 / 67.9 | 18.3 / 18.5 | 122 / 125 | 794 / 796 | 0 / 0 |
| Local release | Background | 0.66 / 0.59 | 63.2 / 63.1 | — / — | 0 / 0 | 1991 / 1993 | 0 / 0 |

All 16 windows reported zero native underruns and zero dropped frame-metric reports. Background PSS grew by 0.12–0.17 MiB in debug and 0.19–0.22 MiB in local release. These short samples cannot establish long-run memory or battery behavior. Emulator charge readings are excluded from the power comparison.

**Finding deferred to hardware**. An exploratory release run had about 1,010 ms swap-stage p95 in EQ and Visualizer. The setup screenshots still showed Library or overlapping destinations, so that run was excluded from the comparable rows. A repeat with longer setup verified the intended screens and did not reproduce the stall. Now Playing still exceeded the provisional 60 Hz threshold in both builds. Capture physical Perfetto traces before attributing the cost or tuning the app. EQ generated only 6–8 frames per run, so its p95 is sensitive to a single slow frame.

**Fixture adjustment**. The initial steady tone yielded only two visualizer frames in eight seconds. The varying-level fixture addressed that measurement gap. The comparable runs then produced 121–125 frames in the same window.
