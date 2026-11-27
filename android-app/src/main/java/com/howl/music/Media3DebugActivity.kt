package com.howl.music

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.howl.music.media.HowlMedia3Player
import kotlinx.coroutines.delay
import java.io.File

@UnstableApi
class Media3DebugActivity : ComponentActivity() {
    private var player: Player? = null
    private val status = mutableStateOf("Loading WAV")
    private val position = mutableStateOf(0L)
    private val playing = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val fixture = File(cacheDir, "media3-demo.wav")
            assets.open("sine_880_16bit_stereo.wav").use { input ->
                fixture.outputStream().use { output -> input.copyTo(output) }
            }
            val adapter = HowlMedia3Player(mainLooper)
            player = adapter
            adapter.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) = updatePlayer(player)
            })
            adapter.setMediaItem(MediaItem.fromUri(Uri.fromFile(fixture)))
            adapter.prepare()
            updatePlayer(adapter)
        } catch (failure: Exception) {
            status.value = failure.message ?: "Unable to open the Media3 demo"
        }

        setContent {
            LaunchedEffect(Unit) {
                while (true) {
                    player?.let(::updatePlayer)
                    delay(250)
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Media3 controls")
                Text(status.value)
                Text("${position.value} ms")
                Button(onClick = {
                    player?.let {
                        if (playing.value) {
                            it.pause()
                        } else {
                            if (it.playbackState == Player.STATE_IDLE) it.prepare()
                            it.play()
                        }
                    }
                }, enabled = player != null) {
                    Text(if (playing.value) "Pause" else "Play")
                }
                Button(onClick = { player?.seekTo(250) }, enabled = player != null) {
                    Text("Seek to 250 ms")
                }
                Button(onClick = { player?.stop() }, enabled = player != null) { Text("Stop") }
            }
        }
    }

    private fun updatePlayer(player: Player) {
        position.value = player.currentPosition
        playing.value = player.isPlaying
        status.value = player.playerError?.message ?: when (player.playbackState) {
            Player.STATE_READY -> if (player.isPlaying) "Playing" else "Ready"
            Player.STATE_ENDED -> "Ended"
            else -> "Idle"
        }
    }

    override fun onStop() {
        player?.pause()
        super.onStop()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
