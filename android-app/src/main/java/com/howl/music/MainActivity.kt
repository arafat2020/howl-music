package com.howl.music

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.howl.music.media.HowlPlaybackService
import com.howl.music.ui.components.HowlButton
import com.howl.music.ui.debug.ArtworkPreviewActivity
import com.howl.music.ui.debug.ComponentGalleryActivity
import com.howl.music.ui.debug.ScanActivity
import com.howl.music.ui.theme.HowlTheme
import com.howl.music.ui.nav.HowlNavHost
import com.howl.music.ui.player.PlaybackUiState

@UnstableApi
class MainActivity : ComponentActivity() {
    private var connection: PlayerController? = null
    private val player = mutableStateOf<MediaController?>(null)
    private val playback = mutableStateOf(PlaybackUiState())
    private val pendingPlay = mutableStateOf(false)
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
            HowlTheme {
                HowlNavHost(
                    playback = playback.value,
                    onToggle = ::togglePlayback,
                    onStop = { player.value?.stop() },
                    onSeekStart = { player.value?.seekTo(0) },
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
        playback.value = playback.value.copy(canToggle = false, canSeek = false)
        pendingPlay.value = false
        pendingDiagnostics.value = false
        connection?.close()
        connection = null
        super.onStop()
    }

    private fun attachPlayer(controller: MediaController) {
        player.value = controller
        controller.addListener(listener)
        updatePlayer(controller)
    }

    private fun onPlaybackUnavailable(message: String) {
        player.value = null
        pendingPlay.value = false
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
            pendingPlay.value = true
            updatePlayer(controller)
            controller.setMediaItem(MediaItem.Builder().setMediaId(HowlPlaybackService.SAMPLE_MEDIA_ID).build())
        } else {
            startPlayback(controller)
        }
    }

    private fun startPlayback(player: Player) {
        player.repeatMode = Player.REPEAT_MODE_ONE
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
        if (pendingPlay.value && player.isCommandAvailable(Player.COMMAND_PREPARE) &&
            player.currentMediaItem?.mediaId == HowlPlaybackService.SAMPLE_MEDIA_ID
        ) {
            pendingPlay.value = false
            startPlayback(player)
        }
        if (pendingDiagnostics.value && player.currentMediaItem == null) {
            pendingDiagnostics.value = false
            startActivity(Intent(this, EngineDebugActivity::class.java))
        }
    }

    private fun updatePlayer(player: Player) {
        val item = player.currentMediaItem
        val readyForCommands = !pendingPlay.value && !pendingDiagnostics.value
        playback.value = PlaybackUiState(
            title = item?.let { player.mediaMetadata.title?.toString() ?: it.mediaId },
            artworkTrackUri = item?.localConfiguration?.uri?.toString(),
            status = player.playerError?.message ?: when {
                pendingPlay.value -> "Loading track"
                player.playbackState == Player.STATE_READY -> if (player.isPlaying) "Playing" else "Paused"
                player.playbackState == Player.STATE_ENDED -> "Ended"
                else -> "Idle"
            },
            playRequested = player.playWhenReady && player.playbackState != Player.STATE_IDLE &&
                player.playbackState != Player.STATE_ENDED,
            canToggle = readyForCommands && (item == null ||
                player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)),
            canSeek = readyForCommands && player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM),
            positionMs = player.currentPosition,
        )
    }
}
