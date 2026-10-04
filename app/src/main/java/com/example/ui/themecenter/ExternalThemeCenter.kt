package com.example.ui.themecenter

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.themeengine.AiluaThemeRuntime
import com.example.ui.themeengine.LocalAiluaTheme
import com.example.ui.themeengine.ThemeSelection
import com.example.ui.themeengine.ThemeStore
import com.example.ui.themeengine.external.ExternalThemeFormat
import com.example.ui.themeengine.external.ExternalThemePackage
import com.example.ui.themeengine.external.ExternalThemeRepository
import com.example.ui.themeengine.external.ImportedThemeSource
import com.example.ui.themeengine.external.ThemeImportPreview
import com.example.ui.themeengine.external.UnifiedThemeImporter
import com.example.ui.themeengine.external.iconpack.AndroidIconEntry
import com.example.ui.themeengine.external.iconpack.AndroidIconPackResolver
import com.example.ui.themeengine.external.iconpack.InstalledIconPackInfo
import com.example.ui.themeengine.external.iconpack.InstalledIconPackScanner
import com.example.ui.themeengine.icon.IconResolver
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val editableApps = listOf(
    "mailbox" to "信箱", "gallery" to "相册", "chat" to "消息", "living" to "生活",
    "moments" to "动态", "contacts" to "联系人", "call" to "通话记录",
    "memories" to "记忆", "relations" to "关系", "diary" to "日记",
    "theater" to "剧场", "check_phone" to "手机", "apps" to "应用库"
)

@Composable
internal fun MyThemesSection(runtime: AiluaThemeRuntime) {
    val themes = ExternalThemeRepository.themes
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = selectedId?.let(ExternalThemeRepository::get)
    ThemeSheetBackHandler(enabled = selected != null) { selectedId = null }
    if (selected != null) {
        TextButton(onClick = { selectedId = null }) { Text("‹ 我的主题") }
        ExternalThemeDetail(selected, runtime, null,
            onApply = { ThemeStore.update(ThemeStore.selection.withImportedTheme(selected)) },
            onDelete = {
                if (ExternalThemeRepository.delete(selected.id)) selectedId = null
            })
        return
    }
    if (themes.isEmpty()) {
        EmptyThemeMessage("还没有保存的主题", "保存到这台手机的主题会显示在这里。")
        return
    }
    themes.forEach { theme ->
        val selectedSource = ThemeStore.selection
        val inUse = selectedSource.wallpaperSourceId == theme.id ||
            selectedSource.iconSourceOverrideId == "theme:" + theme.id
        val shape = RoundedCornerShape(LocalAiluaTheme.current.shapes.medium.dp)
        Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(shape)
            .background(LocalAiluaTheme.current.surfaces.inset)
            .border(1.dp, if (inUse) runtime.palette.accent else Color.Transparent, shape)
            .clickable { selectedId = theme.id }.padding(10.dp)
            .testTag("installed_theme_" + theme.id)) {
            ImportedThemePreview(theme, runtime, modifier = Modifier.fillMaxWidth().height(210.dp))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(theme.name, fontWeight = FontWeight.SemiBold, style = LocalAiluaTheme.current.text.body)
                    Text(theme.author?.takeIf { it.isNotBlank() } ?: "本机主题",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = LocalAiluaTheme.current.text.caption)
                }
                if (inUse) Text("使用中", color = runtime.palette.accent, style = LocalAiluaTheme.current.text.caption)
            }
        }
    }
}

@Composable
private fun ExternalThemeDetail(
    theme: ExternalThemePackage,
    runtime: AiluaThemeRuntime,
    draftAssets: Map<String, ByteArray>?,
    onApply: () -> Unit,
    onDelete: (() -> Unit)? = null,
    applyTitle: String = "应用主题",
    technical: Boolean = false,
) {
    Text(theme.name, style = LocalAiluaTheme.current.text.section, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Text(if (technical) "作者：" + (theme.author ?: "未知") + "  ·  格式：" + formatTitle(theme.format)
        else theme.author?.let { "来自 $it" } ?: "保存在这台手机上的主题",
        color = MaterialTheme.colorScheme.onSurfaceVariant, style = LocalAiluaTheme.current.text.secondary)
    theme.description?.takeIf { it.isNotBlank() }?.let {
        Spacer(Modifier.height(4.dp))
        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = LocalAiluaTheme.current.text.secondary, maxLines = 3)
    }
    Spacer(Modifier.height(12.dp))
    ImportedThemePreview(theme, runtime, draftAssets, Modifier.fillMaxWidth().aspectRatio(9f / 16f))
    Spacer(Modifier.height(9.dp))
    Text("壁纸 " + theme.wallpapers.size + "  ·  图标 " +
        (theme.icons?.allIcons?.size ?: theme.icons?.mappings?.size ?: 0) +
        (if (technical) "  ·  预览 " + theme.previewAssets.size else ""),
        color = MaterialTheme.colorScheme.onSurfaceVariant, style = LocalAiluaTheme.current.text.secondary)
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onApply, modifier = Modifier.weight(1f).testTag("external_theme_apply")) {
            Text(applyTitle)
        }
        if (onDelete != null) {
            OutlinedButton(onClick = onDelete, modifier = Modifier.testTag("external_theme_delete")) {
                Text("删除")
            }
        }
    }
}

@Composable
internal fun ImportThemeSection(runtime: AiluaThemeRuntime, onInstalled: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf<ThemeImportPreview?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    ThemeSheetBackHandler(enabled = preview != null && !busy) { preview = null; status = null }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            preview = null
            status = "正在识别主题…"
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val filename = displayName(context, uri)
                        UnifiedThemeImporter.inspect(ImportedThemeSource(filename, readThemeBytes(context, uri)))
                    }
                }.onSuccess {
                    preview = it
                    status = null
                }.onFailure {
                    status = it.message ?: "无法读取主题文件"
                }
                busy = false
            }
        }
    }
    Text("导入本地主题", style = LocalAiluaTheme.current.text.section, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(5.dp))
    Text("支持 .ailuatheme、MIUI / HyperOS .mtz 和 ColorOS .theme",
        color = MaterialTheme.colorScheme.onSurfaceVariant, style = LocalAiluaTheme.current.text.secondary)
    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = { launcher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
        enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("theme_import_pick")
    ) { Text("选择本地主题文件") }
    if (busy) {
        Spacer(Modifier.height(9.dp))
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    status?.let {
        Spacer(Modifier.height(9.dp))
        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = LocalAiluaTheme.current.text.secondary)
    }
    preview?.let { detected ->
        Spacer(Modifier.height(15.dp))
        Text("检测到主题", color = runtime.palette.accent,
            fontWeight = FontWeight.SemiBold, style = LocalAiluaTheme.current.text.secondary)
        Spacer(Modifier.height(7.dp))
        ExternalThemeDetail(detected.theme, runtime, detected.assets,
            onApply = {
                busy = true
                status = "正在导入…"
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) { ExternalThemeRepository.install(detected) }
                    }.onSuccess { installed ->
                        ThemeStore.update(ThemeStore.selection.withImportedTheme(installed))
                        preview = null
                        status = null
                        onInstalled()
                    }.onFailure { error ->
                        status = error.message ?: "导入失败"
                    }
                    busy = false
                }
            }, applyTitle = "导入并应用", technical = true)
    }
}

@Composable
internal fun IconPacksSection(runtime: AiluaThemeRuntime, showAdvanced: Boolean = true) {
    val context = LocalContext.current
    val resolver = remember(context) { AndroidIconPackResolver(context) }
    var refresh by remember { mutableIntStateOf(0) }
    val packs by produceState<List<InstalledIconPackInfo>>(emptyList(), context, refresh) {
        value = withContext(Dispatchers.IO) { InstalledIconPackScanner(context).scan() }
    }
    val selection = ThemeStore.selection
    val activePackage = selection.iconSourceOverrideId
        ?.takeIf { it.startsWith("android:") }?.removePrefix("android:")
    var editingKey by remember { mutableStateOf<String?>(null) }
    ThemeSheetBackHandler(enabled = showAdvanced && editingKey != null) { editingKey = null }
    val currentPack = packs.firstOrNull { it.packageName == activePackage }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("已安装图标包", style = LocalAiluaTheme.current.text.section,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        TextButton(onClick = { refresh++ }) { Text("刷新") }
    }
    if (packs.isEmpty()) {
        EmptyThemeMessage("没有找到图标包", "这台手机尚未安装可用的图标包。")
    }
    packs.forEach { pack ->
        val active = pack.packageName == activePackage
        val shape = RoundedCornerShape(LocalAiluaTheme.current.shapes.medium.dp)
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(shape)
            .background(LocalAiluaTheme.current.surfaces.inset)
            .border(1.dp, if (active) runtime.palette.accent else Color.Transparent, shape)
            .clickable {
                ThemeStore.update(ThemeStore.selection.copy(
                    iconSourceOverrideId = "android:" + pack.packageName,
                    manualIconOverrides = emptyMap()))
                editingKey = null
            }.padding(12.dp).testTag("icon_pack_" + pack.packageName),
            verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val candidate = selection.copy(iconSourceOverrideId = "android:" + pack.packageName, manualIconOverrides = emptyMap())
                listOf("chat", "gallery").forEach { key ->
                    ResolvedPackIcon(context, key, candidate, runtime, Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(pack.label, fontWeight = FontWeight.SemiBold, style = LocalAiluaTheme.current.text.secondary)
                Text(pack.packageName, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = LocalAiluaTheme.current.text.caption)
            }
            if (active) Icon(Icons.Default.Check, "已应用",
                tint = runtime.palette.accent, modifier = Modifier.size(18.dp))
        }
    }
    if (showAdvanced && currentPack != null) {
        Spacer(Modifier.height(10.dp))
        Text("自定义图标 · " + currentPack.label,
            style = LocalAiluaTheme.current.text.section, fontWeight = FontWeight.SemiBold)
        Text("点选 AILUA 应用，再从图标包中选择图标。",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = LocalAiluaTheme.current.text.caption)
        Spacer(Modifier.height(8.dp))
        if (editingKey != null) {
            val appKey = editingKey!!
            val appName = editableApps.firstOrNull { it.first == appKey }?.second ?: appKey
            val allIcons by produceState<List<AndroidIconEntry>>(
                emptyList(), resolver, currentPack.packageName
            ) {
                value = withContext(Dispatchers.IO) {
                    resolver.availableIcons(currentPack.packageName)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { editingKey = null }) { Text("‹ 应用列表") }
                Text(appName, style = LocalAiluaTheme.current.text.secondary, fontWeight = FontWeight.SemiBold)
            }
            if (allIcons.isEmpty()) {
                EmptyThemeMessage("此图标包没有提供图标列表", "自动映射仍可使用。")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.fillMaxWidth().height(370.dp).testTag("icon_pack_grid"),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(allIcons, key = { it.drawableName }) { icon ->
                        Column(Modifier.clickable {
                            val current = ThemeStore.selection
                            ThemeStore.update(current.copy(manualIconOverrides =
                                current.manualIconOverrides + (appKey to icon.drawableName)))
                            editingKey = null
                        }.padding(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            AndroidPackIcon(resolver, currentPack.packageName, icon.drawableName,
                                runtime, Modifier.size(43.dp))
                            Text(icon.displayName ?: icon.drawableName, style = LocalAiluaTheme.current.text.caption,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        } else {
            editableApps.forEach { (key, name) ->
                Row(Modifier.fillMaxWidth().clickable { editingKey = key }
                    .padding(vertical = 6.dp).testTag("manual_icon_app_" + key),
                    verticalAlignment = Alignment.CenterVertically) {
                    ResolvedPackIcon(context, key, selection, runtime, Modifier.size(34.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(name, modifier = Modifier.weight(1f), style = LocalAiluaTheme.current.text.secondary)
                    if (selection.manualIconOverrides.containsKey(key)) {
                        TextButton(onClick = {
                            ThemeStore.update(ThemeStore.selection.copy(manualIconOverrides =
                                ThemeStore.selection.manualIconOverrides - key))
                        }) { Text("恢复自动", style = LocalAiluaTheme.current.text.caption) }
                    }
                    Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ResolvedPackIcon(
    context: Context, key: String, selection: ThemeSelection,
    runtime: AiluaThemeRuntime, modifier: Modifier
) {
    val bitmap by produceState<android.graphics.Bitmap?>(
        null, context, key, selection
    ) {
        value = withContext(Dispatchers.IO) {
            IconResolver.get(context).resolveBitmap(key, selection, 96)
        }
    }
    if (bitmap != null) {
        androidx.compose.foundation.Image(bitmap!!.asImageBitmap(), key, modifier)
    } else {
        ThemePreviewIcon(runtime, key, 34.dp)
    }
}

@Composable
private fun AndroidPackIcon(
    resolver: AndroidIconPackResolver, packageName: String, drawableName: String,
    runtime: AiluaThemeRuntime, modifier: Modifier
) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, resolver, packageName, drawableName) {
        value = withContext(Dispatchers.IO) {
            resolver.loadBitmap(packageName, drawableName, 96)
        }
    }
    if (bitmap != null) {
        androidx.compose.foundation.Image(bitmap!!.asImageBitmap(), drawableName, modifier)
    } else {
        Box(modifier.clip(RoundedCornerShape(LocalAiluaTheme.current.shapes.small.dp))
            .background(runtime.palette.surfaceVariant))
    }
}

@Composable
private fun EmptyThemeMessage(title: String, message: String) {
    Text(title, fontWeight = FontWeight.SemiBold, style = LocalAiluaTheme.current.text.secondary)
    Spacer(Modifier.height(4.dp))
    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = LocalAiluaTheme.current.text.caption)
}

private fun formatTitle(format: ExternalThemeFormat): String = when (format) {
    ExternalThemeFormat.AILUA -> "AILUA 主题"
    ExternalThemeFormat.MIUI_MTZ -> "MIUI / HyperOS MTZ"
    ExternalThemeFormat.COLOROS_THEME -> "ColorOS 主题"
    ExternalThemeFormat.ANDROID_ICON_PACK -> "Android 图标包"
}

private fun displayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME),
        null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    } ?: uri.lastPathSegment

private fun readThemeBytes(context: Context, uri: Uri): ByteArray {
    val limit = 64 * 1024 * 1024
    val stream = context.contentResolver.openInputStream(uri)
        ?: throw IllegalArgumentException("无法打开主题文件")
    return stream.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > limit) throw IllegalArgumentException("主题文件超过 64 MB")
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    }
}


@Composable
internal fun ExternalWallpaperChoices(runtime: AiluaThemeRuntime, onPreview: ((ExternalThemePackage) -> Unit)? = null) {
    val themes = ExternalThemeRepository.themes.filter { it.wallpapers.isNotEmpty() }
    if (themes.isEmpty()) return
    Spacer(Modifier.height(10.dp))
    Text("已导入壁纸", style = LocalAiluaTheme.current.text.section,
        fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(7.dp))
    themes.forEach { theme ->
        val selected = ThemeStore.selection.wallpaperSourceId == theme.id
        val shape = RoundedCornerShape(LocalAiluaTheme.current.shapes.medium.dp)
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(shape)
            .background(LocalAiluaTheme.current.surfaces.inset)
            .border(1.dp, if (selected) runtime.palette.accent else Color.Transparent, shape)
            .clickable {
                if (onPreview != null) onPreview(theme)
                else ThemeStore.update(ThemeStore.selection.copy(wallpaperSourceId = theme.id))
            }.padding(8.dp).testTag("external_wallpaper_" + theme.id),
            verticalAlignment = Alignment.CenterVertically) {
            ExternalAssetImage(theme.wallpapers.first(), null, 120,
                Modifier.size(width = 48.dp, height = 42.dp)
                    .clip(RoundedCornerShape(LocalAiluaTheme.current.shapes.small.dp)))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(theme.name, fontWeight = FontWeight.SemiBold, style = LocalAiluaTheme.current.text.secondary)
                Text("本机壁纸", style = LocalAiluaTheme.current.text.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Default.Check, "已选",
                tint = runtime.palette.accent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun ExternalIconChoices(runtime: AiluaThemeRuntime) {
    val themes = ExternalThemeRepository.themes.filter { it.icons != null }
    if (themes.isEmpty()) return
    Spacer(Modifier.height(10.dp))
    Text("已导入图标", style = LocalAiluaTheme.current.text.section,
        fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(7.dp))
    themes.forEach { theme ->
        val selected = ThemeStore.selection.iconSourceOverrideId == "theme:" + theme.id
        val shape = RoundedCornerShape(LocalAiluaTheme.current.shapes.medium.dp)
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(shape)
            .background(LocalAiluaTheme.current.surfaces.inset)
            .border(1.dp, if (selected) runtime.palette.accent else Color.Transparent, shape)
            .clickable {
                ThemeStore.update(ThemeStore.selection.copy(
                    iconSourceOverrideId = "theme:" + theme.id,
                    manualIconOverrides = emptyMap()))
            }.padding(10.dp).testTag("external_icons_" + theme.id),
            verticalAlignment = Alignment.CenterVertically) {
            val sample = theme.icons?.allIcons?.firstOrNull()?.asset
                ?: theme.icons?.mappings?.values?.firstOrNull()
            if (sample != null) {
                ExternalAssetImage(sample, null, 96,
                    Modifier.size(35.dp).clip(RoundedCornerShape(LocalAiluaTheme.current.shapes.small.dp)))
            } else {
                ThemePreviewIcon(runtime, "chat", 35.dp)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(theme.name, fontWeight = FontWeight.SemiBold, style = LocalAiluaTheme.current.text.secondary)
                Text("保存在这台手机", style = LocalAiluaTheme.current.text.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Default.Check, "已选",
                tint = runtime.palette.accent, modifier = Modifier.size(18.dp))
        }
    }
}
