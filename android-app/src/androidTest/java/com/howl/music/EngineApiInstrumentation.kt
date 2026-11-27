package com.howl.music

import android.app.Activity
import android.app.Instrumentation
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.howl.music.bridge.EqState
import com.howl.music.bridge.HowlEngine
import com.howl.music.media.HowlMedia3Player
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
                verifyMedia3(fixture)
            } finally {
                fixture.delete()
            }
            result.putString("stream", "\nHowl engine API and Media3 playback passed.\n")
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

    @UnstableApi
    private fun verifyMedia3(source: File) {
        val fixture = File.createTempFile("howl-media3-", ".wav", targetContext.cacheDir)
        val wav = source.readBytes()
        val header = wav.copyOfRange(0, 44)
        val pcm = wav.copyOfRange(44, wav.size)
        val repeats = 6
        ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(4, 36 + pcm.size * repeats)
            .putInt(40, pcm.size * repeats)
        fixture.outputStream().use { out ->
            out.write(header)
            repeat(repeats) { out.write(pcm) }
        }
        try {
            val player = onMain { HowlMedia3Player(targetContext.mainLooper) }
            try {
                val states = mutableListOf<Int>()
                val playRequests = mutableListOf<Boolean>()
                onMain {
                    player.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            states += playbackState
                        }

                        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                            playRequests += playWhenReady
                        }
                    })
                    player.setMediaItem(MediaItem.fromUri(Uri.fromFile(fixture)))
                    check(!player.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS))
                    player.prepare()
                    check(player.playbackState == Player.STATE_READY)
                    check(player.duration == 3_000L)
                    check(!player.isPlaying)
                    player.play()
                    check(player.playWhenReady && player.isPlaying)
                }
                waitFor("Media3 position advancing") { onMain { player.currentPosition >= 100 } }
                val frozen = onMain {
                    player.pause()
                    check(!player.playWhenReady && !player.isPlaying)
                    player.currentPosition
                }
                SystemClock.sleep(260)
                onMain {
                    check(player.currentPosition == frozen)
                    player.seekTo(2_700)
                    check(player.currentPosition == 2_700L)
                    player.play()
                }
                waitFor("Media3 end of stream") { onMain { player.playbackState == Player.STATE_ENDED } }
                onMain {
                    check(!player.isPlaying && player.currentPosition == player.duration)
                    check(Player.STATE_READY in states && Player.STATE_ENDED in states)
                    check(playRequests.take(3) == listOf(true, false, true))
                    player.play()
                    check(player.isPlaying && player.currentPosition < player.duration)
                    player.stop()
                    check(player.playbackState == Player.STATE_IDLE)
                    player.setMediaItem(MediaItem.fromUri("https://example.invalid/test.wav"))
                    player.prepare()
                    check(player.playerError != null && player.playbackState == Player.STATE_IDLE)
                }
            } finally {
                onMain { player.release() }
            }
        } finally {
            fixture.delete()
        }
    }

    private fun <T> onMain(action: () -> T): T {
        var result: Result<T>? = null
        runOnMainSync { result = runCatching(action) }
        return checkNotNull(result).getOrThrow()
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
