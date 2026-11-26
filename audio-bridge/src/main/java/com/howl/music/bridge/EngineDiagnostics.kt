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
) {
    fun formatLines(): String = buildString {
        appendLine("rate=$sampleRate Hz  ch=$channelCount")
        appendLine("burst=$framesPerBurst  cbFrames=$framesPerCallback")
        appendLine("callbacks=$callbackCount")
        appendLine("xruns=$underrunCount  errors=$errorCount")
    }

    companion object {
        val Empty = EngineDiagnostics(0, 0, 0, 0, 0, 0, 0)
    }
}
