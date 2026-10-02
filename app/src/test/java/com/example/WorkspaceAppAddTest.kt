package com.example

import com.example.data.desktop.DesktopContainer
import com.example.data.desktop.DesktopItemType
import com.example.data.desktop.local.SqlDelightWorkspaceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class WorkspaceAppAddTest {
    @Test fun drawerAddUsesPreferredPageRejectsAliasesAndSurvivesRestart(): Unit = runBlocking {
        val file = Files.createTempFile("ailua-drawer-add", ".db").toFile()
        try {
            val first = ChatTestHarness.file(file)
            val repo = SqlDelightWorkspaceRepository(first.database)
            repo.migrateIfEmpty(emptyList())
            val preferred = repo.createPage()
            val result = requireNotNull(repo.addAppToWorkspace("world_map", preferred.id))
            val added = result.items.single { it.type == DesktopItemType.APP && it.sourceId == "world_map" }
            assertEquals(DesktopContainer.WORKSPACE, added.container)
            assertEquals(preferred.id, added.pageId)
            assertNull(repo.addAppToWorkspace("world_map", preferred.id))
            assertNull(repo.addAppToWorkspace("chat", preferred.id)) // legacy messages in the dock
            assertNull(repo.addAppToWorkspace("companion_call", preferred.id)) // legacy call_history
            assertNull(repo.addAppToWorkspace("dreamscape", preferred.id)) // unavailable preview
            first.driver.close()

            val reopened = ChatTestHarness.file(file, createSchema = false)
            val restored = SqlDelightWorkspaceRepository(reopened.database).snapshot()
            assertTrue(restored.items.any { it.id == added.id && it.pageId == preferred.id })
            reopened.driver.close()
        } finally { file.delete() }
    }
}
