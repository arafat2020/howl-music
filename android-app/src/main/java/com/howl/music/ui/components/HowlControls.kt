package com.howl.music.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.howl.music.ui.theme.HowlMotion
import com.howl.music.ui.theme.HowlSpacing
import com.howl.music.ui.theme.LocalMotionMode
import com.howl.music.ui.theme.MotionMode

@Composable
fun HowlButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
) {
    val control = modifier.heightIn(min = HowlSpacing.TouchTarget)
    val interaction = remember { MutableInteractionSource() }
    if (primary) {
        Button(onClick, control, enabled, shape = MaterialTheme.shapes.medium, interactionSource = interaction) {
            PressFeedback(interaction) { Text(label) }
        }
    } else {
        OutlinedButton(onClick, control, enabled, shape = MaterialTheme.shapes.medium, interactionSource = interaction) {
            PressFeedback(interaction) { Text(label) }
        }
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
    val interaction = remember { MutableInteractionSource() }
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.sizeIn(minWidth = HowlSpacing.TouchTarget, minHeight = HowlSpacing.TouchTarget)
            .semantics { contentDescription = label },
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        interactionSource = interaction,
    ) {
        PressFeedback(interaction, icon)
    }
}

@Composable
private fun PressFeedback(interaction: MutableInteractionSource, content: @Composable () -> Unit) {
    val pressed by interaction.collectIsPressedAsState()
    val feedback = if (LocalMotionMode.current == MotionMode.Full) {
        val scale by animateFloatAsState(
            if (pressed) HowlMotion.PressedScale else 1f, HowlMotion.press(), label = "Button press",
        )
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    } else if (pressed) {
        Modifier.graphicsLayer { alpha = HowlMotion.PressedAlpha }
    } else {
        Modifier
    }
    Box(feedback) { content() }
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
