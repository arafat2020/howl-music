package com.howl.music.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.howl.music.data.prefs.observeSystemMotion
import com.howl.music.data.prefs.readSystemMotion

@Composable
fun HowlTheme(
    dark: Boolean = true,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val resolver = LocalContext.current.contentResolver
    val systemMotion by remember(resolver) { observeSystemMotion(resolver) }
        .collectAsStateWithLifecycle(initialValue = remember(resolver) { readSystemMotion(resolver) })
    val motion = when {
        !systemMotion -> MotionMode.Off
        reduceMotion -> MotionMode.Reduced
        else -> MotionMode.Full
    }
    MaterialTheme(
        colorScheme = if (dark) howlDarkColors else howlLightColors,
        typography = HowlTypography,
        shapes = HowlShapes,
    ) {
        CompositionLocalProvider(LocalMotionMode provides motion) {
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
