package com.howl.music.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun HowlTheme(
    dark: Boolean = true,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (dark) howlDarkColors else howlLightColors,
        typography = HowlTypography,
        shapes = HowlShapes,
    ) {
        CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ) {
                Box(Modifier.safeDrawingPadding()) { content() }
            }
        }
    }
}
