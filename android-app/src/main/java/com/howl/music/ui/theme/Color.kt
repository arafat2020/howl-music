package com.howl.music.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal object HowlPalette {
    val Canvas = Color(0xFF101014)
    val Lime = Color(0xFFB7F34A)
    val Purple = Color(0xFFB65CFF)
    val Orange = Color(0xFFFF6B47)
    val Neutral = Color(0xFFE9E5DC)
}

internal val howlDarkColors = darkColorScheme(
    primary = HowlPalette.Lime,
    onPrimary = HowlPalette.Canvas,
    primaryContainer = Color(0xFF293819),
    onPrimaryContainer = Color(0xFFDAFFA2),
    secondary = HowlPalette.Purple,
    onSecondary = HowlPalette.Canvas,
    secondaryContainer = Color(0xFF351E47),
    onSecondaryContainer = Color(0xFFF0D6FF),
    tertiary = HowlPalette.Orange,
    onTertiary = HowlPalette.Canvas,
    tertiaryContainer = Color(0xFF51281E),
    onTertiaryContainer = Color(0xFFFFDACE),
    background = HowlPalette.Canvas,
    onBackground = HowlPalette.Neutral,
    surface = Color(0xFF19191F),
    onSurface = HowlPalette.Neutral,
    surfaceVariant = Color(0xFF292932),
    onSurfaceVariant = Color(0xFFBEBBC4),
    surfaceContainer = Color(0xFF202027),
    surfaceContainerHigh = Color(0xFF292932),
    outline = Color(0xFF85828C),
    outlineVariant = Color(0xFF44434D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF4E1110),
)

internal val howlLightColors = lightColorScheme(
    primary = Color(0xFF395500),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9F5A5),
    onPrimaryContainer = Color(0xFF182900),
    secondary = Color(0xFF7026A6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1DBFF),
    onSecondaryContainer = Color(0xFF3F105E),
    tertiary = Color(0xFFA6351A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBD0),
    onTertiaryContainer = Color(0xFF491408),
    background = Color(0xFFF9F7F2),
    onBackground = Color(0xFF1B1A20),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF1B1A20),
    surfaceVariant = Color(0xFFE9E5EC),
    onSurfaceVariant = Color(0xFF4C4854),
    surfaceContainer = Color(0xFFF1EDF1),
    surfaceContainerHigh = Color(0xFFE9E5EC),
    outline = Color(0xFF756F7C),
    outlineVariant = Color(0xFFCCC6CF),
    error = Color(0xFFAB241D),
    onError = Color.White,
)
