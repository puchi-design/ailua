package com.example

import com.example.data.chat.local.ChatDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * P3D-1 schema migration: devices at schema 2 must upgrade to 3 by creating
 * only the extraction-cursor table, with all existing chat data untouched.
 */
class ChatMigrationTest {

    @Test
    fun schemaVersionIsThree() {
        assertEquals(3, ChatDatabase.Schema.version)
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
}
