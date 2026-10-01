package com.example.ui.themeengine.external

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

/** Bounded, in-memory ZIP reader shared by all local theme formats. */
class SafeThemeArchive private constructor(
    bytes: ByteArray,
    private val maxEntryBytes: Int,
    private val maxTotalBytes: Int,
    private val maxEntries: Int,
    private val budget: ExpansionBudget
) {
    constructor(
        bytes: ByteArray,
        maxEntryBytes: Int = 12 * 1024 * 1024,
        maxTotalBytes: Int = 80 * 1024 * 1024,
        maxEntries: Int = 2048
    ) : this(bytes, maxEntryBytes, maxTotalBytes, maxEntries, ExpansionBudget(maxTotalBytes))

    private val entries: Map<String, ByteArray> = buildMap {
        require(bytes.size <= maxTotalBytes) { "主题文件过大" }
        require(bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4b.toByte()) { "不是 ZIP 文件" }
        var entryCount = 0
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount++
                require(entryCount <= maxEntries) { "主题条目过多" }
                val path = entry.name.trimEnd('/').replace('\\', '/')
                require(isSafePath(path)) { "无效的主题路径" }
                if (!entry.isDirectory) {
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count <= 0) break
                        budget.expandedBytes += count
                        require(output.size() + count <= maxEntryBytes &&
                            budget.expandedBytes <= budget.maxBytes) { "主题展开量过大" }
                        output.write(buffer, 0, count)
                    }
                    require(!containsKey(path)) { "重复的主题路径" }
                    put(path, output.toByteArray())
                }
                zip.closeEntry()
            }
        }
    }

    fun listEntries(): List<String> = entries.keys.toList()
    fun readBytes(path: String): ByteArray? = entries[path]?.copyOf()
    fun copyEntry(path: String, output: java.io.OutputStream): Boolean {
        val bytes = entries[path] ?: return false
        output.write(bytes)
        return true
    }
    fun openNestedArchive(path: String): SafeThemeArchive? {
        val bytes = entries[path] ?: return null
        if (bytes.size < 2 || bytes[0] != 0x50.toByte() || bytes[1] != 0x4b.toByte()) return null
        return SafeThemeArchive(bytes, maxEntryBytes, maxTotalBytes, maxEntries, budget)
    }
    companion object {
        fun isSafePath(path: String): Boolean = path.isNotBlank() &&
            !path.startsWith('/') && !path.startsWith("//") && !Regex("^[A-Za-z]:").containsMatchIn(path) &&
            path.split('/').none { it.isBlank() || it == "." || it == ".." }
    }
}

private class ExpansionBudget(val maxBytes: Int) {
    var expandedBytes: Long = 0
}
