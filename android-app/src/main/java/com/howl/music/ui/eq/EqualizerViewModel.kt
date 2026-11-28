package com.howl.music.ui.eq

import android.app.Application
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.howl.music.bridge.EqState
import com.howl.music.data.db.HowlDatabase
import com.howl.music.data.eq.EqPresetRepository
import com.howl.music.media.PlaybackEqualizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class EqualizerUiState(
    val settings: PlaybackEqualizer.Settings = PlaybackEqualizer.Settings(),
    val connected: Boolean = false,
    val applying: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
)

@UnstableApi
internal class EqualizerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = HowlDatabase.open(application)
    private val repository = EqPresetRepository(database)
    val presets = repository.presets

    private val mutableState = MutableStateFlow(EqualizerUiState())
    val state = mutableState.asStateFlow()
    private val executor = ContextCompat.getMainExecutor(application)
    private var controller: MediaController? = null
    private var generation = 0
    private var applied = PlaybackEqualizer.Settings()
    private var queued: PlaybackEqualizer.Settings? = null

    fun connect(player: MediaController?) {
        controller = player
        generation++
        queued = null
        mutableState.update { it.copy(connected = false, applying = false, error = null) }
        if (player == null) return
        send(player, PlaybackEqualizer.read, Bundle.EMPTY) { settings ->
            if (settings == null) {
                mutableState.update { it.copy(error = "Couldn’t read the playback equalizer.") }
            } else {
                applied = settings
                mutableState.update { it.copy(connected = true, settings = settings) }
            }
        }
    }

    fun setEnabled(enabled: Boolean) = submit(state.value.settings.copy(enabled = enabled))

    fun setGain(band: Int?, db: Float) {
        val settings = state.value.settings
        val gains = settings.gains
        val updated = if (band == null) {
            EqState(db, gains.bandsDb)
        } else {
            EqState(gains.preampDb, gains.bandsDb.also { it[band] = db })
        }
        submit(settings.copy(gains = updated))
    }

    fun applyPreset(gains: EqState) {
        // Stored presets predate the UI. Match the native clamping rules when loading them.
        val clamped = EqState(gains.preampDb.coerceIn(-24f, 12f),
            gains.bandsDb.map { it.coerceIn(-12f, 12f) }.toFloatArray())
        submit(state.value.settings.copy(gains = clamped))
    }

    fun savePreset(name: String, onSaved: () -> Unit) {
        val snapshot = state.value
        if (!snapshot.connected || snapshot.saving || name.isBlank()) return
        mutableState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                repository.create(name, snapshot.settings.gains)
                onSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(error = "Couldn’t save the preset.") }
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }

    private fun submit(settings: PlaybackEqualizer.Settings) {
        val player = controller ?: return
        val snapshot = state.value
        if (!snapshot.connected) return
        mutableState.update { it.copy(settings = settings, applying = true, error = null) }
        if (snapshot.applying) {
            queued = settings
        } else {
            apply(player, settings)
        }
    }

    private fun apply(player: MediaController, settings: PlaybackEqualizer.Settings) {
        send(player, PlaybackEqualizer.apply, settings.toBundle()) { result ->
            if (result != null) applied = result
            val next = queued
            queued = null
            if (next != null) {
                apply(player, next)
            } else {
                mutableState.update {
                    it.copy(
                        settings = applied,
                        applying = false,
                        error = if (result == null) "Couldn’t apply the equalizer changes." else null,
                    )
                }
            }
        }
    }

    private fun send(
        player: MediaController,
        command: SessionCommand,
        args: Bundle,
        onResult: (PlaybackEqualizer.Settings?) -> Unit,
    ) {
        val connection = generation
        if (!player.isConnected || !player.isSessionCommandAvailable(command)) {
            onResult(null)
            return
        }
        val future = player.sendCustomCommand(command, args)
        future.addListener({
            if (connection == generation) {
                val result = runCatching { future.get() }.getOrNull()
                onResult(result?.takeIf { it.resultCode == SessionResult.RESULT_SUCCESS }
                    ?.let { PlaybackEqualizer.readSettings(it.extras) })
            }
        }, executor)
    }

    override fun onCleared() {
        connect(null)
        database.close()
    }
}
