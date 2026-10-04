package com.example.ui.themecenter

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.HideDialogStatusBar
import com.example.ui.themeengine.LocalAiluaTheme
import com.example.ui.themeengine.ThemeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** OEM importers remain asset adapters here, rather than consumer OEM runtime choices. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeLabSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val runtime = LocalAiluaTheme.current
    val scope = rememberCoroutineScope()
    var section by remember { mutableStateOf("overview") }
    var inspections by remember { mutableStateOf<List<ThemeAssetInspection>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            busy = true
            status = "正在导出主题…"
            val selection = ThemeStore.selection
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val output = context.contentResolver.openOutputStream(uri)
                            ?: error("无法写入所选文件")
                        output.use { ThemeLabTools.export(context, selection, runtime, it) }
                    }
                }.onSuccess { status = "已导出 AILUA 主题" }
                    .onFailure { status = it.message ?: "导出失败" }
                busy = false
            }
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
        containerColor = runtime.surfaces.raised,
        modifier = Modifier.testTag("theme_lab_sheet"),
    ) {
        ThemeSheetBackHandler {
            if (section != "overview") section = "overview" else onDismiss()
        }
        HideDialogStatusBar()
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = runtime.layout.screenHorizontalPadding.dp).padding(bottom = 28.dp)) {
            Text("Theme Lab", style = runtime.text.title, color = runtime.palette.onSurface)
            Text("本机主题生产工具", style = runtime.text.secondary, color = runtime.palette.onSurfaceMuted)
            if (section != "overview") {
                TextButton(onClick = { section = "overview" }, modifier = Modifier.testTag("theme_lab_back")) { Text("‹ Theme Lab") }
            }
            when (section) {
                "import" -> ImportThemeSection(runtime) { section = "installed" }
                "installed" -> { SectionLabel("已导入主题"); MyThemesSection(runtime) }
                "packs" -> IconPacksSection(runtime, showAdvanced = true)
                "inspect" -> {
                    Column(Modifier.testTag("theme_asset_inspector")) {
                        SectionLabel("Theme Asset Inspector")
                        inspections.forEach { asset ->
                            Text(asset.key, style = runtime.text.section, color = runtime.palette.onSurface)
                            Text(asset.source, style = runtime.text.caption, color = runtime.palette.onSurfaceMuted)
                            if (asset.isBitmap) {
                                Text("${asset.width} × ${asset.height} · ${asset.byteCount} bytes", style = runtime.text.secondary, color = runtime.palette.onSurface)
                                Text("SHA-256 ${asset.sha256}", style = runtime.text.caption, color = runtime.palette.onSurfaceMuted)
                            } else {
                                Text("由基础主题渲染；导出后随基础主题恢复", style = runtime.text.secondary, color = runtime.palette.onSurface)
                            }
                            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = runtime.surfaces.divider)
                        }
                    }
                }
                else -> {
                    Spacer(Modifier.height(20.dp))
                    OutlinedButton(onClick = { section = "import" }, modifier = Modifier.fillMaxWidth().testTag("theme_lab_import")) { Text("导入外部主题") }
                    Text("支持 AILUA、MIUI / HyperOS、ColorOS", style = runtime.text.caption, color = runtime.palette.onSurfaceMuted)
                    Text("OEM importer 只适配壁纸、图标、预览和元数据。", style = runtime.text.caption, color = runtime.palette.onSurfaceMuted)
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(onClick = { section = "packs" }, modifier = Modifier.fillMaxWidth().testTag("theme_lab_icon_packs")) { Text("已安装 Android 图标包") }
                    OutlinedButton(onClick = { section = "installed" }, modifier = Modifier.fillMaxWidth().testTag("theme_lab_installed")) { Text("已导入主题") }
                    OutlinedButton(onClick = {
                        busy = true
                        status = "正在读取主题素材…"
                        scope.launch {
                            runCatching { ThemeLabTools.inspect(context, ThemeStore.selection, runtime) }
                                .onSuccess { inspections = it; section = "inspect"; status = null }
                                .onFailure { status = it.message ?: "检查失败" }
                            busy = false
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("theme_lab_inspect")) { Text("Theme Asset Inspector") }
                    OutlinedButton(onClick = { exporter.launch("AILUA-${ThemeStore.selection.themePresetId}.ailuatheme") },
                        enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("theme_lab_export")) { Text("Export AILUA Theme") }
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            status?.let { Text(it, style = runtime.text.secondary, color = runtime.palette.onSurfaceMuted, modifier = Modifier.padding(top = 12.dp)) }
        }
    }
}
