package com.howl.music.media

import android.app.PendingIntent
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.io.File

/** Owns the one session, player, and native playback engine for the app. */
@UnstableApi
class HowlPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = try {
            HowlMedia3Player(mainLooper, this)
        } catch (failure: RuntimeException) {
            Log.e(TAG, "Could not create the playback engine", failure)
            stopSelf()
            return
        }
        try {
            val builder = MediaSession.Builder(this, player).setCallback(PlaybackCallback())
            packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
                builder.setSessionActivity(
                    PendingIntent.getActivity(this, 0, launchIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
                )
            }
            session = builder.build()
        } catch (failure: RuntimeException) {
            player.release()
            Log.e(TAG, "Could not create the media session", failure)
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        session.takeIf { controllerInfo.uid == applicationInfo.uid || controllerInfo.isTrusted }

    override fun onDestroy() {
        val ownedSession = session
        session = null
        try {
            if (ownedSession != null) {
                val ownedPlayer = ownedSession.player
                try {
                    ownedSession.release()
                } finally {
                    ownedPlayer.release()
                }
            }
        } finally {
            super.onDestroy()
        }
    }

    private inner class PlaybackCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            if (controller.uid == applicationInfo.uid) return super.onConnect(session, controller)
            if (!controller.isTrusted) return MediaSession.ConnectionResult.reject()

            val transportCommands = Player.Commands.Builder()
                .addAll(
                    Player.COMMAND_PLAY_PAUSE, Player.COMMAND_PREPARE, Player.COMMAND_STOP,
                    Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                    Player.COMMAND_SEEK_BACK, Player.COMMAND_SEEK_FORWARD,
                    Player.COMMAND_GET_CURRENT_MEDIA_ITEM, Player.COMMAND_GET_TIMELINE,
                    Player.COMMAND_GET_METADATA,
                )
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(transportCommands)
                .build()
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> = try {
            check(controller.uid == applicationInfo.uid) { "Only Howl can select media" }
            val requested = mediaItems.single()
            val item = when {
                requested == MediaItem.EMPTY -> MediaItem.EMPTY
                requested.mediaId == SAMPLE_MEDIA_ID -> loadSample()
                else -> throw IllegalArgumentException("Unknown Howl media ID")
            }
            Futures.immediateFuture(listOf(item))
        } catch (failure: Exception) {
            Futures.immediateFailedFuture(failure)
        }
    }

    private fun loadSample(): MediaItem {
        val file = File(cacheDir, "howl-session-sample.wav")
        assets.open("sine_880_16bit_stereo.wav").use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return MediaItem.Builder()
            .setMediaId(SAMPLE_MEDIA_ID)
            .setUri(Uri.fromFile(file))
            .setMediaMetadata(MediaMetadata.Builder().setTitle("Howl test tone").build())
            .build()
    }

    companion object {
        const val SAMPLE_MEDIA_ID = "howl-sample"
        private const val TAG = "HowlPlaybackService"
    }
}
