package com.example.ui.systemui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CallSession
import com.example.data.model.CallState
import com.example.navigation.AiluaDestinations
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.systemui.lockscreen.VirtualLockScreen

/** A single overlay owner above every app route. App navigation stays underneath. */
@Composable
fun VirtualSystemUiHost(
    currentCall: CallSession?,
    currentRoute: String?,
    isDarkTheme: Boolean,
    onToggleDarkMode: () -> Unit,
    onLaunchRoute: (String) -> Unit,
    controller: VirtualSystemUiController = VirtualSystemUiSession.controller,
    content: @Composable () -> Unit,
) {
    val state by controller.state.collectAsStateWithLifecycle()
    val priorityCallId by controller.priorityCallId.collectAsStateWithLifecycle()
    val incoming = currentCall?.state == CallState.INCOMING
    LaunchedEffect(currentCall?.id, incoming) {
        controller.updatePriorityCall(currentCall?.id, incoming)
    }
    // Answering a call keeps the existing call screen usable while preserving the lock
    // underneath. A manual lock clears this exception; ending the call restores it.
    val priorityCallVisible = incoming || (priorityCallId != null &&
        priorityCallId == currentCall?.id && currentRoute == "call/{characterId}")
    val showLock = state.isLocked && !priorityCallVisible
    BackHandler(enabled = showLock) { controller.closeSystemSurface() }

    fun launchUnlocked(route: String) {
        controller.unlock()
        onLaunchRoute(route)
    }

    VirtualSystemUiProvider(controller) {
        Box(Modifier.fillMaxSize()) {
            content()
            AnimatedVisibility(
                visible = showLock,
                enter = fadeIn(),
                exit = fadeOut() + slideOutVertically { -it },
            ) {
                VirtualLockScreen(
                    onUnlock = controller::unlock,
                    onOpenCommunications = { launchUnlocked(AiluaDestinations.MESSAGES) },
                    onOpenGallery = { launchUnlocked(AiluaDestinations.GALLERY) },
                    statusBar = { VirtualPhoneStatusBar(isDarkTheme = isDarkTheme, onToggleTheme = onToggleDarkMode) },
                )
            }
        }
    }
}
