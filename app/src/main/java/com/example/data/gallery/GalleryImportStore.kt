package com.example.data.gallery

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import com.example.data.model.GalleryAsset
import com.example.data.model.GalleryAssetType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Owns the user's copies, so displaying a photo never depends on a Photo Picker grant. */
internal class GalleryImportStore(context: Context) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, "gallery/user_imports")
    private val index = AtomicFile(File(directory, "index.json"))

    fun load(): List<GalleryAsset> = readEntries().map { it.toAsset() }

    fun importPhoto(uri: Uri, title: String? = null): GalleryAsset {
        val previous = readEntries() // A damaged index must never be replaced with an empty one.
        ensureDirectory()
        val uuid = UUID.randomUUID().toString()
        val temporary = File(directory, "$uuid.part")
        val final = File(directory, "$uuid.img")
        try {
            val input = appContext.contentResolver.openInputStream(uri)
                ?: throw GalleryImportException("无法读取选中的照片")
            input.use { source ->
                FileOutputStream(temporary).use { target ->
                    val buffer = ByteArray(32 * 1024)
                    var bytes = 0L
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        bytes += count
                        if (bytes > MAX_IMAGE_BYTES) {
                            throw GalleryImportException("照片超过 40 MB，请选择较小的图片")
                        }
                        target.write(buffer, 0, count)
                    }
                    target.fd.sync()
                    if (bytes == 0L) throw GalleryImportException("选中的照片是空文件")
                }
            }
            if (!hasSupportedImageSignature(temporary)) {
                throw GalleryImportException("无法识别这张图片")
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temporary.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw GalleryImportException("无法识别这张图片")
            }
            if (!temporary.renameTo(final)) throw IOException("Could not finish photo copy")
            val displayTitle = title?.trim()?.takeIf { it.isNotEmpty() }
                ?: sourceDisplayName(uri)
                ?: "我的照片"
            val entry = Entry(
                id = "user_import_$uuid",
                fileName = final.name,
                title = displayTitle.take(MAX_TITLE_LENGTH),
                createdAtMillis = System.currentTimeMillis(),
            )
            try {
                writeEntries(listOf(entry) + previous)
            } catch (failure: Exception) {
                final.delete() // Never show a photo that was not committed to the index.
                throw failure
            }
            return entry.toAsset()
        } finally {
            temporary.delete()
        }
    }

    fun delete(importedId: String) {
        val previous = readEntries()
        val entry = previous.firstOrNull { it.id == importedId }
            ?: throw GalleryImportException("照片已经不存在")
        val image = File(directory, entry.fileName)
        val tombstone = File(directory, "${entry.fileName}.deleting")
        if (image.exists() && !image.renameTo(tombstone)) {
            throw IOException("Could not prepare photo deletion")
        }
        try {
            writeEntries(previous.filterNot { it.id == importedId })
        } catch (failure: Exception) {
            if (tombstone.exists()) tombstone.renameTo(image)
            throw failure
        }
        tombstone.delete()
    }

    private fun readEntries(): List<Entry> {
        if (!index.baseFile.exists() && !File(directory, "index.json.bak").exists()) return emptyList()
        val root = try {
            index.openRead().bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        } catch (error: Exception) {
            throw GalleryImportException("相册索引无法读取；为保护已有照片，已停止写入", error)
        }
        if (root.optInt("version", -1) != INDEX_VERSION) {
            throw GalleryImportException("相册索引版本无法识别；为保护已有照片，已停止写入")
        }
        val array = root.optJSONArray("photos")
            ?: throw GalleryImportException("相册索引内容无效；为保护已有照片，已停止写入")
        val entries = try {
            (0 until array.length()).map { position ->
                val value = array.getJSONObject(position)
                Entry(
                    id = value.getString("id"),
                    fileName = value.getString("fileName"),
                    title = value.getString("title"),
                    createdAtMillis = value.getLong("createdAtMillis"),
                ).also { entry ->
                    require(entry.id.startsWith("user_import_"))
                    require(FILE_NAME.matches(entry.fileName))
                    require(entry.title.length <= MAX_TITLE_LENGTH)
                }
            }.also { items -> require(items.map { it.id }.distinct().size == items.size) }
        } catch (error: Exception) {
            throw GalleryImportException("相册索引内容无效；为保护已有照片，已停止写入", error)
        }
        // A crash between staging a delete and committing the index is recoverable.
        entries.forEach { entry ->
            val image = File(directory, entry.fileName)
            val staged = File(directory, "${entry.fileName}.deleting")
            if (!image.exists() && staged.exists()) staged.renameTo(image)
        }
        return entries
    }

    private fun writeEntries(entries: List<Entry>) {
        ensureDirectory()
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(JSONObject().apply {
                put("id", entry.id)
                put("fileName", entry.fileName)
                put("title", entry.title)
                put("createdAtMillis", entry.createdAtMillis)
            })
        }
        val bytes = JSONObject().put("version", INDEX_VERSION).put("photos", array)
            .toString().toByteArray(Charsets.UTF_8)
        val output = index.startWrite()
        try {
            output.write(bytes)
            index.finishWrite(output)
        } catch (error: Exception) {
            index.failWrite(output)
            throw error
        }
    }

    private fun sourceDisplayName(uri: Uri): String? = try {
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column < 0) return@use null
                cursor.getString(column)?.substringBeforeLast('.')?.trim()?.takeIf { it.isNotEmpty() }
            }
    } catch (_: Exception) {
        null
    }

    private fun hasSupportedImageSignature(file: File): Boolean {
        val header = ByteArray(16)
        val length = file.inputStream().use { it.read(header) }
        if (length < 12) return false
        val png = header.sliceArray(0..7).contentEquals(byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        val jpeg = header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()
        val gif = String(header, 0, 6, Charsets.US_ASCII) in setOf("GIF87a", "GIF89a")
        val webp = String(header, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(header, 8, 4, Charsets.US_ASCII) == "WEBP"
        val isoImage = String(header, 4, 4, Charsets.US_ASCII) == "ftyp" &&
            String(header, 8, 4, Charsets.US_ASCII) in setOf("heic", "heix", "hevc", "hevx", "mif1", "msf1", "avif", "avis")
        return png || jpeg || gif || webp || isoImage
    }

    private fun ensureDirectory() {
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Could not create gallery directory")
    }

    private fun Entry.toAsset() = GalleryAsset(
        id = id,
        characterId = "user",
        type = GalleryAssetType.USER_IMPORTED,
        title = title,
        caption = "从手机相册导入",
        createdAtVirtualTime = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(createdAtMillis)),
        visualReference = "imported",
        uriString = Uri.fromFile(File(directory, fileName)).toString(),
        album = "我的导入",
    )

    private data class Entry(
        val id: String,
        val fileName: String,
        val title: String,
        val createdAtMillis: Long,
    )

    companion object {
        private const val INDEX_VERSION = 1
        private const val MAX_IMAGE_BYTES = 40L * 1024L * 1024L
        private const val MAX_TITLE_LENGTH = 64
        private val FILE_NAME = Regex("[0-9a-f-]{36}\\.img")
    }
}

internal class GalleryImportException(message: String, cause: Throwable? = null) : IOException(message, cause)
