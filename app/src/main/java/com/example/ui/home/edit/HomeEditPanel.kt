package com.example.ui.home.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun HomeEditPanel(
    onDone: () -> Unit,
    onWidgets: () -> Unit,
    onWallpaper: () -> Unit,
    onTheme: () -> Unit,
    onPages: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAiluaTheme.current
    val shape = RoundedCornerShape(theme.shapes.medium.dp)
    Column(
        modifier.fillMaxWidth().padding(horizontal = theme.layout.screenHorizontalPadding.dp, vertical = 6.dp)
            .clip(shape)
            .background(theme.surfaces.overlay)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("home_edit_panel"),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("编辑桌面", color = theme.palette.onSurface, style = theme.text.section)
            Spacer(Modifier.weight(1f))
            Row(Modifier.clip(RoundedCornerShape(theme.shapes.small.dp))
                .background(theme.surfaces.inset)
                .clickable(onClick = onDone)
                .padding(horizontal = 11.dp, vertical = 6.dp)
                .testTag("home_edit_done"), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, null, tint = theme.palette.accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(3.dp))
                Text("完成", color = theme.palette.accent, style = theme.text.secondary)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            EditAction(Icons.Default.Widgets, "组件", "home_edit_widgets", onWidgets, Modifier.weight(1f))
            EditAction(Icons.Default.Wallpaper, "壁纸", "home_edit_wallpaper", onWallpaper, Modifier.weight(1f))
            EditAction(Icons.Default.Palette, "主题", "home_edit_theme", onTheme, Modifier.weight(1f))
            EditAction(Icons.Default.ViewCarousel, "页面", "home_edit_pages", onPages, Modifier.weight(1f))
            EditAction(Icons.Default.Tune, "设置", "home_edit_settings", onSettings, Modifier.weight(1f))
        }
    }
}

@Composable
private fun EditAction(
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAiluaTheme.current
    Column(modifier.clip(RoundedCornerShape(theme.shapes.small.dp))
        .clickable(onClick = onClick).padding(vertical = 7.dp)
        .testTag(tag), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = theme.palette.onSurfaceMuted, modifier = Modifier.size(22.dp))
        Text(label, color = theme.palette.onSurface, style = theme.text.caption)
    }
}
