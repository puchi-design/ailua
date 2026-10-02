package com.example.ui.systemui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class VirtualSystemUiController {
    private val mutable = MutableStateFlow(VirtualSystemUiState())
    val state = mutable.asStateFlow()
    private val priorityCall = MutableStateFlow<String?>(null)
    val priorityCallId = priorityCall.asStateFlow()
    private var coldStartHandled = false

    /** Called once per process, including when the first launch is still onboarding. */
    @Synchronized
    fun initializeColdStart(onboardingComplete: Boolean, lockOnColdStart: Boolean) {
        if (coldStartHandled) return
        coldStartHandled = true
        if (onboardingComplete && lockOnColdStart) lock()
    }

    fun lock() {
        priorityCall.value = null
        mutable.value = VirtualSystemUiState(SystemUiSurface.LOCKSCREEN, isLocked = true)
    }

    fun unlock() {
        mutable.value = VirtualSystemUiState()
    }

    fun updatePriorityCall(id: String?, incoming: Boolean) {
        if (id == null || (priorityCall.value != null && priorityCall.value != id)) priorityCall.value = null
        if (incoming) priorityCall.value = id
    }

    fun openNotifications() {
        mutable.value = mutable.value.copy(
            surface = SystemUiSurface.NOTIFICATION_SHADE, shadeExpansion = 1f, controlExpansion = 0f
        )
    }

    fun openControlCenter() {
        mutable.value = mutable.value.copy(
            surface = SystemUiSurface.CONTROL_CENTER, shadeExpansion = 0f, controlExpansion = 1f
        )
    }

    fun closeSystemSurface() {
        mutable.value = VirtualSystemUiState(
            surface = if (mutable.value.isLocked) SystemUiSurface.LOCKSCREEN else SystemUiSurface.NONE,
            isLocked = mutable.value.isLocked
        )
    }

    fun toggleNotifications() {
        if (mutable.value.surface == SystemUiSurface.NOTIFICATION_SHADE) closeSystemSurface()
        else openNotifications()
    }

    fun toggleControlCenter() {
        if (mutable.value.surface == SystemUiSurface.CONTROL_CENTER) closeSystemSurface()
        else openControlCenter()
    }
}

/** Survives Activity recreation and SAF round trips; a new process starts a new session. */
object VirtualSystemUiSession {
    val controller = VirtualSystemUiController()
}
