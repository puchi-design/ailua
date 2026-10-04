package com.example.ui.themecenter

import org.json.JSONObject
import java.io.FilterOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Schema-v1 archive writer paired with the existing AiluaThemeImporter. */
internal object AiluaThemeArchiveWriter {
    fun write(
        output: OutputStream,
        id: String,
        name: String,
        basePreset: String,
        palette: String,
        wallpaper: ByteArray,
        icons: Map<String, ByteArray>,
        iconStyle: String? = null,
        wallpaperStyle: String? = null,
    ) {
        require(icons.keys.all { it.matches(Regex("[a-z][a-z0-9_]*")) }) { "无效的图标名称" }
        require(icons.size <= 18) { "仅导出核心应用图标" }
        val extension = when {
            wallpaper.size >= 12 && String(wallpaper, 0, 4, Charsets.US_ASCII) == "RIFF" -> "webp"
            wallpaper.size >= 3 && wallpaper[0] == 0xff.toByte() && wallpaper[1] == 0xd8.toByte() -> "jpg"
            else -> "png"
        }
        val wallpaperPath = "wallpaper/current.$extension"
        val manifest = JSONObject().put("schema", 1).put("id", id).put("name", name).put("version", "1")
        val settings = JSONObject().put("basePreset", basePreset).put("palette", palette)
            .put("wallpaper", wallpaperPath).put("icons", JSONObject().put("path", "icons/"))
        iconStyle?.let { settings.put("iconStyle", it) }
        wallpaperStyle?.let { settings.put("wallpaperStyle", it) }
        val entries = linkedMapOf(
            "manifest.json" to manifest.toString().toByteArray(Charsets.UTF_8),
            "theme.json" to settings.toString().toByteArray(Charsets.UTF_8),
            wallpaperPath to wallpaper,
        ).apply { icons.forEach { (key, bytes) -> put("icons/$key.png", bytes) } }
        require(entries.values.all { it.size <= 12 * 1024 * 1024 } &&
            entries.values.sumOf { it.size.toLong() } <= 80L * 1024 * 1024) { "导出图片过大" }
        // The SAF caller owns its stream. Closing the ZIP releases its deflater only.
        val borrowed = object : FilterOutputStream(output) { override fun close() = flush() }
        ZipOutputStream(borrowed).use { archive ->
            entries.forEach { (path, bytes) ->
                archive.putNextEntry(ZipEntry(path))
                archive.write(bytes)
                archive.closeEntry()
            }
        }
    }
}
