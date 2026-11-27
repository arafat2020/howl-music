package com.howl.music.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.howl.music.R

private val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_bold, FontWeight.Bold),
)

private fun barlow(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Barlow, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

internal val HowlTypography = Typography(
    displayLarge = barlow(56, 60, FontWeight.Bold),
    displayMedium = barlow(44, 48, FontWeight.Bold),
    displaySmall = barlow(36, 40, FontWeight.Bold),
    headlineLarge = barlow(32, 36, FontWeight.Bold),
    headlineMedium = barlow(28, 32, FontWeight.SemiBold),
    headlineSmall = barlow(24, 28, FontWeight.SemiBold),
    titleLarge = barlow(22, 26, FontWeight.SemiBold),
    titleMedium = barlow(18, 23, FontWeight.SemiBold),
    titleSmall = barlow(16, 20, FontWeight.SemiBold),
    bodyLarge = barlow(17, 24),
    bodyMedium = barlow(15, 21),
    bodySmall = barlow(13, 18),
    labelLarge = barlow(15, 20, FontWeight.SemiBold),
    labelMedium = barlow(13, 17, FontWeight.Medium),
    labelSmall = barlow(12, 15, FontWeight.Medium),
)
