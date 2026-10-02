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
import com.example.data.model.DayPhase
import com.example.data.model.WeatherState
import com.example.ui.components.HideDialogStatusBar
import com.example.ui.themeengine.*

enum class ThemeCenterSection(val title: String) {
    THEMES("内置"), MINE("我的主题"), PACKS("图标包"), IMPORT("导入"),
    PALETTES("配色"), WALLPAPERS("壁纸"), ICONS("图标样式")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCenterSheet(
    onDismiss: () -> Unit,
    isDarkTheme: Boolean,
    dayPhase: DayPhase,
    weather: WeatherState,
    initialSection: ThemeCenterSection = ThemeCenterSection.THEMES,
) {
    val selection = ThemeStore.selection
    var tab by remember(initialSection) { mutableStateOf(initialSection) }
    val runtime = ThemeResolver.resolve(selection, isDarkTheme, dayPhase, weather)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = runtime.surfaces.raised,
        modifier = Modifier.testTag("theme_center_sheet"),
    ) {
        HideDialogStatusBar()
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("主题中心", style = LocalAiluaTheme.current.text.title,
                fontWeight = FontWeight.Bold, modifier = Modifier.testTag("theme_center_title"))
            Text("选择主题、壁纸和图标",
                style = LocalAiluaTheme.current.text.secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ThemeCenterSection.entries.take(4).forEach { item ->
                    val selected = tab == item
                    val shape = RoundedCornerShape(LocalAiluaTheme.current.shapes.medium.dp)
                    Box(Modifier.weight(1f).clip(shape)
                        .background(if (selected) runtime.palette.accent.copy(alpha = 0.18f) else Color.Transparent)
                        .border(1.dp, if (selected) runtime.palette.accent.copy(alpha = 0.48f) else Color.Transparent, shape)
                        .clickable { tab = item }.padding(vertical = 9.dp)
                        .testTag("theme_center_tab_" + item.name.lowercase()),
                        contentAlignment = Alignment.Center) {
                        Text(item.title,
                            color = if (selected) runtime.palette.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, style = LocalAiluaTheme.current.text.secondary)
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ThemeCenterSection.entries.drop(4).forEach { item ->
                    val selected = tab == item
                    val shape = RoundedCornerShape(LocalAiluaTheme.current.shapes.small.dp)
                    Box(Modifier.weight(1f).clip(shape)
                        .background(if (selected) runtime.palette.accent.copy(alpha = 0.18f) else LocalAiluaTheme.current.surfaces.inset)
                        .clickable { tab = item }.padding(vertical = 6.dp)
                        .testTag("theme_center_tab_" + item.name.lowercase()),
                        contentAlignment = Alignment.Center) {
                        Text(item.title,
                            color = if (selected) runtime.palette.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = LocalAiluaTheme.current.text.caption)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (tab in setOf(ThemeCenterSection.PALETTES, ThemeCenterSection.WALLPAPERS, ThemeCenterSection.ICONS)) {
                    ThemePreview(runtime, Modifier.fillMaxWidth().height(200.dp))
                    Spacer(Modifier.height(14.dp))
                }
                when (tab) {
                    ThemeCenterSection.THEMES -> ThemeCatalog.presets.chunked(2).forEach { presets ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            presets.forEach { preset ->
                                val candidate = selection.copy(themePresetId = preset.id)
                                val preview = ThemeResolver.resolve(candidate, isDarkTheme, dayPhase, weather)
                                val selected = selection.themePresetId == preset.id
                                val shape = RoundedCornerShape(LocalAiluaTheme.current.shapes.medium.dp)
                                Column(Modifier.weight(1f).clip(shape)
                                    .background(LocalAiluaTheme.current.surfaces.inset)
                                    .border(1.5.dp, if (selected) preview.palette.accent else Color.Transparent, shape)
                                    .clickable { ThemeStore.update(candidate) }.padding(7.dp)
                                    .testTag("theme_option_" + preset.id)) {
                                    ThemePreview(preview, Modifier.fillMaxWidth().height(200.dp))
                                    Spacer(Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(preset.name, fontWeight = FontWeight.SemiBold, style = LocalAiluaTheme.current.text.secondary)
                                            Text(preset.nameEn, color = MaterialTheme.colorScheme.onSurfaceVariant, style = LocalAiluaTheme.current.text.caption)
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
                    ThemeCenterSection.MINE -> MyThemesSection(runtime)
                    ThemeCenterSection.PACKS -> IconPacksSection(runtime)
                    ThemeCenterSection.IMPORT -> ImportThemeSection(runtime) {
                        tab = ThemeCenterSection.MINE
                    }
                    ThemeCenterSection.PALETTES -> {
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
                    ThemeCenterSection.WALLPAPERS -> {
                        val defaultColors = ThemeResolver.resolve(selection.copy(wallpaperOverrideId = null, wallpaperSourceId = null),
                            isDarkTheme, dayPhase, weather).wallpaper.colors
                        WallpaperRow("跟随主题", "使用当前主题的默认壁纸", defaultColors,
                            selection.wallpaperOverrideId == null && selection.wallpaperSourceId == null, "wallpaper_option_default") {
                            ThemeStore.update(selection.copy(wallpaperOverrideId = null, wallpaperSourceId = null))
                        }
                        WallpaperCatalog.options.forEach { option ->
                            val candidate = selection.copy(wallpaperOverrideId = option.id, wallpaperSourceId = null)
                            WallpaperRow(option.name, option.nameEn,
                                ThemeResolver.resolve(candidate, isDarkTheme, dayPhase, weather).wallpaper.colors,
                                selection.wallpaperOverrideId == option.id && selection.wallpaperSourceId == null, "wallpaper_option_" + option.id) {
                                ThemeStore.update(candidate)
                            }
                        }
                        ExternalWallpaperChoices(runtime)
                    }
                    ThemeCenterSection.ICONS -> {
                        IconRow("跟随主题", "使用当前主题的默认图标",
                            ThemeResolver.resolve(selection.copy(iconStyleOverrideId = null, iconSourceOverrideId = null, manualIconOverrides = emptyMap()), isDarkTheme, dayPhase, weather),
                            selection.iconStyleOverrideId == null && selection.iconSourceOverrideId == null, "icon_option_default") {
                            ThemeStore.update(selection.copy(iconStyleOverrideId = null, iconSourceOverrideId = null, manualIconOverrides = emptyMap()))
                        }
                        IconStyleCatalog.options.forEach { option ->
                            val candidate = selection.copy(iconStyleOverrideId = option.id, iconSourceOverrideId = null, manualIconOverrides = emptyMap())
                            IconRow(option.name, option.nameEn,
                                ThemeResolver.resolve(candidate, isDarkTheme, dayPhase, weather),
                                selection.iconStyleOverrideId == option.id && selection.iconSourceOverrideId == null, "icon_option_" + option.id) {
                                ThemeStore.update(candidate)
                            }
                        }
                        ExternalIconChoices(runtime)
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun ChoiceRow(title: String, subtitle: String, accent: Color, selected: Boolean, tag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(LocalAiluaTheme.current.shapes.medium.dp)
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(shape)
        .background(LocalAiluaTheme.current.surfaces.inset)
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
    val shape = RoundedCornerShape(LocalAiluaTheme.current.shapes.medium.dp)
    val swatch = colors.ifEmpty { listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface) }
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(shape)
        .background(LocalAiluaTheme.current.surfaces.inset)
        .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
        .clickable(onClick = onClick).padding(9.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 48.dp, height = 42.dp).clip(RoundedCornerShape(LocalAiluaTheme.current.shapes.small.dp))
            .background(Brush.verticalGradient(swatch)))
        Spacer(Modifier.width(12.dp))
        ChoiceText(title, subtitle, Modifier.weight(1f))
        if (selected) Icon(Icons.Default.Check, "已选", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun IconRow(title: String, subtitle: String, preview: AiluaThemeRuntime, selected: Boolean, tag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(LocalAiluaTheme.current.shapes.medium.dp)
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(shape)
        .background(LocalAiluaTheme.current.surfaces.inset)
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
        Text(title, fontWeight = FontWeight.SemiBold, style = LocalAiluaTheme.current.text.secondary)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = LocalAiluaTheme.current.text.caption)
    }
}
