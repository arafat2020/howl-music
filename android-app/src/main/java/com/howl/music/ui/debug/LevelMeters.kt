package com.howl.music.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.howl.music.bridge.AnalysisSnapshot
import com.howl.music.ui.theme.HowlSpacing
import kotlin.math.log10
import kotlin.math.roundToInt

@Composable
internal fun LevelMeters(readLevels: () -> AnalysisSnapshot, modifier: Modifier = Modifier) {
    val snapshot = readLevels()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HowlSpacing.Small)) {
        LevelMeter("Peak", snapshot.peak)
        LevelMeter("RMS", snapshot.rms)
    }
}

@Composable
private fun LevelMeter(label: String, amplitude: Float) {
    val db = if (amplitude > 0f) 20f * log10(amplitude) else Float.NEGATIVE_INFINITY
    val value = if (db < -60f) "< −60 dBFS" else "${db.roundToInt()} dBFS"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
    LinearProgressIndicator(
        progress = { ((db + 60f) / 60f).coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$label level" },
    )
}
