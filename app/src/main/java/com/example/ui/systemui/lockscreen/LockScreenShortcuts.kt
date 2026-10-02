package com.example.ui.systemui.lockscreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun LockScreenShortcuts(onOpenCommunications: () -> Unit, onOpenGallery: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        LockShortcut("通讯", Icons.Default.ChatBubbleOutline, onOpenCommunications, "lock_shortcut_communications")
        LockShortcut("相册", Icons.Default.PhotoAlbum, onOpenGallery, "lock_shortcut_gallery")
    }
}

@Composable
private fun LockShortcut(label: String, icon: ImageVector, onClick: () -> Unit, tag: String) {
    val style = LocalAiluaTheme.current.lockscreen.shortcutStyle
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick, modifier = Modifier.size(54.dp).testTag(tag),
            shape = RoundedCornerShape(LocalAiluaTheme.current.shapes.large.dp),
            color = style.backgroundColor.copy(alpha = style.surfaceAlpha), contentColor = style.foregroundColor,
            border = BorderStroke(style.border.widthDp.dp, style.border.color), shadowElevation = 0.dp
        ) {
            Icon(icon, label, Modifier.padding(16.dp))
        }
        Text(label, modifier = Modifier.padding(top = 7.dp), color = LocalAiluaTheme.current.lockscreen.foregroundColor,
            style = LocalAiluaTheme.current.text.secondary)
    }
}
