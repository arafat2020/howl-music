package com.howl.music.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import androidx.core.content.ContextCompat

/** Audio focus and route-loss events for the session, serialized on the player looper. */
internal class AudioFocusPolicy(
    private val context: Context,
    private val handler: Handler,
    private val onFocusLost: () -> Unit,
    private val onBecomingNoisy: () -> Unit,
) {
    private val manager = checkNotNull(context.getSystemService(AudioManager::class.java))
    private var request: AudioFocusRequest? = null
    private var generation = 0L
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (request != null && intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                onBecomingNoisy()
            }
        }
    }

    fun acquire(): Boolean {
        if (request != null) return true

        val current = ++generation
        val candidate = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener({ change ->
                if (current == generation && request != null) {
                    when (change) {
                        AudioManager.AUDIOFOCUS_LOSS,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> onFocusLost()
                    }
                }
            }, handler)
            .build()
        if (manager.requestAudioFocus(candidate) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return false
        }
        try {
            ContextCompat.registerReceiver(
                context,
                noisyReceiver,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                null,
                handler,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            request = candidate
        } catch (failure: RuntimeException) {
            manager.abandonAudioFocusRequest(candidate)
            throw failure
        }
        return true
    }

    fun release() {
        val held = request ?: return
        request = null
        try {
            context.unregisterReceiver(noisyReceiver)
        } finally {
            manager.abandonAudioFocusRequest(held)
        }
    }
}
