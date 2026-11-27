package com.howl.music.bridge

/** Numeric file playback EQ state, with bands in ascending frequency order (31.25 Hz to 16 kHz). */
class EqState(
    val preampDb: Float = 0f,
    bandsDb: FloatArray = FloatArray(BAND_COUNT),
) {
    private val gains = bandsDb.copyOf()

    init {
        require(gains.size == BAND_COUNT) { "EQ requires $BAND_COUNT band gains." }
        require(preampDb.isFinite() && gains.all { it.isFinite() }) { "EQ gains must be finite." }
    }

    val bandsDb: FloatArray
        get() = gains.copyOf()

    companion object {
        const val BAND_COUNT = 10
    }
}
