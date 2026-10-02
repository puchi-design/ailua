package com.example.ui.systemui

enum class SystemUiSurface { NONE, LOCKSCREEN, NOTIFICATION_SHADE, CONTROL_CENTER }

/** Surface navigation belongs to the virtual OS, independently of the app back stack. */
data class VirtualSystemUiState(
    val surface: SystemUiSurface = SystemUiSurface.NONE,
    val isLocked: Boolean = false,
    val shadeExpansion: Float = 0f,
    val controlExpansion: Float = 0f,
)
