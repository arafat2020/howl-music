package com.howl.music.media

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
import com.howl.music.bridge.HowlEngine

/**
 * Single-item Media3 player for local WAV and FLAC files. Owns one HowlEngine.
 * Create, command, and release it on [looper]. Commands run synchronously on that thread.
 */
@UnstableApi
class HowlMedia3Player(looper: Looper) : SimpleBasePlayer(looper) {
    private val engine = run {
        verifyApplicationThread()
        HowlEngine.create()
    }
    private val handler = Handler(looper)
    private var item: MediaItem? = null
    private var itemUid: Any = Any()
    private var playbackState = Player.STATE_IDLE
    private var playWhenReady = false
    private var started = false
    private var error: PlaybackException? = null
    private var durationMs = C.TIME_UNSET
    private var positionMs = 0L
    private val position = PositionSupplier { positionMs }

    private val poll = object : Runnable {
        override fun run() {
            command { refreshPosition() }
            invalidateState()
        }
    }

    override fun getState(): State {
        val current = item
        val commands = Player.Commands.Builder()
            .addAll(Player.COMMAND_RELEASE, Player.COMMAND_SET_MEDIA_ITEM,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM, Player.COMMAND_GET_TIMELINE,
                Player.COMMAND_GET_METADATA)
            .apply {
                if (current != null) {
                    addAll(Player.COMMAND_PREPARE, Player.COMMAND_PLAY_PAUSE, Player.COMMAND_STOP)
                }
                if (durationMs != C.TIME_UNSET) {
                    addAll(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_DEFAULT_POSITION, Player.COMMAND_SEEK_BACK,
                        Player.COMMAND_SEEK_FORWARD)
                }
            }
            .build()
        val playlist = if (current == null) emptyList() else listOf(
            MediaItemData.Builder(itemUid)
                .setMediaItem(current)
                .setIsSeekable(durationMs != C.TIME_UNSET)
                .setDurationUs(if (durationMs == C.TIME_UNSET) C.TIME_UNSET else durationMs * 1000)
                .build(),
        )
        return State.Builder()
            .setAvailableCommands(commands)
            .setPlaylist(playlist)
            .setPlaybackState(playbackState)
            .setPlayWhenReady(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlayerError(error)
            .setContentPositionMs(position)
            .setContentBufferedPositionMs(position)
            .build()
    }

    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<*> = command {
        val wasPrepared = playbackState != Player.STATE_IDLE
        stopStream()
        item = mediaItems.single()
        itemUid = Any()
        durationMs = C.TIME_UNSET
        positionMs = startPositionMs.coerceAtLeast(0)
        playbackState = Player.STATE_IDLE
        error = null
        if (wasPrepared) prepareItem()
    }

    override fun handlePrepare(): ListenableFuture<*> = command {
        if (playbackState == Player.STATE_IDLE) prepareItem()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> = command {
        refreshPosition()
        this.playWhenReady = playWhenReady
        if (playWhenReady) {
            if (playbackState == Player.STATE_ENDED) {
                positionMs = 0
                playbackState = Player.STATE_READY
            }
            if (playbackState == Player.STATE_READY) {
                if (started) checkStatus(engine.resume(), "Resume") else startStream()
                refreshPosition()
            }
        } else if (started) {
            checkStatus(engine.pause(), "Pause")
            refreshPosition()
        }
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int,
    ): ListenableFuture<*> = command {
        if (mediaItemIndex == 0 && durationMs != C.TIME_UNSET) {
            val requested = positionMs.coerceIn(0, durationMs)
            if (started) {
                checkStatus(engine.seekMs(requested), "Seek")
                refreshPosition()
            } else {
                this.positionMs = requested
                if (playbackState == Player.STATE_ENDED) {
                    playbackState = Player.STATE_READY
                    if (playWhenReady) startStream()
                }
            }
        }
    }

    override fun handleStop(): ListenableFuture<*> = command {
        refreshPosition()
        stopStream()
        playbackState = Player.STATE_IDLE
        error = null
    }

    override fun handleRelease(): ListenableFuture<*> {
        handler.removeCallbacks(poll)
        engine.close()
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
        positionMs = positionMs.coerceIn(0, durationMs)
        playbackState = Player.STATE_READY
        error = null
        if (playWhenReady) startStream()
    }

    private fun startStream() {
        val requested = positionMs
        checkStatus(engine.start(), "Start")
        started = true
        if (requested != 0L) {
            checkStatus(engine.seekMs(requested), "Seek at start")
        }
        refreshPosition()
    }

    private fun stopStream() {
        if (started) checkStatus(engine.stop(), "Stop")
        started = false
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
        const val POLL_INTERVAL_MS = 200L
    }
}
