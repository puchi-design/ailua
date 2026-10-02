package com.example

import com.example.data.desktop.WorkspaceSeed
import com.example.data.desktop.local.SqlDelightWorkspaceRepository
import org.junit.Assert.*
import org.junit.Test

class WorkspaceMigrationTest {
    @Test fun legacyOrderBecomesCellsAndKeepsDockDistinct() {
        val state = WorkspaceSeed.fromLegacyOrder(listOf("gallery", "mailbox", "gallery", "removed"))
        val icons = state.itemsFor(WorkspaceSeed.HOME_ID)
        assertEquals("gallery", icons[0].sourceId)
        assertEquals(0, icons[0].cellX)
        assertEquals("mailbox", icons[1].sourceId)
        assertEquals(1, icons[1].cellX)
        assertEquals(5, state.dockItems().size)
        assertEquals(state.items.size, state.items.map { it.id }.distinct().size)
    }

    @Test fun seedingExistingDatabaseDoesNotOverwriteMovedCell() {
        val db = ChatTestHarness.inMemory()
        try {
            val repo = SqlDelightWorkspaceRepository(db.database)
            repo.migrateIfEmpty(listOf("gallery"))
            val first = repo.snapshot()
            db.database.desktopItemQueries.updatePlacement("WORKSPACE", WorkspaceSeed.HOME_ID, null,
                3, 5, 1, 1, 0, "app_gallery")
            repo.migrateIfEmpty(listOf("mailbox"))
            val restored = repo.snapshot()
            assertEquals(first.items.size, restored.items.size)
            assertEquals(3, restored.items.single { it.id == "app_gallery" }.cellX)
            assertEquals(5, restored.items.single { it.id == "app_gallery" }.cellY)
        } finally { db.driver.close() }
    }
}
