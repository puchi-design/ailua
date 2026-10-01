package com.example.ui.themeengine.external

/** Prefer content over extension, since SAF providers often supply generic names. */
fun detectThemeFormat(
    filename: String?,
    firstBytes: ByteArray,
    archiveEntries: List<String>? = null
): ExternalThemeFormat? {
    if (firstBytes.size < 2 || firstBytes[0] != 0x50.toByte() || firstBytes[1] != 0x4b.toByte()) return null
    val entries = archiveEntries.orEmpty().map { it.lowercase() }.toSet()
    val hasInfo = entries.any { it.substringAfterLast('/') == "themeinfo.xml" }
    val hasAilua = "manifest.json" in entries && "theme.json" in entries
    val hasMtz = "description.xml" in entries ||
        (entries.any { it == "icons" || it.startsWith("icons/") } &&
            entries.any { it.startsWith("preview/") || it.startsWith("wallpaper/") })
    if (hasInfo) return ExternalThemeFormat.COLOROS_THEME
    if (hasAilua) return ExternalThemeFormat.AILUA
    if (hasMtz) return ExternalThemeFormat.MIUI_MTZ
    return when (filename?.substringAfterLast('.', "")?.lowercase()) {
        "ailuatheme" -> ExternalThemeFormat.AILUA
        "mtz" -> ExternalThemeFormat.MIUI_MTZ
        "theme" -> ExternalThemeFormat.COLOROS_THEME
        else -> null
    }
}
