package com.howl.music

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.howl.music.bridge.AnalysisSnapshot
import com.howl.music.bridge.EqState
import com.howl.music.bridge.HowlEngine
import com.howl.music.data.db.verifyRoomSchema
import com.howl.music.data.artwork.verifyArtwork
import com.howl.music.data.readUserData
import com.howl.music.data.writeUserData
import com.howl.music.data.scan.verifyLibraryScan
import com.howl.music.media.HowlMedia3Player
import com.howl.music.media.HowlPlaybackService
import com.howl.music.media.PlaybackEqualizer
import com.howl.music.media.PlaybackQueue
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/** Exercises the production bridge and native library on an Android control thread. */
class EngineApiInstrumentation : Instrumentation() {
    private var persistenceStep: String? = null

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        persistenceStep = arguments?.getString("persistenceStep")
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
                when (persistenceStep) {
                    "write" -> writeUserData(targetContext, context)
                    "read" -> readUserData(targetContext, fixture)
                    null -> {
                        verifyArtwork(targetContext, context, fixture)
                        verifyRoomSchema(targetContext)
                        verifyLibraryScan(targetContext)
                        writeUserData(targetContext, context)
                        readUserData(targetContext, fixture)
                        result.putString("analysis", verifyAnalysis())
                        verifyLifecycle(fixture)
                        verifyCloseAndRestart(fixture)
                        verifyMedia3(fixture)
                        verifyServicePlayback(fixture)
                    }
                    else -> error("Unknown persistence test step")
                }
            } finally {
                fixture.delete()
            }
            val summary = persistenceStep?.let { "User data persistence step $it passed." }
                ?: "Howl engine, analysis, playback, audio focus, schema, scan, user data, and artwork checks passed."
            result.putString("stream", "\n$summary\n")
            Activity.RESULT_OK
        } catch (error: Throwable) {
            result.putString("stream", "\n" + Log.getStackTraceString(error))
            Activity.RESULT_CANCELED
        }
        finish(status, result)
    }

    private fun verifyAnalysis(): String = HowlEngine.create().use { engine ->
        check(engine.analysis() == AnalysisSnapshot.Silent)
        engine.ok(engine.start(), "start sine for analysis")
        waitFor("sine callbacks") { engine.diagnostics().callbackCount > 3 }
        check(engine.analysis() == AnalysisSnapshot.Silent)
        var report = ""
        for (enabled in listOf(false, true, false)) {
            engine.ok(engine.setAnalysisEnabled(enabled), "analysis opt-in")
            val before = engine.diagnostics()
            SystemClock.sleep(2_000)
            val after = engine.diagnostics()
            val blocks = after.callbackCount - before.callbackCount
            check(blocks > 0)
            report += "\nanalysis=$enabled callbacks=$blocks underruns=${after.underrunCount - before.underrunCount}"
            val levels = engine.analysis()
            if (enabled) {
                check(levels.peak in 0.48f..0.51f && levels.rms in 0.30f..0.41f) { levels.toString() }
            } else {
                check(levels == AnalysisSnapshot.Silent)
            }
        }
        engine.ok(engine.setAnalysisEnabled(true), "reenable analysis")
        waitFor("meter signal") { engine.analysis().peak > 0.48f }
        engine.ok(engine.pause(), "pause analysis source")
        check(engine.analysis() == AnalysisSnapshot.Silent)
        engine.ok(engine.resume(), "resume analysis source")
        waitFor("resumed meter signal") { engine.analysis().peak > 0.48f }
        engine.ok(engine.stop(), "stop analysis source")
        check(engine.analysis() == AnalysisSnapshot.Silent)
        report
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
            engine.ok(engine.setAnalysisEnabled(true), "enable file analysis")
            engine.ok(engine.start(), "start")
            waitFor("file meter signal") { engine.analysis().rms > 0.01f }
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
    private fun verifyServicePlayback(fixture: File) {
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
                verifySessionEqualizer(first)
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
                if (Build.VERSION.SDK_INT >= 29) {
                    val visible = startActivitySync(
                        Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                    try {
                        verifyLibraryPlayback(second, fixture)
                    } finally {
                        onMain { visible.finish() }
                    }
                }
            } finally {
                onMain { second.release() }
            }
        } finally {
            onMain { if (!activity.isDestroyed) activity.finish() }
        }
    }

    private fun verifySessionEqualizer(controller: MediaController) {
        fun read(): PlaybackEqualizer.Settings {
            val response = onMain { controller.sendCustomCommand(PlaybackEqualizer.read, Bundle.EMPTY) }
                .get(5, TimeUnit.SECONDS)
            check(response.resultCode == SessionResult.RESULT_SUCCESS)
            return checkNotNull(PlaybackEqualizer.readSettings(response.extras))
        }

        fun apply(settings: PlaybackEqualizer.Settings) {
            val response = onMain {
                controller.sendCustomCommand(PlaybackEqualizer.apply, settings.toBundle())
            }.get(5, TimeUnit.SECONDS)
            check(response.resultCode == SessionResult.RESULT_SUCCESS)
        }

        val original = read()
        val changed = PlaybackEqualizer.Settings(EqState(-6f, FloatArray(EqState.BAND_COUNT) { it - 4f }))
        try {
            apply(changed)
            check(read().let { it.enabled && it.gains.preampDb == -6f &&
                it.gains.bandsDb.contentEquals(changed.gains.bandsDb) })
            apply(changed.copy(enabled = false))
            check(read().let { !it.enabled && it.gains.bandsDb.contentEquals(changed.gains.bandsDb) })
            apply(changed)
            val invalid = onMain {
                controller.sendCustomCommand(PlaybackEqualizer.apply, Bundle.EMPTY)
            }.get(5, TimeUnit.SECONDS)
            check(invalid.resultCode == SessionResult.RESULT_ERROR_BAD_VALUE)
            check(read().enabled && onMain { controller.isPlaying })
        } finally {
            apply(original)
        }
    }

    @androidx.annotation.RequiresApi(29)
    private fun verifyLibraryPlayback(controller: MediaController, fixture: File) {
        val resolver = targetContext.contentResolver
        val uri = checkNotNull(resolver.insert(
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "howl-library-${System.nanoTime()}.wav")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/HowlLibraryTest/")
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            },
        ))
        try {
            val wav = fixture.readBytes()
            ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN).putInt(24, 44_100).putInt(28, 176_400)
            checkNotNull(resolver.openOutputStream(uri)).use { it.write(wav) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            val item = MediaItem.Builder()
                .setMediaId(uri.toString())
                .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
                .setMediaMetadata(MediaMetadata.Builder().setTitle("Indexed WAV").build())
                .build()
            onMain { controller.setMediaItem(item) }
            waitFor("library item resolved") {
                onMain {
                    controller.currentMediaItem?.mediaId == item.mediaId &&
                        controller.isCommandAvailable(Player.COMMAND_PREPARE)
                }
            }
            onMain {
                controller.repeatMode = Player.REPEAT_MODE_ONE
                controller.prepare()
                controller.play()
            }
            waitFor("44.1 kHz library item playback") { onMain { controller.isPlaying } }
            onMain {
                check(controller.mediaMetadata.title?.toString() == "Indexed WAV")
                controller.setMediaItem(MediaItem.EMPTY)
            }
            waitFor("library source released") { onMain { controller.mediaItemCount == 0 } }
            verifySessionQueue(controller, item)
        } finally {
            resolver.delete(uri, null, null)
        }
    }

    private fun verifySessionQueue(controller: MediaController, source: MediaItem) {
        fun edit(action: PlaybackQueue.Action, title: String = "", id: String = "", target: String = "") {
            val args = Bundle().apply {
                putString(PlaybackQueue.ACTION, action.name)
                putString(PlaybackQueue.ENTRY_ID, id)
                putString(PlaybackQueue.TARGET_ID, target)
                if (title.isNotEmpty()) {
                    val track = source.buildUpon()
                        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
                        .build()
                    putBundle(PlaybackQueue.MEDIA, track.toBundle())
                }
            }
            val result = onMain { controller.sendCustomCommand(PlaybackQueue.edit, args) }.get(5, TimeUnit.SECONDS)
            check(result.resultCode == SessionResult.RESULT_SUCCESS) { "$action returned ${result.resultCode}" }
        }

        fun track(title: String): String = onMain {
            (0 until controller.mediaItemCount).map(controller::getMediaItemAt)
                .single { it.mediaMetadata.title?.toString() == title }.mediaId
        }

        fun current(title: String) {
            waitFor("queue current item $title") { onMain { controller.mediaMetadata.title?.toString() == title } }
        }

        edit(PlaybackQueue.Action.AddNext, title = "First")
        current("First")
        onMain { controller.repeatMode = Player.REPEAT_MODE_OFF }
        edit(PlaybackQueue.Action.AddNext, title = "Second")
        edit(PlaybackQueue.Action.AddNext, title = "Third")
        edit(PlaybackQueue.Action.AddNext, title = "Fourth")
        edit(PlaybackQueue.Action.MoveTo, id = track("Second"), target = track("Fourth"))
        waitFor("reordered upcoming tracks") {
            onMain {
                (0 until controller.mediaItemCount).map {
                    controller.getMediaItemAt(it).mediaMetadata.title?.toString()
                } == listOf("First", "Second", "Fourth", "Third")
            }
        }
        onMain { controller.seekToNextMediaItem() }
        current("Second")
        edit(PlaybackQueue.Action.Remove, id = track("Third"))
        edit(PlaybackQueue.Action.Remove, id = track("Second"))
        current("Fourth")
        check(onMain { !controller.playWhenReady })
        edit(PlaybackQueue.Action.AddNext, title = "Play now")
        edit(PlaybackQueue.Action.AddNext, title = "At EOF")
        edit(PlaybackQueue.Action.Play, id = track("Play now"))
        current("Play now")
        current("At EOF")
        edit(PlaybackQueue.Action.Remove, id = track("At EOF"))
        waitFor("queue empty after final removal") { onMain { controller.mediaItemCount == 0 } }
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
