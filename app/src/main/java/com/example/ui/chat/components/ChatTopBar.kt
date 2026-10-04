package com.example.ui.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.designsystem.AiluaTopBar
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun ChatTopBar(
    characterId: String,
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
        leading = { CharacterPortrait(characterId, PortraitVariant.AVATAR, Modifier.size(40.dp), onClick = onOpenProfile) },
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
