package com.example

import com.example.data.desktop.CellRect
import com.example.data.desktop.DesktopContainer
import com.example.data.desktop.DesktopItemType
import com.example.data.desktop.DesktopPage
import com.example.data.desktop.WidgetPlacement
import com.example.data.desktop.local.SqlDelightWorkspaceRepository
import com.example.ui.home.workspace.DropPlan
import com.example.ui.home.workspace.DropResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class WorkspaceWidgetTest {
    @Test fun defaultWidgetsRepackAppsWithoutOverlapAndDoNotReseedAfterDeletion(): Unit = runBlocking {
        val file = Files.createTempFile("ailua-widget-seed", ".db").toFile()
        try {
            val first = ChatTestHarness.file(file)
            val repo = SqlDelightWorkspaceRepository(first.database)
            repo.migrateIfEmpty(emptyList())
            repo.seedDefaultWidgetsOnce()
            val state = repo.snapshot()
            assertEquals(2, state.items.count { it.type == DesktopItemType.AILUA_WIDGET })
            assertEquals(13, state.items.count { it.type == DesktopItemType.APP })
            assertEquals(3, state.itemsFor("page_home").first { it.id == "app_mailbox" }.cellY)
            repo.deleteWidget("widget_world_clock_001")
            repo.deleteWidget("widget_character_living_001")
            first.driver.close()
            val reopened = ChatTestHarness.file(file, createSchema = false)
            val restored = SqlDelightWorkspaceRepository(reopened.database)
            restored.seedDefaultWidgetsOnce()
            assertTrue(restored.snapshot().items.none { it.type == DesktopItemType.AILUA_WIDGET })
            reopened.driver.close()
        } finally { file.delete() }
    }

    @Test fun addTwoInstancesResizeAndCrossPagePersist(): Unit = runBlocking {
        val file = Files.createTempFile("ailua-widget-move", ".db").toFile()
        try {
            val first = ChatTestHarness.file(file)
            val repo = SqlDelightWorkspaceRepository(first.database)
            repo.migrateIfEmpty(emptyList())
            repo.seedDefaultWidgetsOnce()
            val next = repo.createPage()
            val added = repo.addWidget("memory_echo", next.id, 2, 2)!!
            val second = repo.addWidget("memory_echo", next.id, 2, 2)!!
            val instances = second.items.filter { it.sourceId == "memory_echo" }
            assertEquals(2, instances.size)
            assertEquals(2, instances.map { it.id }.distinct().size)
            val clock = added.items.first { it.sourceId == "world_clock" }
            val resized = DropResolver.resolveResize(second, clock, 2, 1) as DropPlan.Accept
            val afterResize = repo.applyDrop(resized.commit)
            assertEquals(2, afterResize.items.first { it.id == clock.id }.spanX)
            val plan = DropResolver.resolveDrop(afterResize,
                afterResize.items.first { it.id == clock.id }, DesktopContainer.WORKSPACE,
                next.id, CellRect(0, 2, 2, 1)) as DropPlan.Accept
            repo.applyDrop(plan.commit)
            first.driver.close()
            val reopened = ChatTestHarness.file(file, createSchema = false)
            val restored = SqlDelightWorkspaceRepository(reopened.database).snapshot()
            assertEquals(next.id, restored.items.first { it.id == clock.id }.pageId)
            assertEquals(2, restored.items.first { it.id == clock.id }.spanX)
            assertEquals(2, restored.items.count { it.sourceId == "memory_echo" })
            reopened.driver.close()
        } finally { file.delete() }
    }

    @Test fun widgetCannotEnterHotseatAndFullPageRejectsLargeSize() {
        val f = ChatTestHarness.inMemory()
        try {
            val repo = SqlDelightWorkspaceRepository(f.database)
            repo.migrateIfEmpty(emptyList())
            repo.seedDefaultWidgetsOnce()
            val state = repo.snapshot()
            val clock = state.items.first { it.sourceId == "world_clock" }
            assertTrue(DropResolver.resolveDrop(state, clock, DesktopContainer.HOTSEAT,
                null, CellRect(0, 0)) is DropPlan.Reject)
            assertFalse(WidgetPlacement.supports("world_clock", 4, 2))
            assertTrue(DropResolver.resolveResize(state, clock, 4, 2) is DropPlan.Reject)
        } finally { f.driver.close() }
    }
}
