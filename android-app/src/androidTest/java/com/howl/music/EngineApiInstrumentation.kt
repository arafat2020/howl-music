package com.howl.music

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.howl.music.bridge.EqState
import com.howl.music.bridge.HowlEngine
import com.howl.music.data.db.verifyRoomSchema
import com.howl.music.data.scan.verifyLibraryScan
import com.howl.music.media.HowlMedia3Player
import com.howl.music.media.HowlPlaybackService
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/** Exercises the production bridge and native library on an Android control thread. */
class EngineApiInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        val result = Bundle()
        val status = try {
            verifyRoomSchema(targetContext)
            verifyLibraryScan(targetContext)
            val fixture = File.createTempFile("howl-🎵-", ".wav", targetContext.cacheDir)
            try {
                targetContext.assets.open("sine_880_16bit_stereo.wav").use { input ->
                    fixture.outputStream().use { input.copyTo(it) }
                }
                verifyLifecycle(fixture)
                verifyCloseAndRestart(fixture)
                verifyMedia3(fixture)
                verifyServicePlayback()
            } finally {
                fixture.delete()
            }
            result.putString("stream", "\nHowl engine API, Media3 playback, service reconnect, audio focus, Room schema, and library scan passed.\n")
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

    @UnstableApi
    private fun verifyServicePlayback() {
        val activity = startActivitySync(
            Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        val token = SessionToken(targetContext, ComponentName(targetContext, HowlPlaybackService::class.java))
        fun connect(): MediaController =
            onMain { MediaController.Builder(targetContext, token).buildAsync() }.get(5, TimeUnit.SECONDS)

        try {
            val first = connect()
            try {
                onMain {
                    first.setMediaItem(MediaItem.Builder().setMediaId(HowlPlaybackService.SAMPLE_MEDIA_ID).build())
                }
                waitFor("service resolving local media") {
                    onMain {
                        first.isCommandAvailable(Player.COMMAND_PREPARE) &&
                            first.mediaMetadata.title?.toString() == "Howl test tone"
                    }
                }
                onMain {
                    first.repeatMode = Player.REPEAT_MODE_ONE
                    first.prepare()
                    first.play()
                }
                waitFor("service playback") { onMain { first.isPlaying } }
                verifyAudioFocus(first)
            } finally {
                onMain { first.release() }
            }

            onMain { activity.finish() }
            waitFor("Activity destruction") { onMain { activity.isDestroyed } }
            SystemClock.sleep(600) // The fixture is 500 ms, so playback must survive a repeat.
            val second = connect()
            try {
                onMain {
                    check(second.currentMediaItem?.mediaId == HowlPlaybackService.SAMPLE_MEDIA_ID)
                    check(second.isPlaying && second.repeatMode == Player.REPEAT_MODE_ONE)
                    second.pause()
                }
                waitFor("service paused") { onMain { !second.playWhenReady } }
                onMain { second.setMediaItem(MediaItem.EMPTY) }
                waitFor("service cleared media") { onMain { second.mediaItemCount == 0 } }
            } finally {
                onMain { second.release() }
            }
        } finally {
            onMain { if (!activity.isDestroyed) activity.finish() }
        }
    }

    private fun verifyAudioFocus(controller: MediaController) {
        val audio = checkNotNull(targetContext.getSystemService(AudioManager::class.java))
        val reasons = mutableListOf<Int>()
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) reasons += reason
            }
        }
        fun request(gain: Int) = AudioFocusRequest.Builder(gain)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
            .setOnAudioFocusChangeListener { }
            .build()

        onMain { controller.addListener(listener) }
        try {
            for (gain in listOf(
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
                AudioManager.AUDIOFOCUS_GAIN,
            )) {
                val rival = request(gain)
                val pauseCount = onMain { reasons.size }
                try {
                    check(audio.requestAudioFocus(rival) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                    waitFor("focus loss pauses playback") {
                        onMain {
                            !controller.isPlaying && !controller.playWhenReady &&
                                reasons.size > pauseCount &&
                                reasons.last() == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS
                        }
                    }
                } finally {
                    audio.abandonAudioFocusRequest(rival)
                }
                SystemClock.sleep(240)
                onMain {
                    check(!controller.isPlaying && !controller.playWhenReady)
                    controller.play()
                }
                waitFor("explicit resume after focus loss") { onMain { controller.isPlaying } }
            }

            val rapid = request(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            repeat(12) {
                try {
                    check(audio.requestAudioFocus(rapid) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                } finally {
                    audio.abandonAudioFocusRequest(rapid)
                }
            }
            waitFor("rapid focus loss leaves playback paused") {
                onMain { !controller.playWhenReady && !controller.isPlaying }
            }
            onMain { controller.play() }
            waitFor("explicit resume after rapid focus changes") { onMain { controller.isPlaying } }
        } finally {
            onMain { controller.removeListener(listener) }
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
