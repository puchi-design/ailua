package com.example.ui.systemui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalVirtualSystemUiController = staticCompositionLocalOf<VirtualSystemUiController?> { null }
val LocalUnseenNotificationCount = staticCompositionLocalOf { 0 }
val LocalStatusBarActivityContent = staticCompositionLocalOf<(@Composable () -> Unit)?> { null }

@Composable
fun VirtualSystemUiProvider(
    controller: VirtualSystemUiController = VirtualSystemUiSession.controller,
    unseenCount: Int = 0,
    activityContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalVirtualSystemUiController provides controller,
        LocalUnseenNotificationCount provides unseenCount,
        LocalStatusBarActivityContent provides activityContent,
        content = content
    )
}
