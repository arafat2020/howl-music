package com.howl.music.media

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.howl.music.bridge.AnalysisSnapshot
import com.howl.music.bridge.HowlEngine

/**
 * Playlist-backed Media3 player for local WAV and FLAC files. Owns one HowlEngine.
 * Create, command, and release it on [looper]. Commands run synchronously on that thread.
 */
@UnstableApi
class HowlMedia3Player internal constructor(
    looper: Looper,
    context: Context?,
    private val onDiscardSource: (Uri) -> Unit = {},
) : SimpleBasePlayer(looper) {
    constructor(looper: Looper) : this(looper, null)

    private val engine = run {
        verifyApplicationThread()
        HowlEngine.create()
    }
    internal var equalizer = PlaybackEqualizer.Settings()
        private set

    internal fun applyEqualizer(settings: PlaybackEqualizer.Settings): Boolean {
        verifyApplicationThread()
        // The native engine publishes one complete state and smooths the transition.
        val status = if (settings.enabled) engine.applyEqState(settings.gains) else engine.resetEqToFlat()
        if (status != 0) return false
        equalizer = settings
        return true
    }

    private val handler = Handler(looper)
    private var analyzing = false
    private val expireAnalysis = Runnable { stopAnalysis() }

    internal fun readAnalysis(): AnalysisSnapshot {
        verifyApplicationThread()
        if (!isPlaying) return AnalysisSnapshot.Silent
        if (!analyzing) analyzing = engine.setAnalysisEnabled(true) == 0
        if (!analyzing) return AnalysisSnapshot.Silent
        handler.removeCallbacks(expireAnalysis)
        handler.postDelayed(expireAnalysis, ANALYSIS_LEASE_MS)
        return engine.analysis()
    }

    internal fun analysisDiagnostics() = engine.diagnostics()

    private fun stopAnalysis() {
        handler.removeCallbacks(expireAnalysis)
        if (analyzing) {
            engine.setAnalysisEnabled(false)
            analyzing = false
        }
    }

    private class Entry(val item: MediaItem)

    private val playlist = mutableListOf<Entry>()
    private var publishedTimeline: List<MediaItemData>? = null
    private var current: Entry? = null
    private val item: MediaItem? get() = current?.item
    private val currentIndex: Int get() = playlist.indexOf(current)
    private var playbackState = Player.STATE_IDLE
    private var playWhenReady = false
    private var playWhenReadyReason = Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
    private val audioFocus = context?.let {
        AudioFocusPolicy(
            it.applicationContext,
            handler,
            onFocusLost = { interruptPlayback(Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS) },
            onBecomingNoisy = { interruptPlayback(Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY) },
        )
    }
    private var repeatMode = Player.REPEAT_MODE_OFF
    private var started = false
    private var error: PlaybackException? = null
    private var durationMs = C.TIME_UNSET
    private var positionMs = 0L
    private var repeatDiscontinuity = false
    private val position = PositionSupplier { positionMs }

    private val poll = object : Runnable {
        override fun run() {
            command {
                val previous = positionMs
                val entry = current
                refreshPosition()
                if (playWhenReady && playbackState == Player.STATE_ENDED) {
                    val next = playlist.getOrNull(currentIndex + 1)
                        ?: playlist.firstOrNull().takeIf { repeatMode == Player.REPEAT_MODE_ALL }
                    if (next != null) select(next, 0, prepare = true)
                }
                repeatDiscontinuity = current === entry && started && repeatMode == Player.REPEAT_MODE_ONE &&
                    positionMs < previous
            }
            invalidateState()
        }
    }

    override fun getState(): State {
        val playingIndex = currentIndex
        val canWrap = repeatMode == Player.REPEAT_MODE_ALL && playlist.size > 1
        val commands = Player.Commands.Builder()
            .addAll(Player.COMMAND_RELEASE, Player.COMMAND_SET_MEDIA_ITEM,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM, Player.COMMAND_GET_TIMELINE,
                Player.COMMAND_GET_METADATA, Player.COMMAND_SET_REPEAT_MODE)
            .apply {
                if (playingIndex >= 0) {
                    addAll(Player.COMMAND_PREPARE, Player.COMMAND_PLAY_PAUSE, Player.COMMAND_STOP,
                        Player.COMMAND_SEEK_TO_MEDIA_ITEM)
                    if (playingIndex > 0 || canWrap) add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    if (playingIndex < playlist.lastIndex || canWrap) add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                }
                if (durationMs != C.TIME_UNSET) {
                    addAll(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_DEFAULT_POSITION, Player.COMMAND_SEEK_BACK,
                        Player.COMMAND_SEEK_FORWARD)
                }
            }
            .build()
        val timeline = publishedTimeline ?: playlist.map { entry ->
            val duration = if (entry === this.current) durationMs else C.TIME_UNSET
            MediaItemData.Builder(entry)
                .setMediaItem(entry.item)
                .setIsSeekable(duration != C.TIME_UNSET)
                .setDurationUs(if (duration == C.TIME_UNSET) C.TIME_UNSET else duration * 1000)
                .build()
        }.also { publishedTimeline = it }
        return State.Builder()
            .setAvailableCommands(commands)
            .setPlaylist(timeline)
            .setCurrentMediaItemIndex(playingIndex)
            .setPlaybackState(playbackState)
            .setRepeatMode(repeatMode)
            .setPlayWhenReady(playWhenReady, playWhenReadyReason)
            .setPlayerError(error)
            .setContentPositionMs(position)
            .setContentBufferedPositionMs(position)
            .apply {
                if (repeatDiscontinuity) {
                    repeatDiscontinuity = false
                    setPositionDiscontinuity(Player.DISCONTINUITY_REASON_AUTO_TRANSITION, positionMs)
                }
            }
            .build()
    }

    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<*> = command {
        val wasPrepared = playbackState != Player.STATE_IDLE
        val previous = playlist.toList()
        val replacement = mediaItems.filterNot { it == MediaItem.EMPTY }.map(::Entry)
        playlist.clear()
        playlist.addAll(replacement)
        val index = startIndex.coerceIn(0, playlist.lastIndex.coerceAtLeast(0))
        try {
            select(playlist.getOrNull(index), startPositionMs, prepare = wasPrepared)
        } finally {
            previous.forEach(::discardSource)
        }
    }

    /** All queue edits resolve entry IDs on the player looper, even after auto-advance. */
    internal fun editQueue(
        action: PlaybackQueue.Action,
        id: String = "",
        targetId: String = "",
        media: MediaItem? = null,
    ): Boolean {
        verifyApplicationThread()
        val entry = playlist.firstOrNull { it.item.mediaId == id }
        val index = playlist.indexOf(entry)
        val playingIndex = currentIndex
        val targetIndex = playlist.indexOfFirst { it.item.mediaId == targetId }
        when (action) {
            PlaybackQueue.Action.AddNext, PlaybackQueue.Action.AddNow -> if (media == null) return false
            PlaybackQueue.Action.Play -> if (entry == null) return false
            PlaybackQueue.Action.Remove -> if (entry == null) return false
            PlaybackQueue.Action.MoveEarlier, PlaybackQueue.Action.MoveNext ->
                if (index <= playingIndex + 1) return false
            PlaybackQueue.Action.MoveLater ->
                if (index <= playingIndex || index >= playlist.lastIndex) return false
            PlaybackQueue.Action.MoveTo ->
                if (index <= playingIndex || targetIndex <= playingIndex || index == targetIndex) return false
        }

        command {
            when (action) {
                PlaybackQueue.Action.AddNext, PlaybackQueue.Action.AddNow -> {
                    val fromSample = item?.mediaId == HowlPlaybackService.SAMPLE_MEDIA_ID
                    if ((fromSample || current == null) && repeatMode == Player.REPEAT_MODE_ONE) {
                        setNativeRepeatMode(Player.REPEAT_MODE_OFF)
                    }
                    val next = Entry(checkNotNull(media))
                    playlist.add(playingIndex + 1, next)
                    if (fromSample && action == PlaybackQueue.Action.AddNow) playlist.removeAt(playingIndex)
                    if (action == PlaybackQueue.Action.AddNow || current == null) {
                        if (action == PlaybackQueue.Action.AddNow) {
                            playWhenReady = true
                            playWhenReadyReason = Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
                        }
                        select(next, 0, prepare = true)
                    }
                }
                PlaybackQueue.Action.Play -> {
                    if (index > playingIndex + 1) playlist.add(playingIndex + 1, playlist.removeAt(index))
                    playWhenReady = true
                    playWhenReadyReason = Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
                    select(checkNotNull(entry), 0, prepare = true)
                }
                PlaybackQueue.Action.Remove -> {
                    if (entry === current) {
                        val next = playlist.getOrNull(index + 1)
                        if (next == null) {
                            playWhenReady = false
                            // Completed tracks are history. Removing the final current track ends the queue.
                            val removed = playlist.toList()
                            playlist.clear()
                            try {
                                select(null, 0, prepare = false)
                            } finally {
                                removed.forEach(::discardSource)
                            }
                        } else {
                            playlist.removeAt(index)
                            try {
                                select(next, 0, prepare = true)
                            } finally {
                                discardSource(checkNotNull(entry))
                            }
                        }
                    } else {
                        discardSource(playlist.removeAt(index))
                    }
                }
                PlaybackQueue.Action.MoveEarlier, PlaybackQueue.Action.MoveLater,
                PlaybackQueue.Action.MoveNext, PlaybackQueue.Action.MoveTo -> {
                    val target = when (action) {
                        PlaybackQueue.Action.MoveEarlier -> index - 1
                        PlaybackQueue.Action.MoveLater -> index + 1
                        PlaybackQueue.Action.MoveTo -> targetIndex
                        else -> playingIndex + 1
                    }
                    playlist.add(target, playlist.removeAt(index))
                }
            }
        }
        publishedTimeline = null
        invalidateState()
        return true
    }

    private fun select(entry: Entry?, position: Long, prepare: Boolean) {
        current = entry
        publishedTimeline = null
        durationMs = C.TIME_UNSET
        positionMs = position.coerceAtLeast(0)
        playbackState = Player.STATE_IDLE
        error = null
        stopStream()
        if (entry == null) {
            playWhenReady = false
        } else if (prepare) {
            prepareItem()
        }
    }

    override fun handlePrepare(): ListenableFuture<*> = command {
        if (playbackState == Player.STATE_IDLE) prepareItem()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> = command {
        refreshPosition()
        playWhenReadyReason = Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
        if (playWhenReady) {
            this.playWhenReady = true
            if (playbackState == Player.STATE_ENDED) {
                positionMs = 0
                playbackState = Player.STATE_READY
            }
            if (playbackState == Player.STATE_READY) {
                startOrResumeStream()
                refreshPosition()
            }
        } else {
            pausePlayback()
        }
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int,
    ): ListenableFuture<*> = command {
        if (mediaItemIndex != currentIndex) {
            playlist.getOrNull(mediaItemIndex)?.let {
                select(it, positionMs, prepare = playbackState != Player.STATE_IDLE || playWhenReady)
            }
        } else if (durationMs != C.TIME_UNSET) {
            val requested = positionMs.coerceIn(0, durationMs)
            if (started) {
                checkStatus(engine.seekMs(requested), "Seek")
                refreshPosition()
            } else {
                this.positionMs = requested
                if (playbackState == Player.STATE_ENDED) {
                    playbackState = Player.STATE_READY
                    if (playWhenReady) startOrResumeStream()
                }
            }
        }
    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> = command {
        setNativeRepeatMode(repeatMode)
    }

    private fun setNativeRepeatMode(repeatMode: Int) {
        checkStatus(
            engine.setRepeatMode(
                if (repeatMode == Player.REPEAT_MODE_ONE) HowlEngine.RepeatMode.One
                else HowlEngine.RepeatMode.None,
            ),
            "Set repeat",
        )
        this.repeatMode = repeatMode
    }

    override fun handleStop(): ListenableFuture<*> = command {
        refreshPosition()
        stopStream()
        playbackState = Player.STATE_IDLE
        error = null
    }

    override fun handleRelease(): ListenableFuture<*> {
        handler.removeCallbacks(poll)
        stopAnalysis()
        try {
            engine.close()
        } finally {
            playlist.forEach(::discardSource)
            audioFocus?.release()
        }
        started = false
        playbackState = Player.STATE_IDLE
        return Futures.immediateVoidFuture()
    }

    private fun prepareItem() {
        val uri = checkNotNull(item?.localConfiguration?.uri) { "Media item has no local file" }
        val path = requireNotNull(uri.path?.takeIf { uri.scheme == "file" && it.startsWith("/") }) {
            "Howl playback requires an absolute file URI"
        }
        checkStatus(engine.setPlayMode(HowlEngine.PlayMode.File), "Select file mode")
        checkStatus(engine.load(path), "Load media")
        durationMs = engine.transport().durationMs
        publishedTimeline = null
        positionMs = positionMs.coerceIn(0, durationMs)
        playbackState = Player.STATE_READY
        error = null
        if (playWhenReady) startOrResumeStream()
    }

    private fun discardSource(entry: Entry) {
        entry.item.localConfiguration?.uri?.let(onDiscardSource)
    }

    private fun startOrResumeStream() {
        if (audioFocus?.acquire() == false) {
            playWhenReady = false
            playWhenReadyReason = Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS
            return
        }
        if (started) {
            checkStatus(engine.resume(), "Resume")
            return
        }
        val requested = positionMs
        checkStatus(engine.start(), "Start")
        started = true
        if (requested != 0L) {
            checkStatus(engine.seekMs(requested), "Seek at start")
        }
        refreshPosition()
    }

    private fun interruptPlayback(reason: Int) {
        command {
            playWhenReadyReason = reason
            pausePlayback()
        }
        invalidateState()
    }

    private fun pausePlayback() {
        playWhenReady = false
        stopAnalysis()
        try {
            refreshPosition()
            if (started) {
                checkStatus(engine.pause(), "Pause")
                refreshPosition()
            }
        } finally {
            audioFocus?.release()
        }
    }

    private fun stopStream() {
        try {
            if (started) checkStatus(engine.stop(), "Stop")
            started = false
        } finally {
            audioFocus?.release()
        }
    }

    private fun refreshPosition() {
        if (!started) return
        val snapshot = engine.transport()
        if (snapshot.playing) {
            positionMs = snapshot.positionMs
        } else {
            // EOF resets the native stream position and can still be finishing teardown.
            stopStream()
            positionMs = durationMs
            playbackState = Player.STATE_ENDED
        }
    }

    private inline fun command(action: () -> Unit): ListenableFuture<*> {
        handler.removeCallbacks(poll)
        try {
            action()
        } catch (failure: RuntimeException) {
            runCatching { stopStream() }
            playbackState = Player.STATE_IDLE
            playWhenReady = false
            error = PlaybackException(failure.message, failure, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        }
        if (started && playWhenReady) handler.postDelayed(poll, POLL_INTERVAL_MS)
        return Futures.immediateVoidFuture()
    }

    private fun checkStatus(status: Int, operation: String) {
        check(status == 0) { "$operation failed (${engine.statusName(status)})" }
    }

    private companion object {
        const val ANALYSIS_LEASE_MS = 750L
        const val POLL_INTERVAL_MS = 200L
    }
}
