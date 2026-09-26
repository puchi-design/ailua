package com.example

import com.example.data.mock.MockData
import com.example.ui.apps.APP_LIBRARY_ALL
import com.example.ui.apps.appLibraryCategories
import com.example.ui.apps.appLibraryDisplayName
import com.example.ui.apps.filterAppLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLibraryFilterTest {

    private val apps get() = MockData.appLibraryList

    @Test
    fun `categories come from real metadata and start with all`() {
        val categories = appLibraryCategories(apps)
        assertEquals(APP_LIBRARY_ALL, categories.first())
        assertEquals(apps.map { it.category }.distinct(), categories.drop(1))
        assertEquals(categories.size, categories.distinct().size)
        assertTrue(categories.contains("核心伴生"))
        assertTrue(categories.contains("虚拟空间"))
    }

    @Test
    fun `app ids are stable and unique`() {
        assertTrue(apps.isNotEmpty())
        assertEquals(apps.size, apps.map { it.id }.distinct().size)
        val requiredIds = listOf("chat", "living", "mailbox", "theater", "world_map")
        requiredIds.forEach { id ->
            assertTrue("missing stable id $id", apps.any { it.id == id })
        }
    }

    @Test
    fun `search matches chinese name`() {
        val result = filterAppLibrary(apps, "通讯", APP_LIBRARY_ALL)
        assertEquals(listOf("chat"), result.map { it.id })
    }

    @Test
    fun `search matches english name segment`() {
        val result = filterAppLibrary(apps, "messages", APP_LIBRARY_ALL)
        assertEquals(listOf("chat"), result.map { it.id })
        val result2 = filterAppLibrary(apps, "Contacts", APP_LIBRARY_ALL)
        assertEquals(listOf("contacts"), result2.map { it.id })
    }

    @Test
    fun `search matches category`() {
        val result = filterAppLibrary(apps, "虚拟空间", APP_LIBRARY_ALL)
        assertEquals(3, result.size)
        assertTrue(result.all { it.category == "虚拟空间" })
        assertTrue(result.map { it.id }.containsAll(listOf("world_map", "world_3d", "dreamscape")))
    }

    @Test
    fun `category filter narrows the grid`() {
        val result = filterAppLibrary(apps, "", "核心伴生")
        assertEquals(11, result.size)
        assertTrue(result.all { it.category == "核心伴生" })
    }

    @Test
    fun `query and category combine`() {
        val result = filterAppLibrary(apps, "记忆", "核心伴生")
        assertEquals(listOf("memories"), result.map { it.id })
        val wrongCategory = filterAppLibrary(apps, "记忆", "虚拟空间")
        assertTrue(wrongCategory.isEmpty())
    }

    @Test
    fun `empty query keeps every app in the category`() {
        val all = filterAppLibrary(apps, "   ", APP_LIBRARY_ALL)
        assertEquals(apps.size, all.size)
    }

    @Test
    fun `unknown query and unknown category yield empty results`() {
        assertTrue(filterAppLibrary(apps, "zzz不存在的应用", APP_LIBRARY_ALL).isEmpty())
        assertTrue(filterAppLibrary(apps, "", "不存在的分类").isEmpty())
    }

    @Test
    fun `display name strips the english suffix`() {
        val chat = apps.first { it.id == "chat" }
        assertEquals("通讯", appLibraryDisplayName(chat))
        val living = apps.first { it.id == "living" }
        assertEquals("生活", appLibraryDisplayName(living))
    }
}
