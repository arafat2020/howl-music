package com.howl.music.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.howl.music.ui.components.AlbumArt
import com.howl.music.ui.components.HowlButton
import com.howl.music.ui.theme.HowlSpacing

internal data class PlaybackUiState(
    val mediaId: String? = null,
    val artist: String? = null,
    val durationMs: Long = 0,
    val hasPreviousTrack: Boolean = false,
    val hasNextTrack: Boolean = false,
    val canSkipBack: Boolean = false,
    val canSkipForward: Boolean = false,
    val title: String? = null,
    val artworkTrackUri: String? = null,
    val status: String = "Connecting to playback service",
    val playRequested: Boolean = false,
    val canToggle: Boolean = false,
    val canSeek: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
)

@Composable
internal fun MiniPlayer(
    playback: PlaybackUiState,
    onToggle: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = HowlSpacing.Medium, vertical = HowlSpacing.ExtraSmall),
            horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).heightIn(min = HowlSpacing.TouchTarget)
                    .clickable(role = Role.Button, onClickLabel = "Open Now Playing", onClick = onExpand),
                horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArt(playback.artworkTrackUri, 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        playback.title ?: "Nothing playing",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        playback.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text("›", style = MaterialTheme.typography.headlineSmall, color = colors.onSurfaceVariant)
            }
            HowlButton(
                label = if (playback.playRequested) "Pause" else "Play",
                onClick = onToggle,
                enabled = playback.canToggle,
            )
        }
    }
}
