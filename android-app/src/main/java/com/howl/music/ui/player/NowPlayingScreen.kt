package com.howl.music.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.howl.music.ui.components.AlbumArt
import com.howl.music.ui.components.HowlButton
import com.howl.music.ui.components.HowlSlider
import com.howl.music.ui.theme.HowlSpacing
import com.howl.music.ui.theme.HowlMotion
import com.howl.music.ui.theme.LocalMotionMode
import com.howl.music.ui.theme.MotionMode
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

@Composable
internal fun NowPlayingScreen(
    playback: PlaybackUiState,
    readPositionMs: () -> Long,
    onToggle: () -> Unit,
    onSeek: (String, Long) -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onBack: () -> Unit,
    onQueue: () -> Unit,
    onVisualizer: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val artworkLimit = (maxHeight * 0.43f).coerceIn(128.dp, 360.dp)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(HowlSpacing.Screen),
            verticalArrangement = Arrangement.spacedBy(HowlSpacing.Large),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HowlButton("Back", onBack, primary = false)
                Text(
                    "Now Playing",
                    style = typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                HowlButton("Queue", onQueue, primary = false)
            }
            NowPlayingArtwork(playback.artworkTrackUri, artworkLimit)
            Column(verticalArrangement = Arrangement.spacedBy(HowlSpacing.ExtraSmall)) {
                Text(
                    playback.title ?: "Nothing playing",
                    style = typography.displayMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                playback.artist?.takeIf { it.isNotBlank() }?.let { artist ->
                    Text(artist, color = colors.onSurfaceVariant, style = typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (!playback.isPlaying) {
                    Text(playback.status, color = colors.onSurfaceVariant, style = typography.bodySmall)
                }
            }
            PlaybackProgress(playback, readPositionMs, onSeek)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HowlButton(if (playback.hasPreviousTrack) "Previous" else "−5 s", onSkipBack,
                    modifier = Modifier.weight(1f).semantics { contentDescription = if (playback.hasPreviousTrack) "Previous track" else "Skip back 5 seconds" },
                    enabled = playback.canSkipBack, primary = false)
                HowlButton(if (playback.playRequested) "Pause" else "Play", onToggle,
                    modifier = Modifier.weight(1.15f), enabled = playback.canToggle)
                HowlButton(if (playback.hasNextTrack) "Next" else "+15 s", onSkipForward,
                    modifier = Modifier.weight(1f).semantics { contentDescription = if (playback.hasNextTrack) "Next track" else "Skip forward 15 seconds" },
                    enabled = playback.canSkipForward, primary = false)
            }
            HowlButton("Visualizer", onVisualizer, Modifier.fillMaxWidth(), primary = false)
        }
    }
}

@Composable
private fun NowPlayingArtwork(uri: String?, sizeLimit: Dp) {
    val reduceMotion = LocalMotionMode.current != MotionMode.Full
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val artworkSize = maxWidth.coerceAtMost(sizeLimit)
        var drag by remember(uri, reduceMotion) { mutableFloatStateOf(0f) }
        var dragging by remember(uri, reduceMotion) { mutableStateOf(false) }
        val motionOffset = remember(uri, reduceMotion) { Animatable(0f) }
        LaunchedEffect(dragging, motionOffset) {
            if (!dragging) {
                motionOffset.snapTo(drag)
                motionOffset.animateTo(0f, HowlMotion.settle())
                drag = 0f
            }
        }
        val motion = if (reduceMotion || uri == null) Modifier else Modifier
            .pointerInput(uri) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        drag = motionOffset.value
                        dragging = true
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, delta ->
                    if (dragging) {
                        change.consume()
                        drag = (drag + delta / size.width.coerceAtLeast(1)).coerceIn(-0.12f, 0.12f)
                    }
                }
            }
            .graphicsLayer {
                val offset = if (dragging) drag else motionOffset.value
                translationX = offset * size.width * 0.24f
                rotationY = offset * 48f
                cameraDistance = 16 * density
            }
        Box(motion.clearAndSetSemantics {}) {
            if (reduceMotion) {
                AlbumArt(uri, artworkSize, Modifier.clip(MaterialTheme.shapes.large))
            } else {
                Crossfade(uri, animationSpec = HowlMotion.fade(), label = "Artwork") { trackUri ->
                    AlbumArt(trackUri, artworkSize, Modifier.clip(MaterialTheme.shapes.large))
                }
            }
        }
    }
}

@Composable
private fun PlaybackProgress(
    playback: PlaybackUiState,
    readPositionMs: () -> Long,
    onSeek: (String, Long) -> Unit,
) {
    val owner = LocalLifecycleOwner.current
    val readPosition by rememberUpdatedState(readPositionMs)
    val position by produceState(playback.positionMs, playback, owner) {
        value = playback.positionMs
        if (playback.isPlaying) {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    value = readPosition()
                    delay(250)
                }
            }
        }
    }
    val duration = playback.durationMs
    val mediaId = playback.mediaId
    val canSeek = mediaId != null && playback.canSeek && duration > 0
    var scrubPosition by remember(mediaId, duration, canSeek) { mutableStateOf<Long?>(null) }
    val shownPosition = (scrubPosition ?: position).coerceIn(0L, duration)
    val elapsed = playbackTime(shownPosition)
    val total = if (duration > 0) playbackTime(duration) else "–:––"

    Column {
        HowlSlider(
            label = "Playback position",
            value = if (duration > 0) (shownPosition.toDouble() / duration).toFloat() else 0f,
            enabled = canSeek,
            modifier = Modifier.fillMaxWidth().semantics {
                stateDescription = if (duration > 0) "$elapsed of $total" else "Duration unavailable"
            },
            onValueChange = { fraction -> scrubPosition = (fraction.toDouble() * duration).roundToLong() },
            onValueChangeFinished = {
                val target = scrubPosition
                scrubPosition = null
                if (canSeek && target != null) onSeek(mediaId, target)
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
            val color = MaterialTheme.colorScheme.onSurfaceVariant
            Text(elapsed, style = style, color = color)
            Text(total, style = style, color = color)
        }
    }
}

private fun playbackTime(positionMs: Long): String {
    val seconds = positionMs / 1_000
    val minutes = seconds / 60
    val remainder = (seconds % 60).toString().padStart(2, '0')
    return if (minutes < 60) "$minutes:$remainder"
    else "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}:$remainder"
}
