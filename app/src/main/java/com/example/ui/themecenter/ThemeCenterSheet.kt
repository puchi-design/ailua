package com.example.ui.themecenter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayPhase
import com.example.data.model.WeatherState
import com.example.ui.themeengine.*

private enum class ThemeCenterTab(val title: String) {
    THEMES("主题"), PALETTES("配色"), WALLPAPERS("壁纸"), ICONS("图标")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCenterSheet(
    onDismiss: () -> Unit,
    isDarkTheme: Boolean,
    dayPhase: DayPhase,
    weather: WeatherState,
) {
    val selection = ThemeStore.selection
    var tab by remember { mutableStateOf(ThemeCenterTab.THEMES) }
    val runtime = ThemeResolver.resolve(selection, isDarkTheme, dayPhase, weather)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("theme_center_sheet"),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("主题中心", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, modifier = Modifier.testTag("theme_center_title"))
            Text("打造你的虚拟手机 · 所有更改即时生效",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ThemeCenterTab.entries.forEach { item ->
                    val selected = tab == item
                    val shape = RoundedCornerShape(13.dp)
                    Box(Modifier.weight(1f).clip(shape)
                        .background(if (selected) runtime.palette.accent.copy(alpha = 0.18f) else Color.Transparent)
                        .border(1.dp, if (selected) runtime.palette.accent.copy(alpha = 0.48f) else Color.Transparent, shape)
                        .clickable { tab = item }.padding(vertical = 9.dp)
                        .testTag("theme_center_tab_" + item.name.lowercase()),
                        contentAlignment = Alignment.Center) {
                        Text(item.title,
                            color = if (selected) runtime.palette.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (tab != ThemeCenterTab.THEMES) {
                    ThemePreview(runtime, Modifier.fillMaxWidth().height(190.dp))
                    Spacer(Modifier.height(14.dp))
                }
                when (tab) {
                    ThemeCenterTab.THEMES -> ThemeCatalog.presets.chunked(2).forEach { presets ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            presets.forEach { preset ->
                                val candidate = selection.copy(themePresetId = preset.id)
                                val preview = ThemeResolver.resolve(candidate, isDarkTheme, dayPhase, weather)
                                val selected = selection.themePresetId == preset.id
                                val shape = RoundedCornerShape(17.dp)
                                Column(Modifier.weight(1f).clip(shape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .border(1.5.dp, if (selected) preview.palette.accent else Color.Transparent, shape)
                                    .clickable { ThemeStore.update(candidate) }.padding(7.dp)
                                    .testTag("theme_option_" + preset.id)) {
                                    ThemePreview(preview, Modifier.fillMaxWidth().height(162.dp))
                                    Spacer(Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(preset.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(preset.nameEn, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                        }
                                        if (selected) Icon(Icons.Default.Check, "当前主题",
                                            tint = preview.palette.accent, modifier = Modifier.size(17.dp))
                                    }
                                }
                            }
                            if (presets.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    ThemeCenterTab.PALETTES -> {
                        ChoiceRow("跟随主题", "使用当前主题的默认配色",
                            ThemeResolver.resolve(selection.copy(paletteOverrideId = null), isDarkTheme, dayPhase, weather).palette.accent,
                            selection.paletteOverrideId == null, "palette_option_default") {
                            ThemeStore.update(selection.copy(paletteOverrideId = null))
                        }
                        PaletteCatalog.palettes.forEach { option ->
                            val candidate = selection.copy(paletteOverrideId = option.id)
                            ChoiceRow(option.name, option.nameEn,
                                ThemeResolver.resolve(candidate, isDarkTheme, dayPhase, weather).palette.accent,
                                selection.paletteOverrideId == option.id, "palette_option_" + option.id) {
                                ThemeStore.update(candidate)
                            }
                        }
                    }
                    ThemeCenterTab.WALLPAPERS -> {
                        val defaultColors = ThemeResolver.resolve(selection.copy(wallpaperOverrideId = null),
                            isDarkTheme, dayPhase, weather).wallpaper.colors
                        WallpaperRow("跟随主题", "使用当前主题的默认壁纸", defaultColors,
                            selection.wallpaperOverrideId == null, "wallpaper_option_default") {
                            ThemeStore.update(selection.copy(wallpaperOverrideId = null))
                        }
                        WallpaperCatalog.options.forEach { option ->
                            val candidate = selection.copy(wallpaperOverrideId = option.id)
                            WallpaperRow(option.name, option.nameEn,
                                ThemeResolver.resolve(candidate, isDarkTheme, dayPhase, weather).wallpaper.colors,
                                selection.wallpaperOverrideId == option.id, "wallpaper_option_" + option.id) {
                                ThemeStore.update(candidate)
                            }
                        }
                    }
                    ThemeCenterTab.ICONS -> {
                        IconRow("跟随主题", "使用当前主题的默认图标",
                            ThemeResolver.resolve(selection.copy(iconStyleOverrideId = null), isDarkTheme, dayPhase, weather),
                            selection.iconStyleOverrideId == null, "icon_option_default") {
                            ThemeStore.update(selection.copy(iconStyleOverrideId = null))
                        }
                        IconStyleCatalog.options.forEach { option ->
                            val candidate = selection.copy(iconStyleOverrideId = option.id)
                            IconRow(option.name, option.nameEn,
                                ThemeResolver.resolve(candidate, isDarkTheme, dayPhase, weather),
                                selection.iconStyleOverrideId == option.id, "icon_option_" + option.id) {
                                ThemeStore.update(candidate)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun ChoiceRow(title: String, subtitle: String, accent: Color, selected: Boolean, tag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(15.dp)
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(shape)
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f))
        .border(1.dp, if (selected) accent else Color.Transparent, shape)
        .clickable(onClick = onClick).padding(12.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(accent)
            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape))
        Spacer(Modifier.width(12.dp))
        ChoiceText(title, subtitle, Modifier.weight(1f))
        if (selected) Icon(Icons.Default.Check, "已选", tint = accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun WallpaperRow(title: String, subtitle: String, colors: List<Color>, selected: Boolean, tag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(15.dp)
    val swatch = colors.ifEmpty { listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface) }
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(shape)
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f))
        .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
        .clickable(onClick = onClick).padding(9.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 48.dp, height = 42.dp).clip(RoundedCornerShape(9.dp))
            .background(Brush.verticalGradient(swatch)))
        Spacer(Modifier.width(12.dp))
        ChoiceText(title, subtitle, Modifier.weight(1f))
        if (selected) Icon(Icons.Default.Check, "已选", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun IconRow(title: String, subtitle: String, preview: AiluaThemeRuntime, selected: Boolean, tag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(15.dp)
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(shape)
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f))
        .border(1.dp, if (selected) preview.palette.accent else Color.Transparent, shape)
        .clickable(onClick = onClick).padding(11.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically) {
        ThemePreviewIcon(preview, "chat", 31.dp)
        Spacer(Modifier.width(5.dp))
        ThemePreviewIcon(preview, "gallery", 31.dp)
        Spacer(Modifier.width(10.dp))
        ChoiceText(title, subtitle, Modifier.weight(1f))
        if (selected) Icon(Icons.Default.Check, "已选", tint = preview.palette.accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ChoiceText(title: String, subtitle: String, modifier: Modifier) {
    Column(modifier) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}
