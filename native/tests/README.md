# native/tests

## Phase 1.1 — lifecycle (no GoogleTest yet)

Host build (macOS):

```bash
cmake -S native -B native/build-host
cmake --build native/build-host --target howl_engine_lifecycle_test
./native/build-host/audio-engine/howl_engine_lifecycle_test
```

GoogleTest harness arrives in Phase 1.4.
