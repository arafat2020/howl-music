package com.howl.music.media

import android.os.Bundle
import androidx.media3.session.SessionCommand
import com.howl.music.bridge.AnalysisSnapshot

object PlaybackAnalysis {
    val read = SessionCommand("com.howl.music.analysis.READ", Bundle.EMPTY)

    const val PEAK = "peak"
    const val RMS = "rms"
    const val DIAGNOSTICS_ONLY = "diagnosticsOnly"
    const val GAPLESS = "gapless"
    const val UNDERRUNS = "underruns"
    const val CALLBACKS = "callbacks"

    fun snapshot(extras: Bundle) = AnalysisSnapshot(extras.getFloat(PEAK), extras.getFloat(RMS))
}
