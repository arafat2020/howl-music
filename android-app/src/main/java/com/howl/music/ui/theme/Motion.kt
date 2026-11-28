package com.howl.music.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.staticCompositionLocalOf

internal enum class MotionMode { Full, Reduced, Off }

internal val LocalMotionMode = staticCompositionLocalOf { MotionMode.Full }

internal object HowlMotion {
    const val PressedScale = 0.97f
    const val PressedAlpha = 0.82f

    fun <T> settle() = spring<T>(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
    fun press() = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    fun <T> fade() = tween<T>(durationMillis = 150)
    fun <T> reducedFade() = tween<T>(durationMillis = 90)
}
