package com.example.ui.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import com.example.ui.designsystem.AiluaTopBar
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun ChatTopBar(
    name: String,
    currentActivity: String,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenMenu: () -> Unit,
    menu: @Composable () -> Unit,
) {
    AiluaTopBar(
        title = name,
        subtitle = currentActivity,
        onBack = onBack,
        onTitleClick = onOpenProfile,
        backTestTag = "chat_back_btn",
        trailing = {
            Box {
                IconButton(onClick = onOpenMenu) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "选项菜单",
                        tint = LocalAiluaTheme.current.palette.onSurfaceMuted,
                    )
                }
                menu()
            }
        },
    )
}
