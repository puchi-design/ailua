package com.example

import com.example.data.desktop.CellRect
import com.example.data.desktop.DesktopContainer
import com.example.data.desktop.DesktopPage
import com.example.data.desktop.WorkspaceSeed
import com.example.data.desktop.local.SqlDelightWorkspaceRepository
import com.example.ui.design.launcher.layout.DragDirection
import com.example.ui.home.workspace.DropPlan
import com.example.ui.home.workspace.DropResolver
import com.example.ui.home.workspace.EdgePageAction
import com.example.ui.home.workspace.EdgePageController
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class WorkspaceCdTest {
    private fun accept(plan: DropPlan) = plan as DropPlan.Accept

    @Test fun edgeHoverTargetsOnlyOrdinaryPages() {
        val edge = EdgePageController
        assertEquals(EdgePageAction.NEXT, edge.action(399f, 400f, 0, 2, 32f, true))
        assertEquals(EdgePageAction.PREVIOUS, edge.action(1f, 400f, 1, 2, 32f, true))
        assertEquals(EdgePageAction.CREATE_PAGE, edge.action(399f, 400f, 1, 2, 32f, true))
        assertEquals(EdgePageAction.NONE, edge.action(399f, 400f, 2, 2, 32f, true))
        assertEquals(EdgePageAction.NONE, edge.action(200f, 400f, 0, 2, 32f, true))
        assertEquals(450L, EdgePageController.DWELL_MS)
    }

    @Test fun crossPageDropCreatesPageOnceAndSurvivesRestart(): Unit = runBlocking {
        val file = Files.createTempFile("ailua-cd-page", ".db").toFile()
        try {
            val first = ChatTestHarness.file(file)
            val repo = SqlDelightWorkspaceRepository(first.database)
            repo.migrateIfEmpty(emptyList())
            val initial = repo.snapshot()
            val gallery = initial.items.single { it.id == "app_gallery" }
            val page = DesktopPage("second", 1, false)
            val plan = accept(DropResolver.resolveDrop(initial.copy(pages = initial.pages + page), gallery,
                DesktopContainer.WORKSPACE, page.id, CellRect(2, 3), temporaryPage = page))
            val saved = repo.applyDrop(plan.commit)
            assertEquals(2, saved.pages.size)
            assertEquals(page.id, saved.items.single { it.id == gallery.id }.pageId)
            first.driver.close()

            val reopened = ChatTestHarness.file(file, createSchema = false)
            val restored = SqlDelightWorkspaceRepository(reopened.database).snapshot()
            assertEquals(2, restored.pages.size)
            assertEquals(2, restored.itemsFor(page.id).single().cellX)
            assertEquals(3, restored.itemsFor(page.id).single().cellY)
            assertEquals(13, restored.items.size)
            reopened.driver.close()
        } finally { file.delete() }
    }

    @Test fun lastEmptyPageIsReclaimedButHomeSurvives(): Unit = runBlocking {
        val db = ChatTestHarness.inMemory()
        try {
            val repo = SqlDelightWorkspaceRepository(db.database)
            repo.migrateIfEmpty(emptyList())
            val start = repo.snapshot()
            val gallery = start.items.single { it.id == "app_gallery" }
            val page = DesktopPage("second", 1, false)
            val out = accept(DropResolver.resolveDrop(start.copy(pages = start.pages + page), gallery,
                DesktopContainer.WORKSPACE, page.id, CellRect(0, 0), temporaryPage = page))
            repo.applyDrop(out.commit)
            val onSecond = repo.snapshot()
            val back = accept(DropResolver.resolveDrop(onSecond, onSecond.items.single { it.id == gallery.id },
                DesktopContainer.WORKSPACE, WorkspaceSeed.HOME_ID, CellRect(1, 0)))
            val restored = repo.applyDrop(back.commit)
            assertEquals(listOf(WorkspaceSeed.HOME_ID), restored.pages.map { it.id })
            assertEquals(13, restored.items.size)
            assertEquals(WorkspaceSeed.HOME_ID, restored.items.single { it.id == gallery.id }.pageId)
        } finally { db.driver.close() }
    }

    @Test fun cancelledPreviewDoesNotWritePageOrPosition(): Unit = runBlocking {
        val db = ChatTestHarness.inMemory()
        try {
            val repo = SqlDelightWorkspaceRepository(db.database)
            repo.migrateIfEmpty(emptyList())
            val before = repo.snapshot()
            val page = DesktopPage("transient", 1, false)
            val gallery = before.items.single { it.id == "app_gallery" }
            val plan = DropResolver.resolveDrop(before.copy(pages = before.pages + page), gallery,
                DesktopContainer.WORKSPACE, page.id, CellRect(0, 0), temporaryPage = page)
            assertTrue(plan is DropPlan.Accept)
            // Back / pointer cancellation discards the pure preview without calling applyDrop.
            assertEquals(before, repo.snapshot())
        } finally { db.driver.close() }
    }

    @Test fun collisionPushesNeighborAndCommitsWithoutOverlap(): Unit = runBlocking {
        val db = ChatTestHarness.inMemory()
        try {
            val repo = SqlDelightWorkspaceRepository(db.database)
            repo.migrateIfEmpty(emptyList())
            val start = repo.snapshot()
            val gallery = start.items.single { it.id == "app_gallery" }
            val plan = accept(DropResolver.resolveDrop(start, gallery, DesktopContainer.WORKSPACE,
                WorkspaceSeed.HOME_ID, CellRect(0, 0), DragDirection.RIGHT))
            val saved = repo.applyDrop(plan.commit)
            assertEquals(0, saved.items.single { it.id == "app_gallery" }.cellX)
            assertEquals(1, saved.items.single { it.id == "app_mailbox" }.cellX)
            assertEquals(13, saved.items.size)
        } finally { db.driver.close() }
    }

    @Test fun hotseatReorderSwapsCellsAndFullHotseatRejectsWorkspace(): Unit = runBlocking {
        val db = ChatTestHarness.inMemory()
        try {
            val repo = SqlDelightWorkspaceRepository(db.database)
            repo.migrateIfEmpty(emptyList())
            val start = repo.snapshot()
            val gallery = start.items.single { it.id == "app_gallery" }
            assertTrue(DropResolver.resolveDrop(start, gallery, DesktopContainer.HOTSEAT,
                null, CellRect(0, 0)) is DropPlan.Reject)
            val messages = start.items.single { it.id == "dock_messages" }
            val reordered = repo.applyDrop(accept(DropResolver.resolveDrop(start, messages,
                DesktopContainer.HOTSEAT, null, CellRect(2, 0))).commit)
            assertEquals(2, reordered.items.single { it.id == messages.id }.cellX)
            assertEquals(0, reordered.items.single { it.id == "dock_living" }.cellX)
            assertEquals(5, reordered.hotseatItems().size)
        } finally { db.driver.close() }
    }

    @Test fun hotseatToWorkspaceThenWorkspaceToEmptyHotseatSurvivesRestart(): Unit = runBlocking {
        val file = Files.createTempFile("ailua-cd-hotseat", ".db").toFile()
        try {
            val first = ChatTestHarness.file(file)
            val repo = SqlDelightWorkspaceRepository(first.database)
            repo.migrateIfEmpty(emptyList())
            val start = repo.snapshot()
            val messages = start.items.single { it.id == "dock_messages" }
            val movedOut = repo.applyDrop(accept(DropResolver.resolveDrop(start, messages,
                DesktopContainer.WORKSPACE, WorkspaceSeed.HOME_ID, CellRect(3, 3))).commit)
            assertEquals(4, movedOut.hotseatItems().size)
            assertEquals(WorkspaceSeed.HOME_ID, movedOut.items.single { it.id == messages.id }.pageId)
            val gallery = movedOut.items.single { it.id == "app_gallery" }
            val movedIn = repo.applyDrop(accept(DropResolver.resolveDrop(movedOut, gallery,
                DesktopContainer.HOTSEAT, null, CellRect(0, 0))).commit)
            assertEquals("app_gallery", movedIn.hotseatItems().single { it.cellX == 0 }.id)
            first.driver.close()

            val reopened = ChatTestHarness.file(file, createSchema = false)
            val restored = SqlDelightWorkspaceRepository(reopened.database).snapshot()
            assertEquals(5, restored.hotseatItems().size)
            assertEquals("app_gallery", restored.hotseatItems().single { it.cellX == 0 }.id)
            assertEquals(WorkspaceSeed.HOME_ID, restored.items.single { it.id == messages.id }.pageId)
            assertEquals(13, restored.items.size)
            reopened.driver.close()
        } finally { file.delete() }
    }
}
