package com.example.ui.home.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.HideDialogStatusBar
import com.example.ui.themeengine.LocalAiluaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDisplaySettingsSheet(
    preferences: HomeDisplayPreferences,
    onChange: (HomeDisplayPreferences) -> Unit,
    onDismiss: () -> Unit,
) {
    val theme = LocalAiluaTheme.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = theme.surfaces.raised,
        modifier = Modifier.testTag("home_display_settings_sheet"),
    ) {
        HideDialogStatusBar()
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text("桌面设置", color = theme.palette.onSurface,
                style = theme.text.title, fontWeight = FontWeight.Bold)
            Text("图标与页面显示", color = theme.palette.onSurfaceMuted,
                style = theme.text.secondary, modifier = Modifier.padding(top = 3.dp, bottom = 14.dp))
            SettingSwitch("显示应用名称", preferences.showLabels, "home_setting_labels") {
                onChange(preferences.copy(showLabels = it))
            }
            Text("图标大小", color = theme.palette.onSurface,
                style = theme.text.body, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.9f to "小", 1f to "标准", 1.1f to "大").forEach { (scale, label) ->
                    val selected = preferences.iconScale == scale
                    val shape = RoundedCornerShape(theme.shapes.small.dp)
                    Text(label, color = if (selected) theme.palette.accent else theme.palette.onSurface,
                        style = theme.text.secondary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.weight(1f).clip(shape)
                            .background(if (selected) theme.palette.accent.copy(alpha = 0.14f)
                                else theme.surfaces.inset)
                            .border(1.dp, if (selected) theme.palette.accent else theme.palette.border, shape)
                            .clickable { onChange(preferences.copy(iconScale = scale)) }
                            .padding(vertical = 11.dp)
                            .testTag("home_setting_icon_scale_${(scale * 100).toInt()}"),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            Spacer(Modifier.height(12.dp))
            SettingSwitch("显示页面指示器", preferences.showPageIndicator, "home_setting_page_indicator") {
                onChange(preferences.copy(showPageIndicator = it))
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, tag: String, onChecked: (Boolean) -> Unit) {
    val theme = LocalAiluaTheme.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(theme.shapes.medium.dp))
        .background(theme.surfaces.inset)
        .clickable { onChecked(!checked) }.padding(horizontal = 14.dp, vertical = 5.dp)
        .testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = theme.palette.onSurface, style = theme.text.body,
            modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
