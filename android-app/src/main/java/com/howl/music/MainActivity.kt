package com.howl.music

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.howl.music.media.HowlPlaybackService
import com.howl.music.ui.debug.ScanActivity
import kotlinx.coroutines.delay

@UnstableApi
class MainActivity : ComponentActivity() {
    private var connection: PlayerController? = null
    private val player = mutableStateOf<MediaController?>(null)
    private val status = mutableStateOf("Connecting to playback service")
    private val position = mutableStateOf(0L)
    private val playing = mutableStateOf(false)
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
            LaunchedEffect(player.value, playing.value) {
                while (playing.value) {
                    player.value?.let(::updatePlayer)
                    delay(250)
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFF101014)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Howl", color = Color(0xFFB7F34A), fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Text(status.value, color = Color.White, modifier = Modifier.padding(top = 8.dp))
                Text("${position.value} ms", color = Color.White)
                Button(
                    onClick = ::togglePlayback,
                    enabled = player.value != null && !pendingPlay.value && !pendingDiagnostics.value,
                ) {
                    Text(if (playing.value) "Pause" else "Play")
                }
                Button(
                    onClick = { player.value?.stop() },
                    enabled = player.value != null && !pendingDiagnostics.value,
                ) { Text("Stop") }
                Button(
                    onClick = { player.value?.seekTo(0) },
                    enabled = player.value?.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) == true &&
                        !pendingDiagnostics.value,
                ) { Text("Seek to start") }
                if (BuildConfig.DEBUG) {
                    Button(onClick = { startActivity(Intent(this@MainActivity, ScanActivity::class.java)) }) {
                        Text("Scan music")
                    }
                    Button(
                        onClick = ::openDiagnostics,
                        enabled = player.value != null && !pendingPlay.value && !pendingDiagnostics.value,
                    ) { Text("Engine diagnostics") }
                }
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
        playing.value = false
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
        playing.value = false
        status.value = message
    }

    private fun togglePlayback() {
        val controller = player.value ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else if (controller.currentMediaItem == null) {
            pendingPlay.value = true
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
        position.value = player.currentPosition
        playing.value = player.isPlaying
        status.value = player.playerError?.message ?: when (player.playbackState) {
            Player.STATE_READY -> if (player.isPlaying) "Playing" else "Paused"
            Player.STATE_ENDED -> "Ended"
            else -> "Idle"
        }
    }
}
