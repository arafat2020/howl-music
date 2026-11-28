package com.howl.music

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.howl.music.data.db.LibraryRow
import com.howl.music.data.db.HowlDatabase
import com.howl.music.data.prefs.PreferencesRepository
import com.howl.music.media.HowlPlaybackService
import com.howl.music.ui.components.HowlButton
import com.howl.music.ui.debug.ArtworkPreviewActivity
import com.howl.music.ui.debug.ComponentGalleryActivity
import com.howl.music.ui.debug.ScanActivity
import com.howl.music.ui.theme.HowlTheme
import com.howl.music.ui.nav.HowlNavHost
import com.howl.music.ui.player.PlaybackUiState
import kotlinx.coroutines.launch

@UnstableApi
class MainActivity : ComponentActivity() {
    private val database by lazy { HowlDatabase.open(this) }
    private val preferences by lazy { PreferencesRepository(database) }
    private var connection: PlayerController? = null
    private val player = mutableStateOf<MediaController?>(null)
    private val playback = mutableStateOf(PlaybackUiState())
    private var pendingMediaId: String? = null
    private val pendingDiagnostics = mutableStateOf(false)

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            completePendingAction(player)
            updatePlayer(player)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val reduceMotion = preferences.reducedMotion.collectAsStateWithLifecycle(initialValue = true).value
            HowlTheme(reduceMotion = reduceMotion) {
                HowlNavHost(
                    playback = playback.value,
                    onToggle = ::togglePlayback,
                    onPlayTrack = ::playTrack,
                    onScan = { startActivity(Intent(this, ScanActivity::class.java)) },
                    onSeek = ::seek,
                    onSkipBack = { player.value?.takeIf { it.isCommandAvailable(Player.COMMAND_SEEK_BACK) }?.seekBack() },
                    onSkipForward = { player.value?.takeIf { it.isCommandAvailable(Player.COMMAND_SEEK_FORWARD) }?.seekForward() },
                    onReduceMotionChange = { enabled -> lifecycleScope.launch { preferences.setReducedMotion(enabled) } },
                    readPositionMs = { player.value?.currentPosition ?: 0L },
                    debugTools = if (BuildConfig.DEBUG) {
                        {
                            HowlButton("Scan music", { startActivity(Intent(this@MainActivity, ScanActivity::class.java)) }, primary = false)
                            HowlButton("Artwork preview", { startActivity(Intent(this@MainActivity, ArtworkPreviewActivity::class.java)) }, primary = false)
                            HowlButton("Component gallery", { startActivity(Intent(this@MainActivity, ComponentGalleryActivity::class.java)) }, primary = false)
                            HowlButton(
                                label = "Engine diagnostics",
                                onClick = ::openDiagnostics,
                                enabled = playback.value.canToggle,
                                primary = false,
                            )
                        }
                    } else null,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connection = PlayerController(this, ::attachPlayer, ::onPlaybackUnavailable)
    }

    override fun onStop() {
        player.value?.removeListener(listener)
        player.value = null
        playback.value = playback.value.copy(
            canToggle = false, canSeek = false, canSkipBack = false, canSkipForward = false, isPlaying = false,
        )
        pendingMediaId = null
        pendingDiagnostics.value = false
        connection?.close()
        connection = null
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        database.close()
    }

    private fun seek(mediaId: String, positionMs: Long) {
        val controller = player.value ?: return
        if (pendingMediaId == null && controller.currentMediaItem?.mediaId == mediaId &&
            controller.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
        ) {
            controller.seekTo(positionMs)
        }
    }

    private fun attachPlayer(controller: MediaController) {
        player.value = controller
        controller.addListener(listener)
        updatePlayer(controller)
    }

    private fun onPlaybackUnavailable(message: String) {
        player.value = null
        pendingMediaId = null
        pendingDiagnostics.value = false
        playback.value = PlaybackUiState(status = message)
    }

    private fun togglePlayback() {
        val controller = player.value ?: return
        if (controller.playWhenReady && controller.playbackState != Player.STATE_ENDED &&
            controller.playbackState != Player.STATE_IDLE
        ) {
            controller.pause()
        } else if (controller.currentMediaItem == null) {
            selectMedia(controller, MediaItem.Builder().setMediaId(HowlPlaybackService.SAMPLE_MEDIA_ID).build())
        } else {
            startPlayback(controller)
        }
    }

    private fun playTrack(track: LibraryRow) {
        val controller = player.value ?: return
        val uri = track.uri ?: return
        val item = MediaItem.Builder()
            .setMediaId(uri)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(Uri.parse(uri)).build())
            .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist).build())
            .build()
        selectMedia(controller, item)
    }

    private fun selectMedia(controller: MediaController, item: MediaItem) {
        if (!controller.isCommandAvailable(Player.COMMAND_SET_MEDIA_ITEM)) return
        pendingMediaId = item.mediaId
        updatePlayer(controller)
        controller.setMediaItem(item)
    }

    private fun startPlayback(player: Player) {
        player.repeatMode = if (player.currentMediaItem?.mediaId == HowlPlaybackService.SAMPLE_MEDIA_ID) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        player.play()
    }

    private fun openDiagnostics() {
        val controller = player.value ?: return
        pendingDiagnostics.value = true
        updatePlayer(controller)
        if (controller.currentMediaItem == null) {
            completePendingAction(controller)
        } else {
            controller.setMediaItem(MediaItem.EMPTY)
        }
    }

    private fun completePendingAction(player: Player) {
        if (pendingMediaId != null && player.isCommandAvailable(Player.COMMAND_PREPARE) &&
            player.currentMediaItem?.mediaId == pendingMediaId
        ) {
            pendingMediaId = null
            startPlayback(player)
        }
        if (pendingDiagnostics.value && player.currentMediaItem == null) {
            pendingDiagnostics.value = false
            startActivity(Intent(this, EngineDebugActivity::class.java))
        }
    }

    private fun updatePlayer(player: Player) {
        val item = player.currentMediaItem
        val readyForCommands = pendingMediaId == null && !pendingDiagnostics.value
        playback.value = PlaybackUiState(
            mediaId = item?.mediaId,
            artist = player.mediaMetadata.artist?.toString(),
            durationMs = player.duration.coerceAtLeast(0L),
            canSkipBack = readyForCommands && player.isCommandAvailable(Player.COMMAND_SEEK_BACK),
            canSkipForward = readyForCommands && player.isCommandAvailable(Player.COMMAND_SEEK_FORWARD),
            title = item?.let { player.mediaMetadata.title?.toString() ?: it.mediaId },
            artworkTrackUri = (item?.requestMetadata?.mediaUri ?: item?.localConfiguration?.uri)?.toString(),
            status = player.playerError?.message ?: when {
                pendingMediaId != null -> "Loading track"
                player.playbackState == Player.STATE_READY -> if (player.isPlaying) "Playing" else "Paused"
                player.playbackState == Player.STATE_ENDED -> "Ended"
                else -> "Idle"
            },
            playRequested = player.playWhenReady && player.playbackState != Player.STATE_IDLE &&
                player.playbackState != Player.STATE_ENDED,
            canToggle = readyForCommands && (item == null ||
                player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)),
            canSeek = readyForCommands && player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM),
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition,
        )
    }
}
