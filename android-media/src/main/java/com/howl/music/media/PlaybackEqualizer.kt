package com.howl.music.media

import android.os.Bundle
import androidx.media3.session.SessionCommand
import com.howl.music.bridge.EqState

/** App-only protocol for the EQ on the engine owned by the playback session. */
object PlaybackEqualizer {
    val read = SessionCommand("com.howl.music.eq.READ", Bundle.EMPTY)
    val apply = SessionCommand("com.howl.music.eq.APPLY", Bundle.EMPTY)

    data class Settings(val gains: EqState = EqState(), val enabled: Boolean = true) {
        fun toBundle() = Bundle().apply {
            putFloat(PREAMP, gains.preampDb)
            putFloatArray(BANDS, gains.bandsDb)
            putBoolean(ENABLED, enabled)
        }
    }

    fun readSettings(bundle: Bundle): Settings? {
        val bands = bundle.getFloatArray(BANDS) ?: return null
        if (!bundle.containsKey(PREAMP) || !bundle.containsKey(ENABLED)) return null
        val preamp = bundle.getFloat(PREAMP)
        if (preamp !in -24f..12f || bands.size != EqState.BAND_COUNT || bands.any { it !in -12f..12f }) {
            return null
        }
        return Settings(EqState(preamp, bands), bundle.getBoolean(ENABLED))
    }

    private const val PREAMP = "preamp"
    private const val BANDS = "bands"
    private const val ENABLED = "enabled"
}
