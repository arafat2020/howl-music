package com.howl.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.howl.music.ui.theme.HowlSpacing
import com.howl.music.ui.theme.HowlMotion
import com.howl.music.ui.theme.LocalMotionMode
import com.howl.music.ui.theme.MotionMode

@Composable
fun HowlListRow(
    title: String,
    metadata: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer else colors.surfaceContainer,
        animationSpec = if (LocalMotionMode.current == MotionMode.Full) HowlMotion.fade() else snap(),
        label = "Row selection",
    )
    val foreground = if (selected) colors.onPrimaryContainer else colors.onSurface
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = HowlSpacing.TouchTarget)
            .semantics { this.selected = selected },
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = background,
        contentColor = foreground,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = HowlSpacing.Large, vertical = HowlSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    metadata,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) foreground else colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Text("Selected", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = HowlSpacing.Small))
            }
        }
    }
}
