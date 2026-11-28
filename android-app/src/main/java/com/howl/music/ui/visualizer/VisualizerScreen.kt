package com.howl.music.ui.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.howl.music.bridge.AnalysisSnapshot
import com.howl.music.ui.components.HowlButton
import com.howl.music.ui.player.PlaybackUiState
import com.howl.music.ui.theme.HowlSpacing
import com.howl.music.ui.theme.LocalMotionMode
import com.howl.music.ui.theme.MotionMode
import kotlinx.coroutines.delay
import kotlin.math.log10
import kotlin.math.roundToInt

private enum class Style { Pulse, Levels }

private data class VisualizerLevels(val peak: Float = 0f, val rms: Float = 0f)

@Composable
internal fun VisualizerScreen(
    playback: PlaybackUiState,
    readAnalysis: suspend () -> AnalysisSnapshot,
    onToggle: () -> Unit,
    onBack: () -> Unit,
) {
    var style by rememberSaveable { mutableStateOf(Style.Pulse) }
    val reducedMotion = LocalMotionMode.current != MotionMode.Full
    val owner = LocalLifecycleOwner.current
    val read by rememberUpdatedState(readAnalysis)
    val animate = playback.isPlaying && !reducedMotion
    val levels = produceState(VisualizerLevels(), animate, playback.mediaId, owner) {
        value = VisualizerLevels()
        if (animate) {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var peak = 0f
                var rms = 0f
                try {
                    while (true) {
                        val sample = read()
                        peak = smooth(peak, sample.peak)
                        rms = smooth(rms, sample.rms)
                        value = VisualizerLevels(displayLevel(peak), displayLevel(rms))
                        delay(50)
                    }
                } finally {
                    value = VisualizerLevels()
                }
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val visualHeight = (maxHeight * 0.48f).coerceIn(160.dp, 480.dp)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(HowlSpacing.Screen),
            verticalArrangement = Arrangement.spacedBy(HowlSpacing.Large),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Large),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HowlButton("Back", onBack, primary = false)
                Text("Visualizer", style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() })
            }
            LevelCanvas(style, reducedMotion, { levels.value }, Modifier.fillMaxWidth().height(visualHeight))
            Column(verticalArrangement = Arrangement.spacedBy(HowlSpacing.ExtraSmall)) {
                Text(
                    playback.title ?: "Nothing playing",
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                playback.artist?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (reducedMotion) Text("Reduced motion", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Style.entries.forEach { choice ->
                    FilterChip(
                        selected = style == choice,
                        onClick = { style = choice },
                        label = { Text(choice.name) },
                    )
                }
                HowlButton(
                    if (playback.playRequested) "Pause" else "Play",
                    onToggle,
                    Modifier.weight(1f),
                    enabled = playback.canToggle,
                )
            }
        }
    }
}

@Composable
private fun LevelCanvas(
    style: Style,
    reducedMotion: Boolean,
    readLevels: () -> VisualizerLevels,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier.graphicsLayer()) {
        val levels = if (reducedMotion) VisualizerLevels(0.58f, 0.35f) else readLevels()
        val peak = levels.peak
        val rms = levels.rms
        val radius = size.minDimension / 2
        val corner = CornerRadius(24.dp.toPx())
        drawRoundRect(color = colors.surfaceContainerLow, cornerRadius = corner)
        when (style) {
            Style.Pulse -> {
                drawCircle(colors.outlineVariant, radius * 0.82f, style = Stroke(1.dp.toPx()))
                drawCircle(colors.secondary.copy(alpha = 0.28f), radius * (0.36f + peak * 0.49f),
                    style = Stroke(3.dp.toPx()))
                drawCircle(colors.primary.copy(alpha = 0.14f + rms * 0.24f), radius * (0.19f + rms * 0.45f))
                drawCircle(colors.primary, radius * (0.19f + rms * 0.45f), style = Stroke(2.dp.toPx()))
            }
            Style.Levels -> {
                val width = size.width * 0.7f
                val height = radius * 0.13f
                val left = (size.width - width) / 2
                val shape = CornerRadius(height / 2)
                drawRoundRect(colors.outlineVariant, Offset(left, center.y - height * 1.55f),
                    Size(width, height), shape)
                drawRoundRect(colors.outlineVariant, Offset(left, center.y + height * 0.55f),
                    Size(width, height), shape)
                drawRoundRect(colors.primary, Offset(left, center.y - height * 1.55f),
                    Size(width * peak, height), shape)
                drawRoundRect(colors.secondary, Offset(left, center.y + height * 0.55f),
                    Size(width * rms, height), shape)
            }
        }
    }
}

private fun smooth(previous: Float, next: Float): Float =
    previous + (next - previous) * if (next > previous) 0.55f else 0.2f

private fun displayLevel(level: Float): Float {
    if (level <= 0f) return 0f
    val fraction = ((20f * log10(level) + 48f) / 48f).coerceIn(0f, 1f)
    // Skip redraws when a level changes by less than a display step.
    return (fraction * 64f).roundToInt() / 64f
}
