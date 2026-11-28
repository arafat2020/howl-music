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
import android.os.Debug
import android.os.Process
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import androidx.lifecycle.Lifecycle
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
import com.howl.music.bridge.TransportSnapshot
import com.howl.music.data.db.verifyRoomSchema
import com.howl.music.data.artwork.verifyArtwork
import com.howl.music.data.readUserData
import com.howl.music.data.writeUserData
import com.howl.music.data.scan.verifyLibraryScan
import com.howl.music.media.HowlMedia3Player
import com.howl.music.media.HowlPlaybackService
import com.howl.music.media.PlaybackAnalysis
import com.howl.music.media.PlaybackEqualizer
import com.howl.music.media.PlaybackQueue
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/** Exercises the production bridge and native library on an Android control thread. */
class EngineApiInstrumentation : Instrumentation() {
    private var persistenceStep: String? = null
    private var profileScenarios: List<String>? = null
    private var profileSeconds = 15
    private var profileBackgroundSeconds = 1800
    private var profileSetupSeconds = 12
    private var profileTrackUri: Uri? = null

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        persistenceStep = arguments?.getString("persistenceStep")
        val scenarios = arguments?.getString("profileScenarios")
        val legacyVisualizer = arguments?.getString("profileVisualizer") == "true"
        profileScenarios = scenarios?.split(',') ?: if (legacyVisualizer) listOf("now-playing", "visualizer") else null
        profileSeconds = arguments?.getString("profileSeconds")?.toIntOrNull() ?: if (legacyVisualizer) 5 else 15
        profileBackgroundSeconds = arguments?.getString("profileBackgroundSeconds")?.toIntOrNull() ?: 1800
        profileSetupSeconds = arguments?.getString("profileSetupSeconds")?.toIntOrNull() ?: 12
        profileTrackUri = arguments?.getString("profileTrackUri")?.let(Uri::parse)
        start()
    }

    override fun onStart() {
        val result = Bundle()
        val status = try {
            profileScenarios?.let { scenarios ->
                result.putString("stream", "\n${profilePlayback(scenarios)}\n")
                finish(Activity.RESULT_OK, result)
                return
            }
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
                        result.putString("gapless", verifyGapless(fixture))
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
            if (enabled) {
                // Short callbacks can contain only part of a sine cycle.
                waitFor("sine block with representative peak and RMS") {
                    val levels = engine.analysis()
                    levels.peak in 0.48f..0.51f && levels.rms in 0.30f..0.41f
                }
            } else {
                check(engine.analysis() == AnalysisSnapshot.Silent)
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

    private fun verifyGapless(wav: File): String {
        val flac = File.createTempFile("howl-gapless-", ".flac", targetContext.cacheDir)
        try {
            targetContext.assets.open("sine_880_16bit_stereo.flac").use { input ->
                flac.outputStream().use { input.copyTo(it) }
            }
            return listOf(wav, flac).joinToString("\n") { source ->
                HowlEngine.create().use { engine ->
                    engine.ok(engine.enableGaplessTransport(), "enable gapless transport")
                    engine.ok(engine.setPlayMode(HowlEngine.PlayMode.File), "file mode")
                    engine.ok(engine.load(source.absolutePath), "load first track")
                    engine.ok(engine.start(), "start gapless fixture")
                    engine.ok(engine.setGaplessNext(source.absolutePath, 1, 0), "prime next track")
                    waitFor("first gapless audio callbacks") { engine.diagnostics().callbackCount > 3 }
                    val before = engine.diagnostics()
                    waitFor("rendered gapless track boundary") { engine.transport().handoffToken == 1L }
                    val joined = engine.transport()
                    val after = engine.diagnostics()
                    check(joined.gaplessMode == TransportSnapshot.GaplessMode.Joined)
                    check(joined.positionFrames < joined.durationFrames)
                    check(after.callbackCount > before.callbackCount)
                    check(after.underrunCount == before.underrunCount)
                    engine.ok(engine.setGaplessNext(null, 0, joined.handoffToken), "acknowledge handoff")
                    engine.ok(engine.pause(), "pause joined track")
                    engine.ok(engine.seekMs(100), "seek within joined track")
                    check(engine.transport().let { it.handoffToken == 1L && it.positionMs == 100L })
                    "${source.extension} callbacks=${after.callbackCount - before.callbackCount} underruns=0"
                }
            }
        } finally {
            flac.delete()
        }
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
                verifySessionAnalysis(first)
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

    // Opt-in manual profile. Follow the status prompts and leave each screen visible for its sample window.
    private fun profilePlayback(scenarios: List<String>): String {
        val prompts = mapOf(
            "background" to "Press Home and leave Howl in the background",
            "now-playing" to "Open Now Playing and leave the album art visible",
            "eq" to "Open EQ and adjust sliders during the measurement",
            "visualizer" to "Open Visualizer with Reduce motion off",
        )
        require(scenarios.isNotEmpty() && scenarios.all { it in prompts })
        require(profileSeconds in 2..300 && profileBackgroundSeconds in 2..3600 && profileSetupSeconds in 2..120)
        val activity = startActivitySync(
            Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        val token = SessionToken(targetContext, ComponentName(targetContext, HowlPlaybackService::class.java))
        val controller = onMain { MediaController.Builder(targetContext, token).buildAsync() }.get(5, TimeUnit.SECONDS)
        try {
            val uri = profileTrackUri
            val item = if (uri == null) {
                MediaItem.Builder().setMediaId(HowlPlaybackService.SAMPLE_MEDIA_ID).build()
            } else {
                MediaItem.Builder().setMediaId(uri.toString())
                    .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
                    .setMediaMetadata(MediaMetadata.Builder().setTitle("Howl profile track").build())
                    .build()
            }
            onMain { controller.setMediaItem(item) }
            waitFor("profile track prepared for playback", timeoutMs = 60_000) {
                onMain { controller.currentMediaItem?.mediaId == item.mediaId &&
                    controller.isCommandAvailable(Player.COMMAND_PREPARE) }
            }
            onMain {
                controller.repeatMode = if (uri == null) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                controller.prepare()
                controller.play()
            }
            waitFor("profile tone playing") { onMain { controller.isPlaying } }

            fun measure(scenario: String): String {
                val background = scenario == "background"
                val seconds = if (background) profileBackgroundSeconds else profileSeconds
                fun report(stage: String, message: String) = sendStatus(1, Bundle().apply {
                    putString("phase", stage)
                    putString("scenario", scenario)
                    putString("profile", message)
                })
                report("setup", prompts.getValue(scenario))
                SystemClock.sleep(profileSetupSeconds * 1000L)
                fun verifyActivityState() = onMain {
                    val state = (activity as MainActivity).lifecycle.currentState
                    check(if (background) state == Lifecycle.State.CREATED else state == Lifecycle.State.RESUMED) {
                        "Unexpected activity state $state for $scenario"
                    }
                    check(controller.currentMediaItem?.mediaId == item.mediaId && controller.isPlaying) {
                        "Playback must remain on the profiling track"
                    }
                }
                verifyActivityState()
                val durations = mutableListOf<LongArray>()
                var dropped = 0
                val listener = Window.OnFrameMetricsAvailableListener { _, metrics, skipped ->
                    dropped += skipped
                    durations += longArrayOf(
                        metrics.getMetric(FrameMetrics.TOTAL_DURATION),
                        metrics.getMetric(FrameMetrics.DRAW_DURATION),
                        metrics.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION),
                    )
                }
                report("measure", "Measuring $scenario for $seconds seconds")
                val pssStart = Debug.getPss()
                val before = visualizerDiagnostics(controller)
                if (!background) onMain {
                    activity.window.addOnFrameMetricsAvailableListener(listener, Handler(Looper.getMainLooper()))
                }
                val start = SystemClock.elapsedRealtimeNanos()
                val cpuStart = Process.getElapsedCpuTime()
                SystemClock.sleep(seconds * 1000L)
                val cpuMs = Process.getElapsedCpuTime() - cpuStart
                val end = SystemClock.elapsedRealtimeNanos()
                val after = visualizerDiagnostics(controller)
                val frames = if (background) emptyList() else onMain {
                    activity.window.removeOnFrameMetricsAvailableListener(listener)
                    durations.filter { it[0] >= 0 }
                }
                val pssEnd = Debug.getPss()
                verifyActivityState()
                val elapsedMs = (end - start) / 1_000_000.0
                val callbacks = after.getLong(PlaybackAnalysis.CALLBACKS) - before.getLong(PlaybackAnalysis.CALLBACKS)
                val underruns = after.getLong(PlaybackAnalysis.UNDERRUNS) - before.getLong(PlaybackAnalysis.UNDERRUNS)
                check(callbacks > 0 && underruns >= 0) { "Audio counters reset or playback stalled during $scenario" }
                fun p95(metric: Int): String {
                    val sorted = frames.map { it[metric] }.filter { it >= 0 }.sorted()
                    if (sorted.isEmpty()) return "NA"
                    return (sorted[(kotlin.math.ceil(sorted.size * 0.95).toInt() - 1)] / 1_000_000.0).toString()
                }
                val summary = "$scenario elapsedMs=$elapsedMs cpuMs=$cpuMs cpuPct=${100 * cpuMs / elapsedMs} " +
                    "pssStartKiB=$pssStart pssEndKiB=$pssEnd frames=${frames.size} droppedReports=$dropped " +
                    "p95Ms=${p95(0)} drawP95Ms=${p95(1)} swapP95Ms=${p95(2)} " +
                    "audioCallbacks=$callbacks underruns=$underruns startBootNs=$start endBootNs=$end"
                report("result", summary)
                return summary
            }

            return scenarios.joinToString("\n", transform = ::measure)
        } finally {
            onMain {
                controller.setMediaItem(MediaItem.EMPTY)
                controller.release()
                activity.finish()
            }
        }
    }

    private fun visualizerDiagnostics(controller: MediaController): Bundle {
        val response = onMain {
            controller.sendCustomCommand(PlaybackAnalysis.read, Bundle().apply {
                putBoolean(PlaybackAnalysis.DIAGNOSTICS_ONLY, true)
            })
        }.get(5, TimeUnit.SECONDS)
        check(response.resultCode == SessionResult.RESULT_SUCCESS)
        return response.extras
    }

    private fun verifySessionAnalysis(controller: MediaController) {
        fun read(): Bundle {
            val response = onMain {
                controller.sendCustomCommand(PlaybackAnalysis.read, Bundle.EMPTY)
            }.get(5, TimeUnit.SECONDS)
            check(response.resultCode == SessionResult.RESULT_SUCCESS)
            return response.extras
        }

        fun window(poll: Boolean): String {
            val before = visualizerDiagnostics(controller)
            var hasSignal = false
            repeat(40) {
                if (poll) {
                    val snapshot = PlaybackAnalysis.snapshot(read())
                    hasSignal = hasSignal || snapshot.peak > 0.01f && snapshot.rms > 0f
                }
                SystemClock.sleep(50)
            }
            val after = visualizerDiagnostics(controller)
            val callbacks = after.getLong(PlaybackAnalysis.CALLBACKS) - before.getLong(PlaybackAnalysis.CALLBACKS)
            val underruns = after.getLong(PlaybackAnalysis.UNDERRUNS) - before.getLong(PlaybackAnalysis.UNDERRUNS)
            check(callbacks > 0 && (!poll || hasSignal))
            return "poll=$poll callbacks=$callbacks underruns=$underruns"
        }

        val baseline = window(false)
        val polling = window(true)
        Log.i("HowlVisualizer", "$baseline; $polling")
        onMain { controller.pause() }
        waitFor("visualizer playback paused") { onMain { !controller.isPlaying } }
        check(PlaybackAnalysis.snapshot(read()) == AnalysisSnapshot.Silent)
        onMain { controller.play() }
        waitFor("visualizer playback resumed") { onMain { controller.isPlaying } }
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
        onMain {
            repeat(10) { controller.seekToNextMediaItem() }
            controller.seekTo(0, 0)
        }
        current("First")
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
        check(visualizerDiagnostics(controller).getString(PlaybackAnalysis.GAPLESS) == "Joined")
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

    private fun waitFor(description: String, timeoutMs: Long = 3_000, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (!condition()) {
            check(SystemClock.elapsedRealtime() < deadline) { "Timed out waiting for $description" }
            SystemClock.sleep(10)
        }
    }
}
