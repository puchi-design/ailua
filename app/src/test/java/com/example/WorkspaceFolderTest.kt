package com.example

import com.example.data.desktop.DesktopContainer
import com.example.data.desktop.DesktopItemType
import com.example.data.desktop.DesktopPlacement
import com.example.data.desktop.local.SqlDelightWorkspaceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class WorkspaceFolderTest {
    @Test
    fun createAddReorderRenameAndRestartPreserveFolder(): Unit = runBlocking {
        val file = Files.createTempFile("ailua-folder", ".db").toFile()
        try {
            val first = ChatTestHarness.file(file)
            val repo = SqlDelightWorkspaceRepository(first.database)
            repo.migrateIfEmpty(emptyList())
            val created = repo.createFolder("app_gallery", "app_memories")
            val folder = created.items.single { it.type == DesktopItemType.FOLDER }
            assertEquals("app_memories", created.folderItems(folder.id)[0].id)
            assertEquals("app_gallery", created.folderItems(folder.id)[1].id)
            assertEquals(3, folder.cellX)
            assertEquals(0, folder.cellY)
            repo.addItemToFolder("app_diary", folder.id)
            repo.moveFolderItem("app_diary", folder.id, 0)
            repo.renameFolder(folder.id, "我的记忆")
            val nextPage = repo.createPage()
            repo.moveItem(folder.id, DesktopPlacement(DesktopContainer.WORKSPACE, nextPage.id, 0, 0))
            repo.moveItem("dock_moments", DesktopPlacement(DesktopContainer.WORKSPACE, folder.pageId, 1, 0))
            repo.moveItem(folder.id, DesktopPlacement(DesktopContainer.HOTSEAT, null, 1, 0, rank = 1))
            first.driver.close()

            val reopened = ChatTestHarness.file(file, createSchema = false)
            val restored = SqlDelightWorkspaceRepository(reopened.database).snapshot()
            assertEquals("我的记忆", restored.folder(folder.id)?.title)
            assertEquals(DesktopContainer.HOTSEAT, restored.folderItem(folder.id)?.container)
            assertEquals(listOf("app_diary", "app_memories", "app_gallery"),
                restored.folderItems(folder.id).map { it.id })
            assertTrue(restored.folderItems(folder.id).all {
                it.container == DesktopContainer.FOLDER && it.parentFolderId == folder.id && it.pageId == null
            })
            reopened.driver.close()
        } finally { file.delete() }
    }

    @Test
    fun removingChildAutomaticallyDissolvesAtOneAndRestoresTargetCell(): Unit = runBlocking {
        val f = ChatTestHarness.inMemory()
        try {
            val repo = SqlDelightWorkspaceRepository(f.database)
            repo.migrateIfEmpty(emptyList())
            val folder = repo.createFolder("app_gallery", "app_memories")
                .items.single { it.type == DesktopItemType.FOLDER }
            val after = repo.moveItemOutOfFolder("app_gallery",
                DesktopPlacement(DesktopContainer.WORKSPACE, folder.pageId, 1, 0))
            assertFalse(after.items.any { it.id == folder.id })
            assertTrue(after.folders.isEmpty())
            val survivor = after.items.single { it.id == "app_memories" }
            assertEquals(DesktopContainer.WORKSPACE, survivor.container)
            assertEquals(folder.pageId, survivor.pageId)
            assertEquals(folder.cellX, survivor.cellX)
            assertEquals(folder.cellY, survivor.cellY)
            assertEquals(null, survivor.parentFolderId)
        } finally { f.driver.close() }
    }

    @Test
    fun invalidMoveOutRollsBackEntireTransaction(): Unit = runBlocking {
        val f = ChatTestHarness.inMemory()
        try {
            val repo = SqlDelightWorkspaceRepository(f.database)
            repo.migrateIfEmpty(emptyList())
            val folder = repo.createFolder("app_gallery", "app_memories")
                .items.single { it.type == DesktopItemType.FOLDER }
            val before = repo.snapshot()
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repo.moveItemOutOfFolder("app_gallery",
                    DesktopPlacement(DesktopContainer.WORKSPACE, folder.pageId, 0, 0)) }
            }
            assertEquals(before, repo.snapshot())
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repo.addItemToFolder("app_memories", folder.id) }
            }
            assertEquals(before, repo.snapshot())
        } finally { f.driver.close() }
    }
}
