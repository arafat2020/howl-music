package com.howl.music

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import com.howl.music.bridge.EqState
import com.howl.music.bridge.HowlEngine
import java.io.File

/** Exercises the production bridge and native library on an Android control thread. */
class EngineApiInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        val result = Bundle()
        val status = try {
            val fixture = File.createTempFile("howl-🎵-", ".wav", targetContext.cacheDir)
            try {
                targetContext.assets.open("sine_880_16bit_stereo.wav").use { input ->
                    fixture.outputStream().use { input.copyTo(it) }
                }
                verifyLifecycle(fixture)
                verifyCloseAndRestart(fixture)
            } finally {
                fixture.delete()
            }
            result.putString("stream", "\nHowl engine API: lifecycle, EQ, and destroy/restart passed.\n")
            Activity.RESULT_OK
        } catch (error: Throwable) {
            result.putString("stream", "\n" + Log.getStackTraceString(error))
            Activity.RESULT_CANCELED
        }
        finish(status, result)
    }

    private fun verifyLifecycle(fixture: File) {
        check(HowlEngine.libraryError() == null) { HowlEngine.libraryError().orEmpty() }
        check(HowlEngine.version().isNotBlank())
        val engine = HowlEngine.create()
        engine.use {
            engine.ok(engine.prepare(), "prepare")
            engine.ok(engine.setPlayMode(HowlEngine.PlayMode.File), "file mode")
            check(engine.statusName(engine.load("bad\u0000path")) == "InvalidArgument")
            engine.ok(engine.load(fixture.absolutePath), "load WAV")
            engine.ok(engine.enqueue(fixture.absolutePath), "enqueue WAV")
            check(engine.queueSize() == 2)
            engine.ok(engine.setRepeatMode(HowlEngine.RepeatMode.All), "repeat")
            engine.ok(engine.start(), "start")
            waitFor("audio callbacks") { engine.diagnostics().callbackCount > 0 }
            engine.ok(engine.pause(), "pause")
            check(engine.transport().paused)
            val midpoint = engine.transport().durationMs / 2
            check(midpoint > 0)
            check(engine.statusName(engine.seekMs(Long.MAX_VALUE)) == "InvalidState")
            engine.ok(engine.seekMs(midpoint), "seek")
            check(engine.transport().positionMs == midpoint)
            engine.ok(engine.applyEqState(EqState(-3f, FloatArray(EqState.BAND_COUNT) { 2f })), "EQ")
            engine.ok(engine.resume(), "resume")
            check(!engine.transport().paused)
            engine.ok(engine.resetEqToFlat(), "reset EQ")
            // use() closes the handle with the stream still open.
        }
        check(engine.isClosed)
        engine.close()
        check(runCatching { engine.transport() }.exceptionOrNull() is IllegalStateException)
    }

    private fun verifyCloseAndRestart(fixture: File) {
        repeat(12) {
            HowlEngine.create().use { engine ->
                engine.ok(engine.setPlayMode(HowlEngine.PlayMode.File), "file mode")
                engine.ok(engine.load(fixture.absolutePath), "load before destroy")
                engine.ok(engine.start(), "start before destroy")
                when (it % 3) {
                    0 -> {
                        engine.ok(engine.seekMs(engine.transport().durationMs), "seek to EOF")
                        waitFor("automatic EOF teardown") { engine.stateName() == "Idle" }
                        engine.ok(engine.start(), "restart after EOF")
                    }
                    1 -> engine.ok(engine.seekMs(engine.transport().durationMs), "seek before close")
                    else -> waitFor("audio callbacks before destroy") {
                        engine.diagnostics().callbackCount > 0
                    }
                }
            }
        }
    }

    private fun HowlEngine.ok(code: Int, operation: String) {
        check(code == 0) { "$operation returned ${statusName(code)}" }
    }

    private fun waitFor(description: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 3_000
        while (!condition()) {
            check(SystemClock.elapsedRealtime() < deadline) { "Timed out waiting for $description" }
            SystemClock.sleep(10)
        }
    }
}
