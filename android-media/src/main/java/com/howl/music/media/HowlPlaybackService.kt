package com.howl.music.media

import android.app.PendingIntent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

/** Owns the one session, player, and native playback engine for the app. */
@UnstableApi
class HowlPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private val sourceExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())
    @Volatile private var destroying = false
    private val stagedSources by lazy { File(cacheDir, "playback-sources-${UUID.randomUUID()}") }

    private fun outputRoute(id: Int): String? {
        if (id <= 0) return null
        val device = getSystemService(AudioManager::class.java)?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            ?.firstOrNull { it.id == id } ?: return null
        val type = when (device.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> "Built-in speaker"
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired audio"
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB audio"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER, AudioDeviceInfo.TYPE_BLE_BROADCAST -> "Bluetooth"
            AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC, AudioDeviceInfo.TYPE_HDMI_EARC -> "HDMI"
            else -> null
        }
        val name = device.productName.toString().trim().takeIf { it.isNotEmpty() }
        return when (type) {
            "USB audio", "Bluetooth" -> if (name == null) type else "$type · $name"
            else -> type ?: name
        }
    }

    private fun discardSource(uri: Uri) {
        val file = uri.path?.let(::File) ?: return
        if (file.parentFile == stagedSources) {
            sourceExecutor.execute { file.delete() }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val player = try {
            HowlMedia3Player(mainLooper, this, ::discardSource)
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
        destroying = true
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
            sourceExecutor.execute { stagedSources.deleteRecursively() }
            sourceExecutor.shutdown()
            super.onDestroy()
        }
    }

    private inner class PlaybackCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            if (controller.uid == applicationInfo.uid) {
                val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(PlaybackEqualizer.read)
                    .add(PlaybackEqualizer.apply)
                    .add(PlaybackQueue.edit)
                    .add(PlaybackAnalysis.read)
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(commands)
                    .build()
            }
            if (!controller.isTrusted) return MediaSession.ConnectionResult.reject()

            val transportCommands = Player.Commands.Builder()
                .addAll(
                    Player.COMMAND_PLAY_PAUSE, Player.COMMAND_PREPARE, Player.COMMAND_STOP,
                    Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                    Player.COMMAND_SEEK_BACK, Player.COMMAND_SEEK_FORWARD,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                    Player.COMMAND_GET_CURRENT_MEDIA_ITEM, Player.COMMAND_GET_TIMELINE,
                    Player.COMMAND_GET_METADATA,
                )
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(transportCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (controller.uid != applicationInfo.uid) {
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_PERMISSION_DENIED))
            }
            val player = session.player as HowlMedia3Player
            if (customCommand == PlaybackQueue.edit) return handleQueueEdit(player, args)
            val result = when (customCommand) {
                PlaybackAnalysis.read -> {
                    val extras = Bundle()
                    if (args.getBoolean(PlaybackAnalysis.DIAGNOSTICS_ONLY)) {
                        val diagnostics = player.analysisDiagnostics()
                        extras.putString(PlaybackAnalysis.GAPLESS, player.gaplessStatus())
                        extras.putLong(PlaybackAnalysis.UNDERRUNS, diagnostics.underrunCount)
                        extras.putLong(PlaybackAnalysis.CALLBACKS, diagnostics.callbackCount)
                        PlaybackOutputDiagnostics.write(
                            extras,
                            diagnostics,
                            player.hasDiagnosticSource(),
                            outputRoute(diagnostics.outputDeviceId),
                            player.equalizer.enabled,
                            diagnostics.sampleRate > 0 && !player.isPlaying,
                        )
                    } else {
                        val snapshot = player.readAnalysis()
                        extras.putFloat(PlaybackAnalysis.PEAK, snapshot.peak)
                        extras.putFloat(PlaybackAnalysis.RMS, snapshot.rms)
                    }
                    SessionResult(SessionResult.RESULT_SUCCESS, extras)
                }
                PlaybackEqualizer.read -> SessionResult(SessionResult.RESULT_SUCCESS, player.equalizer.toBundle())
                PlaybackEqualizer.apply -> {
                    val settings = PlaybackEqualizer.readSettings(args)
                    when {
                        settings == null -> SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
                        player.applyEqualizer(settings) ->
                            SessionResult(SessionResult.RESULT_SUCCESS, player.equalizer.toBundle())
                        else -> SessionResult(SessionResult.RESULT_ERROR_UNKNOWN)
                    }
                }
                else -> return super.onCustomCommand(session, controller, customCommand, args)
            }
            return Futures.immediateFuture(result)
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> = try {
            check(controller.uid == applicationInfo.uid) { "Only Howl can select media" }
            val requested = mediaItems.single()
            when {
                requested == MediaItem.EMPTY -> Futures.immediateFuture(listOf(MediaItem.EMPTY))
                requested.mediaId == SAMPLE_MEDIA_ID -> Futures.immediateFuture(listOf(loadSample()))
                else -> {
                    val uri = requireNotNull(indexedTrackUri(requested))
                    sourceExecutor.submit<List<MediaItem>> { listOf(stageTrack(requested, uri)) }
                }
            }
        } catch (failure: Exception) {
            Futures.immediateFailedFuture(failure)
        }
    }

    private fun handleQueueEdit(player: HowlMedia3Player, args: Bundle): ListenableFuture<SessionResult> {
        val action = PlaybackQueue.Action.entries.firstOrNull { it.name == args.getString(PlaybackQueue.ACTION) }
            ?: return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE))
        if (action != PlaybackQueue.Action.AddNext && action != PlaybackQueue.Action.AddNow) {
            val id = args.getString(PlaybackQueue.ENTRY_ID).orEmpty()
            val target = args.getString(PlaybackQueue.TARGET_ID).orEmpty()
            return Futures.immediateFuture(queueResult(id.isNotEmpty() && player.editQueue(action, id, target)))
        }

        val requested = args.getBundle(PlaybackQueue.MEDIA)?.let(MediaItem::fromBundle)
            ?: return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE))
        val uri = indexedTrackUri(requested)
            ?: return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE))
        val entry = requested.buildUpon().setMediaId(UUID.randomUUID().toString()).build()
        // Apply to the current queue when staging completes. No stale insertion index crosses threads.
        val result = SettableFuture.create<SessionResult>()
        val mainExecutor = ContextCompat.getMainExecutor(this)
        sourceExecutor.execute {
            if (!result.isCancelled) {
                val media = stageTrack(entry, uri)
                mainExecutor.execute {
                    val localUri = media.localConfiguration?.uri
                    if (localUri?.scheme == "file" && !destroying && !result.isCancelled && session?.player === player) {
                        val accepted = player.editQueue(action, media = media)
                        if (!accepted) discardSource(localUri)
                        result.set(queueResult(accepted))
                    } else {
                        if (localUri?.scheme == "file" && !destroying) discardSource(localUri)
                        result.set(SessionResult(SessionResult.RESULT_ERROR_IO))
                    }
                }
            }
        }
        return result
    }

    private fun queueResult(accepted: Boolean) =
        SessionResult(if (accepted) SessionResult.RESULT_SUCCESS else SessionResult.RESULT_ERROR_INVALID_STATE)

    private fun indexedTrackUri(item: MediaItem): Uri? {
        val uri = item.requestMetadata.mediaUri ?: return null
        val parts = uri.pathSegments
        return uri.takeIf {
            it.scheme == "content" && it.authority == "media" && parts.size == 4 &&
                parts[1] == "audio" && parts[2] == "media" &&
                parts[3].toLongOrNull()?.let { id -> id > 0 } == true
        }
    }

    private fun stageTrack(item: MediaItem, uri: Uri): MediaItem {
        // Native decoders reopen a seekable filesystem path. MediaStore descriptors may
        // be readable only through the provider and cannot always be reopened via procfs.
        var file: File? = null
        try {
            stagedSources.mkdirs()
            file = File.createTempFile("track-", ".audio", stagedSources)
            checkNotNull(contentResolver.openInputStream(uri)).use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (!destroying && !Thread.currentThread().isInterrupted) {
                        val size = input.read(buffer)
                        if (size < 0) break
                        output.write(buffer, 0, size)
                    }
                    check(!destroying && !Thread.currentThread().isInterrupted) { "Source staging cancelled" }
                }
            }
            return item.buildUpon().setUri(Uri.fromFile(file)).build()
        } catch (failure: Exception) {
            file?.delete()
            Log.w(TAG, "Could not stage the selected track", failure)
            // Leave the local URI unresolved so prepare reports a playback error.
            return item.buildUpon().setUri(uri).build()
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
