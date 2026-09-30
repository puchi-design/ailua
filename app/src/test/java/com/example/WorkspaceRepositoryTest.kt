package com.example

import com.example.data.desktop.DesktopContainer
import com.example.data.desktop.WorkspaceSeed
import com.example.data.desktop.local.SqlDelightWorkspaceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class WorkspaceRepositoryTest {
    @Test fun committedLayoutSurvivesDatabaseRestartAndRejectsOverlap(): Unit = runBlocking {
        val file = Files.createTempFile("ailua-workspace", ".db").toFile()
        try {
            val firstDb = ChatTestHarness.file(file)
            val repo = SqlDelightWorkspaceRepository(firstDb.database)
            repo.migrateIfEmpty(listOf("mailbox", "gallery"))
            val gallery = repo.snapshot().items.single { it.id == "app_gallery" }
            repo.moveItem(gallery.id, gallery.placement().copy(cellX = 3, cellY = 3))
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repo.moveItem(gallery.id, gallery.placement().copy(cellX = 0, cellY = 0)) }
            }
            firstDb.driver.close()

            val reopened = ChatTestHarness.file(file, createSchema = false)
            val restored = SqlDelightWorkspaceRepository(reopened.database).snapshot()
            assertEquals(3, restored.items.single { it.id == "app_gallery" }.cellX)
            assertEquals(3, restored.items.single { it.id == "app_gallery" }.cellY)
            assertEquals(5, restored.items.count { it.container == DesktopContainer.HOTSEAT })
            reopened.driver.close()
        } finally { file.delete() }
    }

    @Test fun pageOperationsAndEmptyOnlyDeletion(): Unit = runBlocking {
        val db = ChatTestHarness.inMemory()
        try {
            val repo = SqlDelightWorkspaceRepository(db.database)
            repo.migrateIfEmpty(emptyList())
            val second = repo.createPage()
            val third = repo.createPage()
            repo.reorderPages(listOf(third.id, WorkspaceSeed.HOME_ID, second.id))
            assertEquals(third.id, repo.snapshot().pages.first().id)
            repo.setHomePage(third.id)
            assertTrue(repo.snapshot().pages.first().isHome)
            repo.deletePage(second.id)
            assertEquals(2, repo.snapshot().pages.size)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repo.deletePage(WorkspaceSeed.HOME_ID) }
            }
        } finally { db.driver.close() }
    }
}
