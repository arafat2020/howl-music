# HowlEngine Kotlin API

`com.howl.music.bridge.HowlEngine` owns one native playback engine per instance. Create it with `HowlEngine.create()` and release it with `close()` or Kotlin `use`. Closing stops playback, waits for native workers, and releases the handle. Repeated close calls on the control thread are safe.

## Threading

Use one control thread per instance for commands, snapshot reads, and close. The methods are synchronous. File probes, seeks, track changes, stream start/stop, and close can block, so production clients should run the engine on a dedicated serial thread. Send snapshots to the UI for display. The temporary debug screen uses the main thread for its small local fixtures. JNI calls carry commands and snapshots. PCM stays in native code.

## Commands and snapshots

| Area | Kotlin API |
|---|---|
| Lifecycle | `create()`, optional `prepare()`, `start()`, `stop()`, `close()`, `isClosed` |
| File playback | Select `PlayMode.File`, then call `load(path)` to replace the queue with one WAV or FLAC file. Paths are local filesystem paths. |
| Transport | `pause()`, `resume()`, `seekMs(positionMs)`, `transport()`. The snapshot carries position, duration, sample rate, and pause/play flags. A paused stream has both `playing` and `paused` set. After EOF, wait for the engine to reach `Idle` before restarting. |
| Queue | `enqueue(path)`, `clearQueue()`, `playAt(index)`, `next()`, `previous()`, `setRepeatMode(mode)`, `currentIndex()`, `queueSize()`, `repeatMode()`. Indices start at 0. |
| DSP | `applyEqState(state)` publishes a preamp value and ten band gains together. `resetEqToFlat()` returns all gains to 0 dB. Individual controls use `setPreampGainDb` and `setEqBandGainDb`. |
| Diagnostics | `diagnostics()`, `stateName()`, `HowlEngine.version()`, `HowlEngine.libraryError()` |

Commands return `0` on success. Use `statusName(code)` for failures such as `InvalidState`, `NotRunning`, `FileOpenFailed`, or `InvalidArgument`. Snapshot methods return their empty value on a native status failure. Linkage, unexpected native exceptions, and instance calls requiring a closed handle surface as JVM errors or exceptions. The library load diagnostic is available through `libraryError()`.

```kotlin
HowlEngine.create().use { engine ->
    fun requireOk(code: Int) = check(code == 0) { engine.statusName(code) }
    requireOk(engine.setPlayMode(HowlEngine.PlayMode.File))
    requireOk(engine.load(wavPath))
    requireOk(engine.start())
    // Keep the engine open for the playback session.
}
```

The C control declarations live in `native/audio-engine/include/howl/engine_api.h`. The JNI bridge owns conversions from UTF-8 paths, numeric EQ settings, and snapshots. Kotlin clients should use the façade methods. `NativeSmoke` and `lifecycleSmokeReport()` are deprecated.

## Android instrumentation

Build the APKs with `./gradlew :android-app:assembleDebug :android-app:assembleDebugAndroidTest`. Install the debug and debug test APKs and start the instrumentation with this command.

```sh
adb shell am instrument -w com.howl.music.test/com.howl.music.EngineApiInstrumentation
```

The runner prints a pass or a failure stack trace. It loads a WAV with a Unicode filename, plays, pauses, seeks, applies EQ, and closes with the stream active. It also repeats destruction and restart after EOF.
