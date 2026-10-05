package com.example.ui.themeengine.external

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Private app storage for normalized theme assets. Install uses a temporary directory. */
class ThemeAssetStore(context: Context) {
    val root: File = File(context.applicationContext.filesDir, "themes").also { it.mkdirs() }
    private val idPattern = Regex("[A-Za-z0-9_-][A-Za-z0-9._-]*")
    private fun validId(id: String) = idPattern.matches(id)
    private fun directChild(name: String): File = File(root, name).also { child ->
        val canonical = child.canonicalFile
        require(canonical.parentFile == root.canonicalFile && canonical.name == name) {
            "无效的主题目录"
        }
    }

    @Synchronized
    fun install(preview: ThemeImportPreview): ExternalThemePackage {
        val theme = preview.theme
        require(validId(theme.id)) { "无效的主题 ID" }
        val temporary = directChild(".${theme.id}-${System.nanoTime()}.tmp")
        require(temporary.mkdirs()) { "无法创建主题目录" }
        try {
            for ((relativePath, bytes) in preview.assets) {
                require(relativePath.startsWith("${theme.id}/"))
                val inside = relativePath.removePrefix("${theme.id}/")
                require(SafeThemeArchive.isSafePath(inside))
                require(inside.substringBefore('/') in setOf("wallpaper", "icons", "preview", "raw"))
                val destination = File(temporary, inside)
                require(destination.canonicalPath.startsWith(temporary.canonicalPath + File.separator))
                destination.parentFile?.mkdirs()
                destination.writeBytes(bytes)
            }
            File(temporary, "manifest.json").writeText(encodeManifest(theme).toString(), Charsets.UTF_8)
            val target = directChild(theme.id)
            val previous = directChild(".${theme.id}.previous")
            if (previous.exists()) previous.deleteRecursively()
            if (target.exists() && !renameDirectory(target, previous)) error("无法替换旧主题")
            if (!renameDirectory(temporary, target)) {
                if (previous.exists()) renameDirectory(previous, target)
                error("无法完成主题安装")
            }
            if (previous.exists()) previous.deleteRecursively()
            return theme
        } finally {
            if (temporary.exists()) temporary.deleteRecursively()
        }
    }

    /** Windows/Robolectric can briefly hold a just-written directory; keep the atomic move. */
    private fun renameDirectory(source: File, target: File): Boolean {
        repeat(5) { attempt ->
            if (source.renameTo(target)) return true
            if (attempt < 4) Thread.sleep(20L * (attempt + 1))
        }
        return false
    }

    fun loadAll(): List<ExternalThemePackage> = root.listFiles().orEmpty()
        .filter { it.isDirectory && validId(it.name) }
        .mapNotNull { directory ->
            try {
                if (directChild(directory.name).canonicalFile != directory.canonicalFile) return@mapNotNull null
                decodeManifest(JSONObject(File(directory, "manifest.json").readText(Charsets.UTF_8)))
                    .takeIf { it.id == directory.name }
            }
            catch (_: Exception) { null }
        }

    fun read(ref: ThemeAssetRef): ByteArray? = when (ref) {
        is ThemeAssetRef.LocalFile -> {
            val path = ref.relativePath
            if (!SafeThemeArchive.isSafePath(path)) null else {
                val file = File(root, path)
                if (file.canonicalPath.startsWith(root.canonicalPath + File.separator) && file.isFile) {
                    file.takeIf { it.length() <= 12L * 1024 * 1024 }?.readBytes()
                } else null
            }
        }
        else -> null
    }

    @Synchronized
    fun delete(id: String): Boolean {
        if (!validId(id)) return false
        val target = runCatching { directChild(id) }.getOrNull() ?: return false
        return !target.exists() || target.deleteRecursively()
    }

    private fun encodeRef(ref: ThemeAssetRef): JSONObject = when (ref) {
        is ThemeAssetRef.LocalFile -> JSONObject().put("kind", "local").put("path", ref.relativePath)
        is ThemeAssetRef.BuiltIn -> JSONObject().put("kind", "builtin").put("key", ref.key)
        is ThemeAssetRef.InstalledAndroidResource -> JSONObject().put("kind", "android")
            .put("package", ref.packageName).put("drawable", ref.drawableName)
    }
    private fun decodeRef(json: JSONObject): ThemeAssetRef? = when (json.optString("kind")) {
        "local" -> json.optString("path").takeIf { SafeThemeArchive.isSafePath(it) }?.let(ThemeAssetRef::LocalFile)
        "builtin" -> ThemeAssetRef.BuiltIn(json.optString("key"))
        "android" -> ThemeAssetRef.InstalledAndroidResource(json.optString("package"), json.optString("drawable"))
        else -> null
    }
    private fun refs(values: List<ThemeAssetRef>): JSONArray = JSONArray().also { array ->
        values.forEach { array.put(encodeRef(it)) }
    }
    private fun readRefs(array: JSONArray?): List<ThemeAssetRef> = (0 until (array?.length() ?: 0))
        .mapNotNull { index -> array?.optJSONObject(index)?.let(::decodeRef) }

    private fun encodeManifest(theme: ExternalThemePackage): JSONObject = JSONObject()
        .put("schema", 1).put("id", theme.id).put("sourceFormat", theme.format.name)
        .put("name", theme.name).put("author", theme.author).put("version", theme.version)
        .put("description", theme.description).put("basePresetId", theme.basePresetId)
        .put("preferredPaletteId", theme.preferredPaletteId)
        .put("wallpapers", refs(theme.wallpapers)).put("previewAssets", refs(theme.previewAssets))
        .put("metadata", JSONObject(theme.metadata)).put("icons", theme.icons?.let { set ->
            JSONObject().put("id", set.id).put("name", set.name)
                .put("mappings", JSONObject().also { map ->
                    set.mappings.forEach { (key, ref) -> map.put(key, encodeRef(ref)) }
                })
                .put("allIcons", JSONArray().also { array ->
                    set.allIcons.forEach { icon ->
                        array.put(JSONObject().put("key", icon.key).put("displayName", icon.displayName)
                            .put("asset", encodeRef(icon.asset)))
                    }
                })
        })

    private fun decodeManifest(json: JSONObject): ExternalThemePackage {
        val id = json.getString("id")
        require(validId(id))
        val format = ExternalThemeFormat.valueOf(json.getString("sourceFormat"))
        val iconJson = json.optJSONObject("icons")
        val icons = iconJson?.let { objectJson ->
            val mapJson = objectJson.optJSONObject("mappings") ?: JSONObject()
            val mapping = mapJson.keys().asSequence().mapNotNull { key ->
                mapJson.optJSONObject(key)?.let(::decodeRef)?.let { key to it }
            }.toMap()
            val entries = objectJson.optJSONArray("allIcons")
            ExternalIconSet(objectJson.optString("id", id), objectJson.optString("name", "Icons"),
                com.example.ui.themeengine.IconSource.ThemePackage(id), mapping,
                (0 until (entries?.length() ?: 0)).mapNotNull { index ->
                    entries?.optJSONObject(index)?.let { item ->
                        item.optJSONObject("asset")?.let(::decodeRef)?.let { ref ->
                            ExternalIconEntry(item.optString("key"), item.optString("displayName"), ref)
                        }
                    }
                })
        }
        val metaJson = json.optJSONObject("metadata") ?: JSONObject()
        val metadata = metaJson.keys().asSequence().associateWith { metaJson.optString(it) }
        return ExternalThemePackage(id, format, json.getString("name"),
            json.optString("author").takeIf { it.isNotBlank() },
            json.optString("version").takeIf { it.isNotBlank() },
            json.optString("description").takeIf { it.isNotBlank() },
            readRefs(json.optJSONArray("previewAssets")), readRefs(json.optJSONArray("wallpapers")),
            icons, metadata, json.optString("basePresetId").takeIf { it.isNotBlank() },
            json.optString("preferredPaletteId").takeIf { it.isNotBlank() })
    }
}
