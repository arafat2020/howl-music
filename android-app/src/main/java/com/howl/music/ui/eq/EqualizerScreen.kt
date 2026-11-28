package com.howl.music.ui.eq

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.howl.music.bridge.EqState
import com.howl.music.media.PlaybackEqualizer
import com.howl.music.ui.components.HowlButton
import com.howl.music.ui.components.HowlSlider
import com.howl.music.ui.theme.HowlSpacing
import kotlin.math.roundToInt

private val frequencyLabels = listOf(
    "31.25 Hz", "62.5 Hz", "125 Hz", "250 Hz", "500 Hz",
    "1 kHz", "2 kHz", "4 kHz", "8 kHz", "16 kHz",
)

@UnstableApi
@Composable
internal fun EqualizerScreen(model: EqualizerViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val gains = settings.gains
    val bands = remember(gains) { gains.bandsDb }
    var showPresets by rememberSaveable { mutableStateOf(false) }
    var showSave by rememberSaveable { mutableStateOf(false) }
    var presetName by rememberSaveable { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(HowlSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(HowlSpacing.Medium),
    ) {
        item {
            Text("Equalizer", style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.semantics { heading() })
        }
        item {
            val status = when {
                !state.connected -> "Playback equalizer unavailable"
                state.applying -> "Applying changes"
                settings.enabled -> "EQ enabled"
                else -> "EQ bypassed"
            }
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = HowlSpacing.TouchTarget)
                    .toggleable(
                        value = settings.enabled,
                        enabled = state.connected,
                        role = Role.Switch,
                        onValueChange = model::setEnabled,
                    ),
                horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(status, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Switch(checked = settings.enabled, onCheckedChange = null, enabled = state.connected)
            }
            state.error?.let { Text(it, color = colors.error) }
        }
        item { GainGuide(settings, bands) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Small)) {
                HowlButton("Presets", { showPresets = true }, Modifier.weight(1f), enabled = state.connected)
                HowlButton("Save preset", { showSave = true }, Modifier.weight(1f),
                    enabled = state.connected && !state.saving, primary = false)
            }
        }
        item {
            GainSlider("Preamp", gains.preampDb, -24f..12f, state.connected) { model.setGain(null, it) }
        }
        items(frequencyLabels.size, key = { frequencyLabels[it] }) { index ->
            GainSlider(frequencyLabels[index], bands[index], -12f..12f, state.connected) {
                model.setGain(index, it)
            }
        }
    }
    if (showPresets) {
        val presets by model.presets.collectAsStateWithLifecycle(initialValue = emptyList())
        AlertDialog(
            onDismissRequest = { showPresets = false },
            title = { Text("Presets") },
            text = {
                LazyColumn(Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(HowlSpacing.Small)) {
                    item {
                        HowlButton("Flat", {
                            model.applyPreset(EqState())
                            showPresets = false
                        }, Modifier.fillMaxWidth(), enabled = state.connected, primary = false)
                    }
                    items(presets, key = { it.id }) { preset ->
                        HowlButton(preset.name, {
                            model.applyPreset(preset.state)
                            showPresets = false
                        }, Modifier.fillMaxWidth(), enabled = state.connected, primary = false)
                    }
                }
            },
            confirmButton = { HowlButton("Close", { showPresets = false }, primary = false) },
        )
    }
    if (showSave) {
        AlertDialog(
            onDismissRequest = { if (!state.saving) showSave = false },
            title = { Text("Save preset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(HowlSpacing.Small)) {
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = { presetName = it },
                        label = { Text("Preset name") },
                        enabled = !state.saving,
                        singleLine = true,
                    )
                    state.error?.let { Text(it, color = colors.error) }
                }
            },
            confirmButton = {
                HowlButton("Save", {
                    model.savePreset(presetName) {
                        presetName = ""
                        showSave = false
                    }
                }, enabled = state.connected && !state.saving && presetName.isNotBlank())
            },
            dismissButton = { HowlButton("Cancel", { showSave = false }, enabled = !state.saving, primary = false) },
        )
    }
}

@Composable
private fun GainSlider(label: String, gain: Float, range: ClosedFloatingPointRange<Float>,
                       enabled: Boolean, onCommit: (Float) -> Unit) {
    var preview by remember(gain, enabled) { mutableStateOf<Float?>(null) }
    val value = preview ?: gain
    val displayGain = (value * 2f).roundToInt() / 2f
    val number = if (displayGain % 1f == 0f) displayGain.toInt().toString() else displayGain.toString()
    val formatted = if (displayGain > 0f) "+$number dB" else "$number dB"
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(formatted, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HowlSlider(
            label = "$label gain",
            value = value,
            onValueChange = { preview = it },
            modifier = Modifier.fillMaxWidth().semantics { stateDescription = formatted },
            enabled = enabled,
            range = range,
            steps = ((range.endInclusive - range.start) * 2).toInt() - 1,
            onValueChangeFinished = {
                preview?.let(onCommit)
                preview = null
            },
        )
    }
}

@Composable
private fun GainGuide(settings: PlaybackEqualizer.Settings, bands: FloatArray) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(HowlSpacing.Small)) {
        Text("Band gain guide", style = MaterialTheme.typography.titleSmall)
        Text("Preamp + band settings · dB", style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant)
        Canvas(Modifier.fillMaxWidth().height(88.dp).padding(horizontal = HowlSpacing.Small)) {
            val zero = size.height / 2
            val inset = 4.dp.toPx()
            val path = Path()
            drawLine(colors.outline, Offset(0f, zero), Offset(size.width, zero), 1.dp.toPx())
            bands.forEachIndexed { index, band ->
                val gain = if (settings.enabled) settings.gains.preampDb + band else 0f
                val x = inset + (size.width - 2 * inset) * index / bands.lastIndex
                val y = zero - gain / 36f * (zero - inset)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, if (settings.enabled) colors.primary else colors.outline,
                style = Stroke(2.dp.toPx()))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(frequencyLabels.first(), style = MaterialTheme.typography.labelSmall)
            Text(frequencyLabels.last(), style = MaterialTheme.typography.labelSmall)
        }
    }
}
