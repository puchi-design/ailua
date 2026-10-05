package com.example.ui.themecenter

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.DayPhase
import com.example.data.model.WeatherState
import com.example.ui.components.AppIconItem
import com.example.ui.components.HideDialogStatusBar
import com.example.ui.themeengine.*

// Legacy entry points remain source compatible; only four product sections are displayed.
enum class ThemeCenterSection(val title: String) {
    THEMES("主题"), MINE("我的"), PACKS("图标"), IMPORT("我的"),
    PALETTES("我的"), WALLPAPERS("壁纸"), ICONS("图标")
}

private fun ThemeCenterSection.consumerSection(): ThemeCenterSection = when (this) {
    ThemeCenterSection.PACKS -> ThemeCenterSection.ICONS
    ThemeCenterSection.IMPORT, ThemeCenterSection.PALETTES -> ThemeCenterSection.MINE
    else -> this
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
    val context = LocalContext.current
    var tab by remember(initialSection) { mutableStateOf(initialSection.consumerSection()) }
    var detailId by remember { mutableStateOf<String?>(null) }
    var wallpaperCandidate by remember { mutableStateOf<ThemeSelection?>(null) }
    var wallpaperTitle by remember { mutableStateOf("") }
    var iconCandidate by remember { mutableStateOf<ThemeSelection?>(null) }
    var iconTitle by remember { mutableStateOf("") }
    var recent by remember { mutableStateOf(ThemeRecentHistory.read(context)) }
    val runtime = ThemeResolver.resolve(selection, isDarkTheme, dayPhase, weather)
    val theme = LocalAiluaTheme.current
    fun applyTheme(id: String) {
        ThemeStore.update(ThemeStore.selection.withOfficialTheme(id))
        ThemeRecentHistory.record(context, id)
        recent = ThemeRecentHistory.read(context)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
        containerColor = theme.surfaces.raised,
        modifier = Modifier.testTag("theme_center_sheet"),
    ) {
        // Handle navigation inside the dialog before dismissing the whole theme center.
        ThemeSheetBackHandler {
            when {
                detailId != null -> detailId = null
                wallpaperCandidate != null -> wallpaperCandidate = null
                iconCandidate != null -> iconCandidate = null
                else -> onDismiss()
            }
        }
        HideDialogStatusBar()
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            if (iconCandidate == null) {
                Text("主题中心", style = theme.text.title, fontWeight = FontWeight.Bold,
                    color = theme.palette.onSurface, modifier = Modifier.testTag("theme_center_title"))
                Text("让这台手机变成你的世界", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ThemeCenterSection.THEMES, ThemeCenterSection.WALLPAPERS, ThemeCenterSection.ICONS, ThemeCenterSection.MINE).forEach { item ->
                        val selected = tab == item
                        Text(item.title, style = theme.text.secondary,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) theme.palette.onSurface else theme.palette.onSurfaceMuted,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(theme.shapes.small.dp))
                                .background(if (selected) theme.surfaces.inset else Color.Transparent)
                                .clickable { tab = item; detailId = null; wallpaperCandidate = null; iconCandidate = null }
                                .padding(vertical = 12.dp).testTag("theme_center_tab_" + item.name.lowercase()))
                    }
                }
                Spacer(Modifier.height(12.dp))
            } else {
                TextButton(onClick = { iconCandidate = null }, modifier = Modifier.testTag("icon_preview_back")) {
                    Text("‹  图标")
                }
            }
            key(tab, detailId, wallpaperCandidate, iconCandidate) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    val detail = detailId?.let { id -> officialThemeProducts.firstOrNull { it.id == id } }
                    when {
                        detail != null -> {
                            TextButton(onClick = { detailId = null }, modifier = Modifier.testTag("theme_detail_back")) { Text("‹ 主题") }
                            Column(Modifier.testTag("theme_detail"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                ProductDescription(detail)
                                ProductPreview(detail.id, isDarkTheme, dayPhase, weather)
                                Button(onClick = { applyTheme(detail.id) }, modifier = Modifier.fillMaxWidth().testTag("theme_apply_full")) {
                                    Text(if (selection.themePresetId == detail.id) "应用完整主题 · 当前" else "应用完整主题")
                                }
                                TextButton(onClick = {
                                    ThemeStore.update(ThemeStore.selection.copy(wallpaperOverrideId = ThemeCatalog.byId(detail.id).wallpaperId, wallpaperSourceId = null))
                                }, modifier = Modifier.fillMaxWidth().testTag("theme_apply_wallpaper")) { Text("只使用壁纸") }
                                TextButton(onClick = {
                                    ThemeStore.update(ThemeStore.selection.copy(iconStyleOverrideId = ThemeCatalog.byId(detail.id).iconStyleId,
                                        iconSourceOverrideId = null, manualIconOverrides = emptyMap()))
                                }, modifier = Modifier.fillMaxWidth().testTag("theme_apply_icons")) { Text("只使用图标") }
                            }
                        }
                        wallpaperCandidate != null -> {
                            val candidate = wallpaperCandidate!!
                            TextButton(onClick = { wallpaperCandidate = null }, modifier = Modifier.testTag("wallpaper_preview_back")) { Text("‹ 壁纸") }
                            Text(wallpaperTitle, style = theme.text.section, color = theme.palette.onSurface)
                            Spacer(Modifier.height(12.dp))
                            Box(Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(theme.shapes.medium.dp)).testTag("wallpaper_preview")) {
                                ThemeWallpaper(Modifier.fillMaxSize(), ThemeResolver.resolve(candidate, isDarkTheme, dayPhase, weather), candidate)
                            }
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = {
                                ThemeStore.update(ThemeStore.selection.copy(wallpaperOverrideId = candidate.wallpaperOverrideId,
                                    wallpaperSourceId = candidate.wallpaperSourceId))
                            }, modifier = Modifier.fillMaxWidth().testTag("wallpaper_apply")) { Text("应用壁纸") }
                        }
                        iconCandidate != null -> {
                            val candidate = iconCandidate!!
                            val preview = ThemeResolver.resolve(candidate, isDarkTheme, dayPhase, weather)
                            Text(iconTitle, style = theme.text.title, color = theme.palette.onSurface,
                                fontWeight = FontWeight.SemiBold)
                            Text("常用应用图标", style = theme.text.secondary,
                                color = theme.palette.onSurfaceMuted, modifier = Modifier.padding(top = 4.dp))
                            Spacer(Modifier.height(20.dp))
                            IconCollectionPreview(preview, candidate)
                            Spacer(Modifier.height(22.dp))
                            val alreadyApplied = candidate.iconStyleOverrideId == selection.iconStyleOverrideId &&
                                candidate.iconSourceOverrideId == selection.iconSourceOverrideId &&
                                candidate.manualIconOverrides == selection.manualIconOverrides
                            if (alreadyApplied) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = theme.palette.accent)
                                    Spacer(Modifier.width(8.dp))
                                    Text("正在使用", style = theme.text.secondary, color = theme.palette.onSurface)
                                }
                            } else {
                                OutlinedButton(onClick = {
                                    ThemeStore.update(ThemeStore.selection.copy(iconStyleOverrideId = candidate.iconStyleOverrideId,
                                        iconSourceOverrideId = candidate.iconSourceOverrideId, manualIconOverrides = candidate.manualIconOverrides))
                                }, modifier = Modifier.fillMaxWidth().testTag("icon_apply"),
                                    border = BorderStroke(1.dp, theme.palette.accent.copy(alpha = 0.65f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.palette.onSurface)) {
                                    Text("应用这套图标")
                                }
                            }
                            Text("壁纸与桌面布局保持原样", style = theme.text.caption,
                                color = theme.palette.onSurfaceMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                        }
                        tab == ThemeCenterSection.THEMES -> {
                            officialThemeProducts.forEach { product ->
                                Column(Modifier.fillMaxWidth().padding(bottom = 28.dp)
                                    .clickable { detailId = product.id }.testTag("theme_option_" + product.id),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ProductDescription(product)
                                    ProductPreview(product.id, isDarkTheme, dayPhase, weather) { detailId = product.id }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = { detailId = product.id }, modifier = Modifier.weight(1f)) { Text("查看主题") }
                                        Button(onClick = { applyTheme(product.id) }, modifier = Modifier.testTag("theme_quick_apply_" + product.id)) {
                                            Text(if (selection.themePresetId == product.id) "已应用" else "应用")
                                        }
                                    }
                                }
                            }
                        }
                        tab == ThemeCenterSection.WALLPAPERS -> {
                            ChoiceRow("跟随主题", "使用主题自带壁纸", runtime.palette.accent,
                                selection.wallpaperOverrideId == null && selection.wallpaperSourceId == null, "wallpaper_follow_theme") {
                                wallpaperCandidate = selection.copy(wallpaperOverrideId = null, wallpaperSourceId = null)
                                wallpaperTitle = "跟随主题"
                            }
                            val oilWallpapers = WallpaperCatalog.options.filter { it.id.startsWith("oil_") }
                            if (oilWallpapers.isNotEmpty()) {
                                SectionLabel("油画壁纸")
                                oilWallpapers.chunked(2).forEach { wallpapers ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        wallpapers.forEach { wallpaper ->
                                            val candidate = selection.copy(wallpaperOverrideId = wallpaper.id, wallpaperSourceId = null)
                                            Column(Modifier.weight(1f).clickable {
                                                wallpaperCandidate = candidate; wallpaperTitle = wallpaper.name
                                            }.testTag("wallpaper_option_" + wallpaper.id)) {
                                                Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(theme.shapes.medium.dp))) {
                                                    ThemeWallpaper(Modifier.fillMaxSize(), ThemeResolver.resolve(candidate, isDarkTheme, dayPhase, weather), candidate)
                                                }
                                                Text(wallpaper.name, style = theme.text.secondary, color = theme.palette.onSurface,
                                                    modifier = Modifier.padding(vertical = 10.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            SectionLabel("官方壁纸")
                            officialThemeProducts.chunked(2).forEach { products ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    products.forEach { product ->
                                        val candidate = selection.copy(wallpaperOverrideId = ThemeCatalog.byId(product.id).wallpaperId, wallpaperSourceId = null)
                                        Column(Modifier.weight(1f).clickable {
                                            wallpaperCandidate = candidate; wallpaperTitle = product.title
                                        }.testTag("wallpaper_option_" + product.id)) {
                                            Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(theme.shapes.medium.dp))) {
                                                ThemeWallpaper(Modifier.fillMaxSize(), ThemeResolver.resolve(candidate, isDarkTheme, dayPhase, weather), candidate)
                                            }
                                            Text(product.title, style = theme.text.secondary, color = theme.palette.onSurface, modifier = Modifier.padding(vertical = 10.dp))
                                        }
                                    }
                                }
                            }
                            SectionLabel("我的壁纸")
                            if (com.example.ui.themeengine.external.ExternalThemeRepository.themes.none { it.wallpapers.isNotEmpty() }) {
                                Text("你的壁纸会显示在这里。", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                            }
                            ExternalWallpaperChoices(runtime) { imported ->
                                wallpaperCandidate = selection.copy(wallpaperSourceId = imported.id)
                                wallpaperTitle = imported.name
                            }
                        }
                        tab == ThemeCenterSection.ICONS -> {
                            SectionLabel("官方图标")
                            val follow = selection.copy(iconStyleOverrideId = null, iconSourceOverrideId = null, manualIconOverrides = emptyMap())
                            IconRow("跟随主题", "使用主题自带图标", ThemeResolver.resolve(follow, isDarkTheme, dayPhase, weather), follow,
                                selection.iconStyleOverrideId == null && selection.iconSourceOverrideId == null, "icon_option_default") {
                                iconCandidate = follow; iconTitle = "跟随主题"
                            }
                            officialThemeProducts.forEach { product ->
                                val style = ThemeCatalog.byId(product.id).iconStyleId
                                val candidate = selection.copy(iconStyleOverrideId = style, iconSourceOverrideId = null, manualIconOverrides = emptyMap())
                                IconRow(product.title, product.materials, ThemeResolver.resolve(candidate, isDarkTheme, dayPhase, weather), candidate,
                                    selection.iconStyleOverrideId == style && selection.iconSourceOverrideId == null, "icon_option_" + style) {
                                    iconCandidate = candidate; iconTitle = product.title
                                }
                            }
                            SectionLabel("我的图标包")
                            ExternalIconChoices(runtime)
                            IconPacksSection(runtime, showAdvanced = false)
                        }
                        else -> {
                            SectionLabel("当前主题")
                            val current = ThemeCatalog.byId(selection.themePresetId)
                            ThemePreview(runtime, Modifier.fillMaxWidth().height(230.dp), selection = selection)
                            Text(current.name, style = theme.text.section, color = theme.palette.onSurface, modifier = Modifier.padding(vertical = 12.dp))
                            if (recent.isNotEmpty()) {
                                SectionLabel("最近使用")
                                recent.forEach { id ->
                                    val preset = ThemeCatalog.byId(id)
                                    ChoiceRow(preset.name, preset.nameEn, runtime.palette.accent, id == selection.themePresetId, "recent_theme_" + id) {
                                        if (officialThemeProducts.any { it.id == id }) detailId = id else applyTheme(id)
                                    }
                                }
                            }
                            SectionLabel("已导入主题")
                            MyThemesSection(runtime)
                            CustomThemeChoices(selection, isDarkTheme, dayPhase, weather)
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

/** Register dialog navigation on the dialog's START, after Material3's default callback. */
@Composable
internal fun ThemeSheetBackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val owner = LocalOnBackPressedDispatcherOwner.current as? LifecycleOwner
        ?: LocalLifecycleOwner.current
    CompositionLocalProvider(LocalLifecycleOwner provides owner) {
        BackHandler(enabled = enabled, onBack = onBack)
    }
}

@Composable
private fun ProductPreview(id: String, dark: Boolean, phase: DayPhase, weather: WeatherState, onClick: (() -> Unit)? = null) {
    val candidate = officialPreviewSelection(id)
    ThemePreview(ThemeResolver.resolve(candidate, dark, phase, weather),
        Modifier.fillMaxWidth().aspectRatio(9f / 16f), candidate, onClick)
}

@Composable
private fun ProductDescription(product: ThemeProduct) {
    val theme = LocalAiluaTheme.current
    Text(product.title, style = theme.text.title, color = theme.palette.onSurface, fontWeight = FontWeight.SemiBold)
    Text(product.description, style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
    Text(product.materials, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
}

private val iconCollectionApps = listOf(
    ThemePreviewApp("chat", "消息"),
    ThemePreviewApp("living", "生活"),
    ThemePreviewApp("moments", "动态"),
    ThemePreviewApp("gallery", "相册"),
    ThemePreviewApp("contacts", "联系人"),
    ThemePreviewApp("diary", "日记"),
    ThemePreviewApp("mailbox", "信箱"),
    ThemePreviewApp("call", "通话"),
    ThemePreviewApp("memories", "记忆"),
    ThemePreviewApp("world", "地点"),
    ThemePreviewApp("theater", "剧场"),
    ThemePreviewApp("apps", "应用库"),
)

/** The same icon renderer and candidate selection used by Home, without a duplicate phone mockup. */
@Composable
private fun IconCollectionPreview(preview: AiluaThemeRuntime, candidate: ThemeSelection) {
    val sheetTheme = LocalAiluaTheme.current
    AiluaThemeProvider(preview) {
        Column(Modifier.fillMaxWidth().testTag("icon_preview"), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            iconCollectionApps.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    row.forEach { app ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            AppIconItem(
                                name = app.name,
                                iconKey = app.iconKey,
                                size = 56.dp,
                                selection = candidate,
                                showLabel = false,
                                interactive = false,
                                onClick = {},
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = app.name,
                                style = sheetTheme.text.caption,
                                color = sheetTheme.palette.onSurface,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SectionLabel(title: String) {
    val theme = LocalAiluaTheme.current
    Text(title, style = theme.text.section, color = theme.palette.onSurface, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 18.dp, bottom = 12.dp))
}

@Composable
private fun CustomThemeChoices(selection: ThemeSelection, dark: Boolean, phase: DayPhase, weather: WeatherState) {
    val theme = LocalAiluaTheme.current
    var expanded by remember { mutableStateOf(false) }
    SectionLabel("自定义搭配")
    Text("壁纸与图标可以自由组合，桌面位置保持不变。", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("theme_custom_mix")) { Text(if (expanded) "收起搭配" else "调整配色与经典外观") }
    if (expanded) {
        ChoiceRow("跟随主题", "默认配色", theme.palette.accent, selection.paletteOverrideId == null, "palette_option_default") {
            ThemeStore.update(ThemeStore.selection.copy(paletteOverrideId = null))
        }
        PaletteCatalog.palettes.forEach { palette ->
            ChoiceRow(palette.name, "", palette.accent, selection.paletteOverrideId == palette.id, "palette_option_" + palette.id) {
                ThemeStore.update(ThemeStore.selection.copy(paletteOverrideId = palette.id))
            }
        }
        SectionLabel("经典外观")
        ThemeCatalog.presets.take(4).forEach { preset ->
            ChoiceRow(preset.name, "兼容已有搭配", ThemeResolver.resolve(ThemeSelection(preset.id), dark, phase, weather).palette.accent,
                selection.themePresetId == preset.id, "theme_option_" + preset.id) {
                ThemeStore.update(ThemeStore.selection.withOfficialTheme(preset.id))
            }
        }
        SectionLabel("纯色与随世界变化")
        WallpaperCatalog.options.filter { option -> !option.id.startsWith("oil_") && officialThemeProducts.none { it.id == option.id } }.forEach { wallpaper ->
            ChoiceRow(wallpaper.name, "只更换壁纸", theme.palette.accent,
                selection.wallpaperOverrideId == wallpaper.id && selection.wallpaperSourceId == null, "wallpaper_option_" + wallpaper.id) {
                ThemeStore.update(ThemeStore.selection.copy(wallpaperOverrideId = wallpaper.id, wallpaperSourceId = null))
            }
        }
    }
}

@Composable
private fun ChoiceRow(title: String, subtitle: String, accent: Color, selected: Boolean, tag: String, onClick: () -> Unit) {
    val theme = LocalAiluaTheme.current
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp).testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = theme.text.secondary, color = theme.palette.onSurface)
            if (subtitle.isNotEmpty()) Text(subtitle, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
        }
        if (selected) Icon(Icons.Default.Check, "已选", tint = theme.palette.accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun IconRow(title: String, subtitle: String, preview: AiluaThemeRuntime, selection: ThemeSelection, selected: Boolean, tag: String, onClick: () -> Unit) {
    val theme = LocalAiluaTheme.current
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp).testTag(tag), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = theme.text.section, color = theme.palette.onSurface)
                Text(subtitle, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
            }
            if (selected) Icon(Icons.Default.Check, "已选", tint = theme.palette.accent, modifier = Modifier.size(18.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf("chat", "living", "gallery", "settings").forEach { key -> ThemePreviewIcon(preview, key, 44.dp, selection, onClick) }
        }
        HorizontalDivider(color = theme.surfaces.divider)
    }
}
