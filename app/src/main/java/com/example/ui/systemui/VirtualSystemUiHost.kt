package com.example.ui.systemui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.data.model.CallSession
import com.example.data.model.CallState
import com.example.data.model.CallAction
import com.example.data.engine.CallStateEngine
import com.example.data.systemui.live.LiveActivityCenter
import com.example.data.systemui.notification.VirtualNotification
import com.example.data.systemui.notification.VirtualNotificationGraph
import com.example.data.systemui.notification.NotificationHeadsUpPolicy
import com.example.data.systemui.control.ControlCenterStore
import com.example.navigation.AiluaDestinations
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.themeengine.LocalAiluaTheme
import com.example.ui.systemui.lockscreen.VirtualLockScreen
import com.example.ui.systemui.lockscreen.LockScreenNotificationItem
import com.example.ui.systemui.lockscreen.LockScreenNotificationPreview
import com.example.ui.systemui.notification.NotificationShade
import com.example.ui.systemui.notification.HeadsUpNotification
import com.example.ui.designsystem.publicNotificationTitle
import com.example.ui.systemui.control.ControlCenter
import com.example.ui.systemui.control.QuickControlsRow
import com.example.ui.systemui.live.LiveActivityChip
import com.example.ui.systemui.live.LiveActivityHost
import com.example.ui.systemui.live.LiveActivitySummary
import kotlinx.coroutines.launch

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
    val repository = VirtualNotificationGraph.repository
    val notificationFlow = remember(repository) { repository.observeActive() }
    val notifications by notificationFlow.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val controls by ControlCenterStore.state.collectAsStateWithLifecycle()
    val activities by LiveActivityCenter.activities.collectAsStateWithLifecycle()
    var liveExpanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var windowHasFocus by remember(view) { mutableStateOf(view.hasWindowFocus()) }
    DisposableEffect(view) {
        val observer = view.viewTreeObserver
        val listener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { windowHasFocus = it }
        observer.addOnWindowFocusChangeListener(listener)
        onDispose { if (observer.isAlive) observer.removeOnWindowFocusChangeListener(listener) }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var headsUp by remember { mutableStateOf<VirtualNotification?>(null) }
    val currentIncoming by androidx.compose.runtime.rememberUpdatedState(incoming)
    val currentWindowFocus by androidx.compose.runtime.rememberUpdatedState(windowHasFocus)
    LaunchedEffect(repository, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            repository.newNotifications.collect { notification ->
                val current = controller.state.value
                if (currentWindowFocus && NotificationHeadsUpPolicy.shouldShow(notification, current.isLocked,
                        current.surface != SystemUiSurface.NONE,
                        focusMode = ControlCenterStore.state.value.focusMode, isIncomingCall = currentIncoming)) {
                    headsUp = notification
                    if (!ControlCenterStore.state.value.quietMode) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            }
        }
    }
    LaunchedEffect(state.surface, incoming, controls.focusMode, windowHasFocus) {
        if (state.surface != SystemUiSurface.NONE || incoming || controls.focusMode || !windowHasFocus) headsUp = null
        if (state.surface != SystemUiSurface.NONE || incoming) {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }
    LaunchedEffect(activities.map { it.id }, state.surface, incoming) {
        liveExpanded = false
    }
    LaunchedEffect(state.surface, notifications) {
        if (state.surface == SystemUiSurface.NOTIFICATION_SHADE && !incoming) {
            notifications.filterNot { it.seen }.forEach { repository.markSeen(it.id) }
        }
    }
    LaunchedEffect(currentCall?.id, incoming) {
        controller.updatePriorityCall(currentCall?.id, incoming)
    }
    // Answering a call keeps the existing call screen usable while preserving the lock
    // underneath. A manual lock clears this exception; ending the call restores it.
    val priorityCallVisible = incoming || (priorityCallId != null &&
        priorityCallId == currentCall?.id && currentRoute == "call/{characterId}")
    val showLock = state.isLocked && !priorityCallVisible
    val panelOpen = !incoming && (state.surface == SystemUiSurface.NOTIFICATION_SHADE ||
        state.surface == SystemUiSurface.CONTROL_CENTER)

    fun launchUnlocked(route: String) {
        headsUp = null
        liveExpanded = false
        controller.unlock()
        onLaunchRoute(route)
    }

    fun openNotification(notification: VirtualNotification) {
        scope.launch { repository.markSeen(notification.id) }
        headsUp = null
        controller.unlock()
        notification.route?.let(onLaunchRoute)
    }

    val activityContent: (@Composable () -> Unit)? = if (activities.isEmpty()) null else {
        { LiveActivityChip(activities, onClick = { liveExpanded = !liveExpanded }) }
    }
    VirtualSystemUiProvider(controller, unseenCount = notifications.count { !it.seen }, activityContent = activityContent) {
        // Android 15+ enforces edge-to-edge. Keep all virtual OS surfaces inside
        // the same cutout/system-bar safe area, including transient bar reveals.
        Box(Modifier.fillMaxSize().background(LocalAiluaTheme.current.palette.backgroundPrimary).safeDrawingPadding()) {
            Box(Modifier.fillMaxSize().coveredSystemUi(showLock || panelOpen || liveExpanded)) {
                content()
            }
            headsUp?.let { notification ->
                HeadsUpNotification(notification, ::openNotification, onDismiss = { headsUp = null },
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 46.dp, start = 16.dp, end = 16.dp))
            }
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
                    modifier = Modifier.coveredSystemUi(panelOpen || liveExpanded).clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}
                    ),
                    notificationContent = {
                        val unread = notifications.filterNot { it.seen }
                        LockScreenNotificationPreview(
                            unread.map { LockScreenNotificationItem(it.id, publicNotificationTitle(it), it.body) },
                            onOpen = { id -> unread.firstOrNull { it.id == id }?.let(::openNotification) },
                            onOpenAll = controller::openNotifications,
                        )
                    },
                    liveActivityContent = {
                        activities.firstOrNull()?.let { activity ->
                            LiveActivitySummary(activity, onClick = { liveExpanded = true })
                        }
                    },
                )
            }
            if (!incoming && state.surface == SystemUiSurface.NOTIFICATION_SHADE) {
                NotificationShade(
                    notifications = notifications,
                    onOpen = ::openNotification,
                    onDismiss = { scope.launch { repository.dismiss(it.id) } },
                    onClearAll = { scope.launch { repository.clearDismissible() } },
                    onClose = controller::closeSystemSurface,
                    modifier = Modifier.coveredSystemUi(liveExpanded).clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}
                    ),
                    quickControls = {
                        QuickControlsRow(
                            state = controls, isDarkTheme = isDarkTheme,
                            onToggleDarkMode = onToggleDarkMode,
                            onFocusModeChange = ControlCenterStore::setFocusMode,
                            onLock = controller::lock,
                            onExpand = controller::openControlCenter,
                        )
                    },
                    ongoingContent = {
                        activities.firstOrNull()?.let { activity ->
                            LiveActivitySummary(activity, onClick = { liveExpanded = true })
                        }
                    },
                )
            }
            if (!incoming) {
                LiveActivityHost(
                    activities = activities, expanded = liveExpanded,
                    onDismiss = { liveExpanded = false },
                    onLaunchRoute = ::launchUnlocked,
                    onEnd = {
                        liveExpanded = false
                        CallStateEngine.handleAction(CallAction.END)
                        if (currentRoute == "call/{characterId}") onLaunchRoute(AiluaDestinations.CALL_HISTORY)
                    },
                )
            }
            if (!incoming && state.surface == SystemUiSurface.CONTROL_CENTER) {
                ControlCenter(
                    state = controls, isDarkTheme = isDarkTheme,
                    onToggleDarkMode = onToggleDarkMode,
                    onFocusModeChange = ControlCenterStore::setFocusMode,
                    onQuietModeChange = ControlCenterStore::setQuietMode,
                    onBrightnessChange = ControlCenterStore::setVirtualBrightness,
                    onLock = controller::lock,
                    onTheme = { launchUnlocked(AiluaDestinations.HOME) },
                    onLaunchRoute = ::launchUnlocked,
                    onClose = controller::closeSystemSurface,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}
                    ),
                )
            }
            // Draw-only layer: virtual dimming never intercepts controls or changes Android brightness.
            if (controls.virtualDimAlpha > 0f) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = controls.virtualDimAlpha)))
            }
            BackHandler(enabled = !incoming && (liveExpanded || showLock ||
                state.surface == SystemUiSurface.NOTIFICATION_SHADE || state.surface == SystemUiSurface.CONTROL_CENTER)) {
                if (liveExpanded) liveExpanded = false else controller.closeSystemSurface()
            }
        }
    }
}

/** An occluded app remains composed, but cannot expose duplicate controls to accessibility. */
private fun Modifier.coveredSystemUi(covered: Boolean): Modifier =
    if (covered) clearAndSetSemantics { } else this
