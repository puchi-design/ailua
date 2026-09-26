package com.example.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

object AppMotion {
    const val MIN_DURATION_MS = 220
    const val MAX_DURATION_MS = 320

    const val OPEN_DURATION_MS = 300
    const val CLOSE_DURATION_MS = 300

    const val OPEN_SCALE = 0.96f
    const val CLOSE_SCALE = 0.96f

    val EASING = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun enterSpec() = tween<Float>(durationMillis = OPEN_DURATION_MS, easing = EASING)

    fun exitSpec() = tween<Float>(durationMillis = CLOSE_DURATION_MS, easing = EASING)
}
