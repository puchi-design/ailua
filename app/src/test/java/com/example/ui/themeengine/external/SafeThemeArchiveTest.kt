package com.example.ui.themeengine.external

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SafeThemeArchiveTest {
    @Test fun unsafePathsAreRejected() {
        listOf("../escape.png", "/absolute.png", "C:/absolute.png", "icons\\..\\escape.png").forEach { path ->
            assertRejects { SafeThemeArchive(zip(path to byteArrayOf(1))) }
        }
    }

    @Test fun expandedSizeAndNonZipBytesAreRejected() {
        assertRejects { SafeThemeArchive(zip("wallpaper/large.png" to ByteArray(16)), maxEntryBytes = 8) }
        assertRejects {
            SafeThemeArchive(zip("first.png" to ByteArray(300), "second.png" to ByteArray(300)), maxTotalBytes = 500)
        }
        assertRejects { SafeThemeArchive("not a zip".toByteArray()) }
    }

    @Test fun nestedZipIsReadableAndReturnedBytesAreCopies() {
        val nested = zip("icons/com.android.contacts.png" to byteArrayOf(1, 2, 3))
        val outer = SafeThemeArchive(zip("icons" to nested))
        outer.readBytes("icons")!![0] = 0
        assertArrayEquals(nested, outer.readBytes("icons"))
        assertEquals(listOf("icons/com.android.contacts.png"), outer.openNestedArchive("icons")!!.listEntries())
    }

    @Test fun directoriesCountTowardEntryLimitAndNestedArchivesShareExpansionBudget() {
        val directories = zip(
            "a/" to byteArrayOf(), "b/" to byteArrayOf(), "icons/icon.png" to byteArrayOf(1)
        )
        assertRejects { SafeThemeArchive(directories, maxEntries = 2) }

        val nested = zip("icons/large.png" to ByteArray(1000))
        val outer = zip("icons" to nested)
        val archive = SafeThemeArchive(outer, maxTotalBytes = outer.size + 10)
        assertRejects { archive.openNestedArchive("icons") }
    }
    private fun assertRejects(action: () -> Unit) {
        var rejected = false
        try { action() } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue("expected unsafe archive to be rejected", rejected)
    }

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().use { output ->
        ZipOutputStream(output).use { archive ->
            entries.forEach { (name, bytes) ->
                archive.putNextEntry(ZipEntry(name))
                archive.write(bytes)
                archive.closeEntry()
            }
        }
        output.toByteArray()
    }
}