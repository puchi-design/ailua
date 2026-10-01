package com.example.ui.themeengine.external

import com.example.ui.themeengine.external.ailua.AiluaThemeImporter
import com.example.ui.themeengine.external.coloros.ColorOsThemeImporter
import com.example.ui.themeengine.external.mtz.MtzThemeImporter

/** Selects a local importer from ZIP contents and returns normalized assets. */
object UnifiedThemeImporter {
    fun inspect(source: ImportedThemeSource): ThemeImportPreview {
        val archive = SafeThemeArchive(source.bytes)
        val format = detectThemeFormat(source.filename, source.bytes, archive.listEntries())
            ?: throw IllegalArgumentException("无法识别主题格式")
        return when (format) {
            ExternalThemeFormat.AILUA -> AiluaThemeImporter().parse(source, archive)
            ExternalThemeFormat.MIUI_MTZ -> MtzThemeImporter().parse(source, archive)
            ExternalThemeFormat.COLOROS_THEME -> ColorOsThemeImporter().parse(source, archive)
            ExternalThemeFormat.ANDROID_ICON_PACK -> throw IllegalArgumentException("请在已安装图标包中选择")
        }
    }
}
