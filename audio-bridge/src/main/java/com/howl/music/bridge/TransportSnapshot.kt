package com.howl.music.bridge

/**
 * Control-thread transport snapshot (Phase 2.3).
 * Poll from UI — never from an audio callback.
 */
data class TransportSnapshot(
    val positionFrames: Long,
    val durationFrames: Long,
    val sampleRate: Int,
    val paused: Boolean,
    val playing: Boolean,
) {
    val positionMs: Long
        get() = if (sampleRate <= 0) 0 else (positionFrames * 1000L) / sampleRate

    val durationMs: Long
        get() = if (sampleRate <= 0) 0 else (durationFrames * 1000L) / sampleRate

    fun formatLine(): String {
        val p = positionMs / 1000.0
        val d = durationMs / 1000.0
        val flag = when {
            !playing -> "stopped"
            paused -> "paused"
            else -> "playing"
        }
        return "pos=%.1fs / %.1fs (%s)".format(p, d, flag)
    }

    companion object {
        val Empty = TransportSnapshot(0, 0, 0, paused = false, playing = false)
    }
}
