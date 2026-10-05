package com.example.ui.themeengine.external

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExternalThemeRepositoryRebindTest {
    @Test
    fun reopeningWithAnotherAppStorageRootReadsOnlyThatRoot() {
        val application = ApplicationProvider.getApplicationContext<Context>()
        val sandbox = File(application.cacheDir, "theme-rebind-${UUID.randomUUID()}")
        val firstFiles = File(sandbox, "first")
        val secondFiles = File(sandbox, "second")
        val first = isolatedContext(application, firstFiles)
        val second = isolatedContext(application, secondFiles)
        val id = "rebind-${UUID.randomUUID()}"

        try {
            ExternalThemeRepository.initialize(first)
            ExternalThemeRepository.install(ThemeImportPreview(
                ExternalThemePackage(id, ExternalThemeFormat.AILUA, "Rebind test"), emptyMap()
            ))
            assertNotNull(ExternalThemeRepository.get(id))

            ExternalThemeRepository.initialize(second)
            assertNull(ExternalThemeRepository.get(id))

            ExternalThemeRepository.initialize(first)
            assertNotNull(ExternalThemeRepository.get(id))
            assertTrue(ExternalThemeRepository.delete(id))
        } finally {
            ExternalThemeRepository.initialize(application)
            assertTrue(sandbox.deleteRecursively())
        }
    }

    private fun isolatedContext(application: Context, files: File): Context =
        object : ContextWrapper(application) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = files.also { it.mkdirs() }
        }
}
