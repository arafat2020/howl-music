package com.howl.music.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.howl.music.ui.theme.HowlSpacing

@Composable
fun HowlButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
) {
    val control = modifier.heightIn(min = HowlSpacing.TouchTarget)
    if (primary) {
        Button(onClick, control, enabled, shape = MaterialTheme.shapes.medium) { Text(label) }
    } else {
        OutlinedButton(onClick, control, enabled, shape = MaterialTheme.shapes.medium) { Text(label) }
    }
}

@Composable
fun HowlIconButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.sizeIn(minWidth = HowlSpacing.TouchTarget, minHeight = HowlSpacing.TouchTarget)
            .semantics { contentDescription = label },
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        content = icon,
    )
}

@Composable
fun HowlSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.heightIn(min = HowlSpacing.TouchTarget).semantics { contentDescription = label },
        enabled = enabled,
        valueRange = range,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
    )
}
