package com.example

import com.example.ui.home.HomeWidgetId
import com.example.ui.home.WidgetRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetRegistryTest {

    @Test
    fun `default widget order stays stable`() {
        assertEquals(
            listOf("world_clock", "character_living", "memory_echo", "bond"),
            WidgetRegistry.defaultOrder.map { it.stableId }
        )
    }

    @Test
    fun `stable widget ids are unique`() {
        val ids = HomeWidgetId.entries.map { it.stableId }
        assertEquals(4, ids.size)
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `every default widget resolves to a registered spec`() {
        WidgetRegistry.defaultOrder.forEach { id ->
            assertNotNull("missing spec for ${id.stableId}", WidgetRegistry.spec(id.stableId))
        }
        assertEquals(WidgetRegistry.defaultOrder.size, HomeWidgetId.entries.size)
    }

    @Test
    fun `missing widget id falls back safely`() {
        assertNull(WidgetRegistry.spec("ghost_widget"))
        val fallback = WidgetRegistry.resolve("ghost_widget")
        assertEquals(WidgetRegistry.fallbackId, fallback.id)
        assertEquals("world_clock", fallback.id.stableId)
        assertNotNull(fallback.title)
        assertNotEquals("", fallback.title)
    }

    @Test
    fun `known widget id resolves to itself`() {
        assertEquals(HomeWidgetId.BOND, WidgetRegistry.resolve("bond").id)
        assertEquals(HomeWidgetId.MEMORY_ECHO, WidgetRegistry.resolve("memory_echo").id)
        assertEquals(HomeWidgetId.CHARACTER_LIVING, WidgetRegistry.resolve("character_living").id)
    }
}
