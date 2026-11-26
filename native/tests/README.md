# native/tests

Host GoogleTest suite (Phase 1.4). Links `howl_audio_engine` only — **no Oboe / JNI / Android**.

## Run

```bash
# From repo root (uses SDK CMake when ANDROID_HOME is set)
./scripts/run-host-native-tests.sh
```

Or manually:

```bash
cmake -S native -B native/build-host -DHOWL_BUILD_TESTS=ON
cmake --build native/build-host --target howl_native_tests
ctest --test-dir native/build-host --output-on-failure
```

## What is covered

| Suite | Purpose |
|-------|---------|
| `EngineLifecycle.*` | State machine happy path + invalid transitions + C API |
| `SineOsc.*` | Deterministic sample math used by the 440 Hz test tone |

## What is **not** covered here

- Oboe stream open/start/callback I/O — device/emulator only (Phases 1.2–1.3).
- JNI / Kotlin façades — exercise via the app or later instrumented tests.

## GoogleTest source

Fetched via CMake `FetchContent`, **pinned** to tag `v1.15.2` (see `native/tests/CMakeLists.txt`). First configure needs network.
