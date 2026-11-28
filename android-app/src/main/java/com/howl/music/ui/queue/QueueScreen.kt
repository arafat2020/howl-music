package com.howl.music.ui.queue

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.howl.music.media.PlaybackQueue.Action
import com.howl.music.ui.components.AlbumArt
import com.howl.music.ui.components.HowlButton
import com.howl.music.ui.theme.HowlSpacing

internal data class QueueTrack(val id: String, val title: String, val artist: String?, val uri: String?)

internal data class QueueUiState(
    val current: QueueTrack? = null,
    val upcoming: List<QueueTrack> = emptyList(),
    val canEdit: Boolean = false,
)

@Composable
internal fun QueueScreen(state: QueueUiState, onBack: () -> Unit, onEdit: (Action, String, String) -> Unit) {
    val list = rememberLazyListState()
    val latest by rememberUpdatedState(state)
    val edit by rememberUpdatedState(onEdit)
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    fun finishDrag(commit: Boolean) {
        val id = draggedId
        val visible = list.layoutInfo.visibleItemsInfo
        val source = visible.firstOrNull { it.key == id }
        val upcomingIds = latest.upcoming.map { it.id }.toSet()
        val target = source?.let {
            val center = it.offset + it.size / 2 + dragOffset
            visible.firstOrNull { row ->
                row.key in upcomingIds && center >= row.offset && center <= row.offset + row.size
            }?.key as? String
        }
        if (commit && id != null && target != null && target != id) edit(Action.MoveTo, id, target)
        draggedId = null
        dragOffset = 0f
    }

    LaunchedEffect(state.upcoming, state.canEdit) {
        if (!state.canEdit || state.upcoming.none { it.id == draggedId }) finishDrag(false)
    }

    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(HowlSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(HowlSpacing.Medium),
    ) {
        item(key = "heading") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Large),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HowlButton("Back", onBack, primary = false)
                Text("Queue", style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.semantics { heading() })
            }
        }
        state.current?.let { track ->
            item(key = "current") {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                    QueueRow(track, null, state.canEdit, onAction = { onEdit(it, track.id, "") })
                }
            }
        }
        item(key = "upcoming") {
            Text("Up next", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        }
        if (state.upcoming.isEmpty()) {
            item(key = "empty") { Text("No upcoming tracks", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        itemsIndexed(state.upcoming, key = { _, track -> track.id }) { index, track ->
            val dragging = draggedId == track.id
            Surface(
                modifier = Modifier.zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationY = if (dragging) dragOffset else 0f },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                QueueRow(
                    track = track,
                    position = index + 1,
                    enabled = state.canEdit,
                    canMoveEarlier = index > 0,
                    canMoveLater = index < state.upcoming.lastIndex,
                    onAction = { onEdit(it, track.id, "") },
                    dragHandle = if (state.canEdit && state.upcoming.size > 1) {
                        {
                            Box(
                                Modifier.sizeIn(minWidth = HowlSpacing.TouchTarget, minHeight = HowlSpacing.TouchTarget)
                                    .pointerInput(track.id) {
                                        detectVerticalDragGestures(
                                            onDragStart = { draggedId = track.id; dragOffset = 0f },
                                            onDragEnd = { finishDrag(true) },
                                            onDragCancel = { finishDrag(false) },
                                        ) { change, amount ->
                                            change.consume()
                                            dragOffset += amount
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("≡", style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun QueueRow(
    track: QueueTrack,
    position: Int?,
    enabled: Boolean,
    onAction: (Action) -> Unit,
    canMoveEarlier: Boolean = false,
    canMoveLater: Boolean = false,
    dragHandle: (@Composable () -> Unit)? = null,
) {
    var showMenu by remember(track.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(track.uri, 48.dp)
        Column(Modifier.weight(1f)) {
            Text(position?.toString() ?: "Current track", style = MaterialTheme.typography.labelMedium)
            Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            track.artist?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        dragHandle?.invoke()
        Box {
            HowlButton(
                "Actions", { showMenu = true },
                modifier = Modifier.semantics { contentDescription = "Actions for ${track.title}" },
                enabled = enabled,
                primary = false,
            )
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                val actions = buildList {
                    if (position != null) add("Play now" to Action.Play)
                    if (canMoveEarlier) {
                        add("Play next" to Action.MoveNext)
                        add("Move up" to Action.MoveEarlier)
                    }
                    if (canMoveLater) add("Move down" to Action.MoveLater)
                    add("Remove" to Action.Remove)
                }
                actions.forEach { (label, action) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = {
                        showMenu = false
                        onAction(action)
                    })
                }
            }
        }
    }
}
