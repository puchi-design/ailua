package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.gallery.GalleryImportException
import com.example.data.gallery.GalleryImportStore
import com.example.data.model.GalleryAssetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class GalleryImportStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun importedImageSurvivesStoreRecreationAndOriginalBecomingUnavailable() {
        val source = imageFile("first")
        val imported = GalleryImportStore(context).importPhoto(Uri.fromFile(source), "周末照片")
        val ownedCopy = ownedCopy(imported.id)
        assertTrue(ownedCopy.exists())
        assertEquals(GalleryAssetType.USER_IMPORTED, imported.type)
        assertEquals("我的导入", imported.album)

        assertTrue(source.delete()) // Simulates an expired/removed Photo Picker source.
        val reopened = GalleryImportStore(context).load().single()
        assertEquals(imported.id, reopened.id)
        assertEquals("周末照片", reopened.title)
        assertEquals(imported.uriString, reopened.uriString)
        assertTrue(ownedCopy.length() > 0L)
    }

    @Test fun deletionRemovesOnlyTheRequestedOwnedCopyAndPersists() {
        val store = GalleryImportStore(context)
        val first = store.importPhoto(Uri.fromFile(imageFile("one")), "第一张")
        val second = store.importPhoto(Uri.fromFile(imageFile("two")), "第二张")
        val firstCopy = ownedCopy(first.id)
        val secondCopy = ownedCopy(second.id)

        store.delete(first.id)
        assertFalse(firstCopy.exists())
        assertTrue(secondCopy.exists())
        assertEquals(listOf(second.id), GalleryImportStore(context).load().map { it.id })
        assertThrows(GalleryImportException::class.java) { store.delete("ga_official_photo") }
        assertTrue(secondCopy.exists())
    }

    @Test fun invalidImageNeverPublishesAnEntryOrLeavesAStagingCopy() {
        val invalid = File(context.cacheDir, "invalid-gallery.png").apply { writeText("not an image") }
        val store = GalleryImportStore(context)
        assertThrows(GalleryImportException::class.java) {
            store.importPhoto(Uri.fromFile(invalid), "坏照片")
        }
        assertTrue(store.load().isEmpty())
        val directory = File(context.filesDir, "gallery/user_imports")
        assertTrue(directory.listFiles().orEmpty().none { it.name.endsWith(".part") || it.name.endsWith(".img") })
    }

    @Test fun damagedIndexStopsFurtherWritesWithoutDeletingSavedImage() {
        val store = GalleryImportStore(context)
        val imported = store.importPhoto(Uri.fromFile(imageFile("kept")), "保留")
        val ownedCopy = ownedCopy(imported.id)
        val index = File(context.filesDir, "gallery/user_imports/index.json")
        index.writeText("{broken")

        assertThrows(GalleryImportException::class.java) { GalleryImportStore(context).load() }
        assertThrows(GalleryImportException::class.java) {
            GalleryImportStore(context).importPhoto(Uri.fromFile(imageFile("new")), "新照片")
        }
        assertEquals("{broken", index.readText())
        assertTrue(ownedCopy.exists())
    }

    private fun imageFile(name: String): File = File(context.cacheDir, "gallery-$name.png").also { file ->
        val bitmap = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
        file.outputStream().use { stream ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
        bitmap.recycle()
    }

    private fun ownedCopy(id: String): File =
        File(context.filesDir, "gallery/user_imports/${id.removePrefix("user_import_")}.img")
}
