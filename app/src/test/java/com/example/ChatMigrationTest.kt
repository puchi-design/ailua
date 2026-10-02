package com.example

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.data.chat.local.ChatDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Regression coverage for chat and workspace schema upgrades.
 */
class ChatMigrationTest {

    @Test
    fun schemaVersionIsEight() {
        assertEquals(8, ChatDatabase.Schema.version)
    }

    @Test
    fun migratingFromV7CreatesNotificationHistoryWithoutChangingChatOrWorkspace() {
        val f = ChatTestHarness.inMemory()
        try {
            val session = f.repository.getOrCreatePrivateSession("mira")
            f.repository.appendUserTurn(session.id, "通知升级前的聊天")
            val workspace = com.example.data.desktop.local.SqlDelightWorkspaceRepository(f.database)
            workspace.migrateIfEmpty(listOf("gallery"))
            val before = workspace.snapshot()
            f.driver.execute(null, "DROP TABLE virtual_notification", 0)
            ChatDatabase.Schema.migrate(f.driver, oldVersion = 7, newVersion = 8)
            assertEquals(emptyList<com.example.data.chat.local.Virtual_notification>(), f.database.virtualNotificationQueries.selectActiveNotifications().executeAsList())
            assertEquals(before, workspace.snapshot())
            assertEquals("通知升级前的聊天", f.repository.getResolvedTurns(session.id).single().activeVariant?.content)
        } finally { f.driver.close() }
    }

    @Test
    fun migratingFromV2CreatesCursorTableAndKeepsData() {
        val f = ChatTestHarness.inMemory()
        val session = f.repository.getOrCreatePrivateSession("mira")
        f.repository.appendUserTurn(session.id, "你好")

        // Simulate a device still at schema 2: drop the P3D-1 table.
        f.driver.execute(null, "DROP TABLE memory_extract_cursor", 0)
        f.driver.execute(null, "PRAGMA user_version = 2", 0)

        ChatDatabase.Schema.migrate(f.driver, oldVersion = 2, newVersion = 3)

        f.database.memoryExtractCursorQueries.upsertMemoryExtractCursor(
            cursor_key = "mira:${session.id}",
            last_position = 1L,
            updated_at_epoch_ms = 1L,
        )
        val row = f.database.memoryExtractCursorQueries
            .selectMemoryExtractCursor("mira:${session.id}")
            .executeAsOneOrNull()
        assertNotNull(row)
        assertEquals(1L, row!!.last_position)

        val turns = f.repository.getResolvedTurns(session.id)
        assertEquals(1, turns.size)
        assertEquals("你好", turns.single().activeVariant?.content)
    }
    @Test
    fun migratingFromV3AddsWorkspaceTablesWithoutChangingChat() {
        val f = ChatTestHarness.inMemory()
        try {
            val session = f.repository.getOrCreatePrivateSession("mira")
            f.repository.appendUserTurn(session.id, "迁移前")
            f.driver.execute(null, "DROP TABLE desktop_folder", 0)
            f.driver.execute(null, "DROP TABLE workspace_widget_state", 0)
            f.driver.execute(null, "DROP TABLE desktop_item", 0)
            f.driver.execute(null, "DROP TABLE desktop_page", 0)
            f.driver.execute(null, "PRAGMA user_version = 3", 0)
            ChatDatabase.Schema.migrate(f.driver, oldVersion = 3, newVersion = 7)
            val workspace = com.example.data.desktop.local.SqlDelightWorkspaceRepository(f.database)
            workspace.migrateIfEmpty(listOf("gallery"))
            assertEquals("gallery", workspace.snapshot().itemsFor("page_home").first().sourceId)
            assertEquals("迁移前", f.repository.getResolvedTurns(session.id).single().activeVariant?.content)
        } finally { f.driver.close() }
    }

    @Test
    fun migratingFromV4ConvertsDockRowsToHotseatWithoutChangingChat() {
        val f = ChatTestHarness.inMemory()
        try {
            val session = f.repository.getOrCreatePrivateSession("mira")
            f.repository.appendUserTurn(session.id, "仍在这里")
            val workspace = com.example.data.desktop.local.SqlDelightWorkspaceRepository(f.database)
            workspace.migrateIfEmpty(emptyList())
            f.driver.execute(null, "UPDATE desktop_item SET container = 'DOCK' WHERE id = 'dock_messages'", 0)
            f.driver.execute(null, "PRAGMA user_version = 4", 0)
            ChatDatabase.Schema.migrate(f.driver, oldVersion = 4, newVersion = 5)
            assertEquals("HOTSEAT", f.database.desktopItemQueries.selectAllItems().executeAsList()
                .single { it.id == "dock_messages" }.container)
            assertEquals("仍在这里", f.repository.getResolvedTurns(session.id).single().activeVariant?.content)
        } finally { f.driver.close() }
    }

    @Test
    fun migratingFromV5AddsWidgetSeedMarkerWithoutChangingChat() {
        val f = ChatTestHarness.inMemory()
        try {
            val session = f.repository.getOrCreatePrivateSession("mira")
            f.repository.appendUserTurn(session.id, "组件升级")
            f.driver.execute(null, "DROP TABLE workspace_widget_state", 0)
            f.driver.execute(null, "PRAGMA user_version = 5", 0)
            ChatDatabase.Schema.migrate(f.driver, oldVersion = 5, newVersion = 6)
            val workspace = com.example.data.desktop.local.SqlDelightWorkspaceRepository(f.database)
            workspace.migrateIfEmpty(emptyList())
            workspace.seedDefaultWidgetsOnce()
            assertEquals(2, workspace.snapshot().items.count {
                it.type == com.example.data.desktop.DesktopItemType.AILUA_WIDGET })
            assertEquals("组件升级", f.repository.getResolvedTurns(session.id).single().activeVariant?.content)
        } finally { f.driver.close() }
    }

    @Test
    fun migratingFromV6AddsFolderStorageWithoutChangingWorkspaceRows() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            driver.execute(null, "CREATE TABLE desktop_page (id TEXT NOT NULL PRIMARY KEY, rank INTEGER NOT NULL, is_home INTEGER NOT NULL DEFAULT 0)", 0)
            driver.execute(null, "CREATE TABLE desktop_item (id TEXT NOT NULL PRIMARY KEY, item_type TEXT NOT NULL, source_id TEXT NOT NULL, container TEXT NOT NULL, page_id TEXT, cell_x INTEGER NOT NULL, cell_y INTEGER NOT NULL, span_x INTEGER NOT NULL DEFAULT 1, span_y INTEGER NOT NULL DEFAULT 1, rank INTEGER NOT NULL DEFAULT 0, locked INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(page_id) REFERENCES desktop_page(id) ON DELETE CASCADE)", 0)
            driver.execute(null, "INSERT INTO desktop_page VALUES ('page_home', 0, 1)", 0)
            driver.execute(null, "INSERT INTO desktop_item VALUES ('app_gallery', 'APP', 'gallery', 'WORKSPACE', 'page_home', 2, 3, 1, 1, 7, 0)", 0)
            ChatDatabase.Schema.migrate(driver, oldVersion = 6, newVersion = 7)
            val state = com.example.data.desktop.local.SqlDelightWorkspaceRepository(ChatDatabase(driver)).snapshot()
            assertEquals(2, state.items.single().cellX)
            assertEquals(3, state.items.single().cellY)
            assertEquals(null, state.items.single().parentFolderId)
            assertEquals(emptyList<com.example.data.desktop.DesktopFolder>(), state.folders)
        } finally { driver.close() }
    }

}
