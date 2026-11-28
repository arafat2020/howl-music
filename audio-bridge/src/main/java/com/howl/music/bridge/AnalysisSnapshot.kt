package com.howl.music.bridge

/** Peak and RMS of the latest rendered block across channels. 1.0 is full scale. */
data class AnalysisSnapshot(val peak: Float, val rms: Float) {
    companion object {
        val Silent = AnalysisSnapshot(0f, 0f)
    }
}
