package com.example.ui.themeengine.icon

import android.graphics.Bitmap
import android.util.LruCache

/** Small in-memory cache shared by desktop, dock, theme preview, and icon picker. */
class IconBitmapCache(maxBytes: Int = 8 * 1024 * 1024) {
    private val bitmaps = object : LruCache<String, Bitmap>(maxBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    @Synchronized
    fun get(packageName: String, drawableName: String, targetSizePx: Int): Bitmap? =
        bitmaps.get(key(packageName, drawableName, targetSizePx))

    @Synchronized
    fun put(packageName: String, drawableName: String, targetSizePx: Int, bitmap: Bitmap) {
        bitmaps.put(key(packageName, drawableName, targetSizePx), bitmap)
    }

    @Synchronized
    fun clear() = bitmaps.evictAll()

    private fun key(packageName: String, drawableName: String, targetSizePx: Int) =
        "$packageName/$drawableName@$targetSizePx"
}
