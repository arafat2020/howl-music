package com.howl.music

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.howl.music.media.HowlPlaybackService

/** Activity-scoped connection. Close releases the controller and its service binding. */
@UnstableApi
internal class PlayerController(
    context: Context,
    private val onConnected: (MediaController) -> Unit,
    private val onUnavailable: (String) -> Unit,
) : AutoCloseable {
    private var closed = false
    private val future = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, HowlPlaybackService::class.java)),
    ).setListener(object : MediaController.Listener {
        override fun onDisconnected(controller: MediaController) {
            if (!closed) onUnavailable("Playback service disconnected")
        }
    }).buildAsync()

    init {
        future.addListener({
            if (!closed) {
                val controller = runCatching(future::get).getOrNull()
                if (controller != null) onConnected(controller)
                else onUnavailable("Unable to connect to the playback service")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override fun close() {
        if (closed) return
        closed = true
        MediaController.releaseFuture(future)
    }
}
