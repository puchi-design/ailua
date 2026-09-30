package com.example

import com.example.data.chat.local.ChatDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Regression coverage for chat and workspace schema upgrades.
 */
class ChatMigrationTest {

    @Test
    fun schemaVersionIsFour() {
        assertEquals(4, ChatDatabase.Schema.version)
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
            f.driver.execute(null, "DROP TABLE desktop_item", 0)
            f.driver.execute(null, "DROP TABLE desktop_page", 0)
            f.driver.execute(null, "PRAGMA user_version = 3", 0)
            ChatDatabase.Schema.migrate(f.driver, oldVersion = 3, newVersion = 4)
            val workspace = com.example.data.desktop.local.SqlDelightWorkspaceRepository(f.database)
            workspace.migrateIfEmpty(listOf("gallery"))
            assertEquals("gallery", workspace.snapshot().itemsFor("page_home").first().sourceId)
            assertEquals("迁移前", f.repository.getResolvedTurns(session.id).single().activeVariant?.content)
        } finally { f.driver.close() }
    }

}
