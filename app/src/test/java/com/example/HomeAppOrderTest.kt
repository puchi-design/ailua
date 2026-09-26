package com.example

import com.example.ui.home.HomeAppOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HomeAppOrderTest {

    private val catalog = listOf("living", "clock", "memory", "bond", "mail", "moments")

    @Test
    fun `restores saved order from storage`() {
        val saved = listOf("bond", "living", "clock", "memory", "mail", "moments")
        assertEquals(saved, HomeAppOrder.normalize(saved, catalog))
    }

    @Test
    fun `drops ids that no longer exist`() {
        val saved = listOf("gone_app", "bond", "living", "removed")
        val normalized = HomeAppOrder.normalize(saved, catalog)
        assertEquals(listOf("bond", "living"), normalized.filter { it in saved })
        assertEquals(false, normalized.contains("gone_app"))
        assertEquals(false, normalized.contains("removed"))
        assertEquals(catalog.size, normalized.size)
    }

    @Test
    fun `appends new apps in catalog order`() {
        val saved = listOf("moments", "mail")
        val normalized = HomeAppOrder.normalize(saved, catalog)
        assertEquals(listOf("moments", "mail", "living", "clock", "memory", "bond"), normalized)
    }

    @Test
    fun `moves item from one index to another`() {
        val moved = HomeAppOrder.move(catalog, 0, 3)
        assertEquals(listOf("clock", "memory", "bond", "living", "mail", "moments"), moved)
        assertEquals(catalog.size, moved.size)
        assertEquals(catalog.toSet(), moved.toSet())
    }

    @Test
    fun `empty storage keeps default catalog order and rejects invalid moves`() {
        assertEquals(catalog, HomeAppOrder.normalize(emptyList(), catalog))
        assertEquals(catalog, HomeAppOrder.move(catalog, -1, 2))
        assertEquals(catalog, HomeAppOrder.move(catalog, 1, 99))
        assertEquals(catalog, HomeAppOrder.move(catalog, 2, 2))
        assertNotEquals(null, HomeAppOrder.move(catalog, 5, 0).first())
    }
}
