package com.example

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.gallery.GalleryImportStore
import com.example.data.notes.MemoRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Uses unique temporary names under the target APK's device-protected storage, then removes them. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29, maxSdkVersion = 29)
@SmallTest
class P5VRealAppsStorageSmokeTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test
    fun galleryImportOwnsItsCopyAfterSourceDisappearsAndCanDeleteIt() {
        val testContext = isolatedDirectBootContext()
        val sandbox = File(testContext.cacheDir, "gallery-storage-smoke-${UUID.randomUUID()}")
        assertTrue(sandbox.canonicalPath.startsWith(testContext.cacheDir.canonicalPath + File.separator))
        val privateFiles = File(sandbox, "files")
        assertTrue(privateFiles.mkdirs())
        val isolatedContext = object : ContextWrapper(testContext) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = privateFiles
        }
        val source = File(sandbox, "selected.png")

        try {
            val bitmap = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
            try {
                source.outputStream().use { stream ->
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                }
            } finally {
                bitmap.recycle()
            }

            // A file URI still passes through the device's real ContentResolver.
            val imported = GalleryImportStore(isolatedContext).importPhoto(Uri.fromFile(source), "真机照片")
            val ownedCopy = File(
                privateFiles,
                "gallery/user_imports/${imported.id.removePrefix("user_import_")}.img",
            )
            assertTrue(ownedCopy.isFile)
            assertTrue(ownedCopy.length() > 0L)
            assertTrue(source.delete())

            val reopened = GalleryImportStore(isolatedContext).load().single()
            assertEquals(imported.id, reopened.id)
            assertEquals("真机照片", reopened.title)
            testContext.contentResolver.openInputStream(Uri.parse(reopened.uriString)).use { stream ->
                assertNotNull(BitmapFactory.decodeStream(stream))
            }

            GalleryImportStore(isolatedContext).delete(imported.id)
            assertTrue(GalleryImportStore(isolatedContext).load().isEmpty())
            assertFalse(ownedCopy.exists())
        } finally {
            assertTrue("Could not remove isolated gallery test files", sandbox.deleteRecursively())
        }
    }

    @Test
    fun memoSaveSearchEditAndDeleteSurviveRepositoryRecreation() = runBlocking {
        val testContext = isolatedDirectBootContext()
        val preferenceName = "memo-storage-smoke-${UUID.randomUUID()}"
        val preferences = testContext.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)

        try {
            val saved = requireNotNull(MemoRepository(preferences, now = { 100L }).save(
                id = null,
                title = "周末计划",
                body = "买花，再写一封信",
            ))
            val reopened = MemoRepository(testContext.getSharedPreferences(preferenceName, Context.MODE_PRIVATE),
                now = { 200L })
            assertEquals(saved.id, reopened.search("写一封信").single().id)
            assertTrue(reopened.search("不存在").isEmpty())

            val edited = requireNotNull(reopened.save(saved.id, "周末计划", "先去买咖啡"))
            assertEquals(saved.id, edited.id)
            assertEquals(100L, edited.createdAt)
            assertEquals(200L, edited.updatedAt)
            assertEquals("先去买咖啡", MemoRepository(preferences).search("咖啡").single().body)

            assertTrue(MemoRepository(preferences).delete(saved.id))
            assertTrue(MemoRepository(preferences).notes.value.isEmpty())
        } finally {
            assertTrue("Could not remove isolated memo test preferences",
                testContext.deleteSharedPreferences(preferenceName))
        }
    }

    private fun isolatedDirectBootContext(): Context {
        val testContext = instrumentation.targetContext.createDeviceProtectedStorageContext()
        assertTrue("Locked-device smoke must use device-protected storage",
            testContext.isDeviceProtectedStorage)
        assertEquals(instrumentation.targetContext.packageName, testContext.packageName)
        assertTrue("Test data must be outside AILUA's normal credential-protected storage",
            testContext.dataDir.canonicalPath != instrumentation.targetContext.dataDir.canonicalPath)
        return testContext
    }
}
