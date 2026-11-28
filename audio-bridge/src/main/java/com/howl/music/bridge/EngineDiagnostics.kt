package com.howl.music.bridge

/**
 * Control-thread snapshot of native stream / callback health (Phase 1.3).
 *
 * UI must poll this (e.g. ~2 Hz). Never push updates from the audio callback.
 */
data class EngineDiagnostics(
    val sampleRate: Int,
    val channelCount: Int,
    val framesPerBurst: Int,
    val framesPerCallback: Int,
    val callbackCount: Long,
    val underrunCount: Long,
    val errorCount: Long,
    val sourceCodec: SourceCodec = SourceCodec.Unknown,
    val sourceRate: Int = 0,
    val sourceChannels: Int = 0,
    val sourceBits: Int = 0,
    val sourceFloat: Boolean = false,
    val hardwareRate: Int = 0,
    val outputDeviceId: Int = 0,
    val platformXruns: Long = -1,

) {
    enum class SourceCodec { Unknown, WAV, FLAC }

    fun formatLines(): String = buildString {
        if (sourceCodec != SourceCodec.Unknown) {
            appendLine("source=${sourceCodec.name} $sourceRate Hz  $sourceBits-bit  ch=$sourceChannels")
        }
        if (hardwareRate > 0) appendLine("hardware-reported rate=$hardwareRate Hz")
        if (platformXruns >= 0) appendLine("platform xruns=$platformXruns")
        appendLine("rate=$sampleRate Hz  ch=$channelCount")
        appendLine("burst=$framesPerBurst  cbFrames=$framesPerCallback")
        appendLine("callbacks=$callbackCount")
        appendLine("xruns=$underrunCount  errors=$errorCount")
    }

    companion object {
        val Empty = EngineDiagnostics(0, 0, 0, 0, 0, 0, 0)
    }
}
