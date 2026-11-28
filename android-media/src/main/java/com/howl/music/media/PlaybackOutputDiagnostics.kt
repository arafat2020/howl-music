package com.howl.music.media

import android.os.Bundle
import com.howl.music.bridge.EngineDiagnostics

/** Diagnostic values copied from the session-owned engine and Android's output device list. */
data class PlaybackOutputDiagnostics(
    val sourceCodec: String?,
    val sourceRate: Int,
    val sourceChannels: Int,
    val sourceBits: Int,
    val sourceFloat: Boolean,
    val streamRate: Int,
    val streamChannels: Int,
    val hardwareRate: Int,
    val route: String?,
    val eqEnabled: Boolean,
    val paused: Boolean,
    val underruns: Long,
    val platformXruns: Long,
    val errors: Long,
    val gapless: String?,
) {
    companion object {
        internal fun write(
            bundle: Bundle,
            engine: EngineDiagnostics,
            hasSource: Boolean,
            route: String?,
            eqEnabled: Boolean,
            paused: Boolean,
        ) = with(bundle) {
            if (hasSource && engine.sourceCodec != EngineDiagnostics.SourceCodec.Unknown) {
                putString("sourceCodec", engine.sourceCodec.name)
                putInt("sourceRate", engine.sourceRate)
                putInt("sourceChannels", engine.sourceChannels)
                putInt("sourceBits", engine.sourceBits)
                putBoolean("sourceFloat", engine.sourceFloat)
            }
            putInt("streamRate", engine.sampleRate)
            putInt("streamChannels", engine.channelCount)
            putInt("hardwareRate", engine.hardwareRate)
            putString("route", route)
            putBoolean("eqEnabled", eqEnabled)
            putBoolean("paused", paused)
            putLong("platformXruns", engine.platformXruns)
            putLong("streamErrors", engine.errorCount)
        }

        fun read(bundle: Bundle) = PlaybackOutputDiagnostics(
            sourceCodec = bundle.getString("sourceCodec"),
            sourceRate = bundle.getInt("sourceRate"),
            sourceChannels = bundle.getInt("sourceChannels"),
            sourceBits = bundle.getInt("sourceBits"),
            sourceFloat = bundle.getBoolean("sourceFloat"),
            streamRate = bundle.getInt("streamRate"),
            streamChannels = bundle.getInt("streamChannels"),
            hardwareRate = bundle.getInt("hardwareRate"),
            route = bundle.getString("route"),
            eqEnabled = bundle.getBoolean("eqEnabled"),
            paused = bundle.getBoolean("paused"),
            underruns = bundle.getLong(PlaybackAnalysis.UNDERRUNS),
            platformXruns = bundle.getLong("platformXruns", -1),
            errors = bundle.getLong("streamErrors"),
            gapless = bundle.getString(PlaybackAnalysis.GAPLESS),
        )
    }
}
