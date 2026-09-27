package com.example

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.data.chat.local.ChatDatabase
import com.example.data.chat.local.SqlDelightChatRepository
import com.example.data.chat.repository.EpochClock
import com.example.data.chat.repository.IdGenerator
import java.io.File

/** Deterministic id source: sequential ids, with the ability to force chosen values. */
internal class FakeIdGenerator : IdGenerator {
    private var counter = 0
    val forcedIds = ArrayDeque<String>()

    override fun newId(): String = forcedIds.removeFirstOrNull() ?: "id-${++counter}"
}

/** Freezable clock so tests control every timestamp deterministically. */
internal class FakeClock(var now: Long = 1_700_000_000_000L) : EpochClock {
    override fun nowEpochMs(): Long = now
}

/**
 * ChatTestHarness — shared SQLite fixture for the P3C-3 persistence tests.
 *
 * Uses the JDBC test driver in memory (or file-backed for restart tests),
 * enables `PRAGMA foreign_keys = ON` so FK atomicity is really enforced, and
 * creates the schema exactly once per fresh database.
 */
internal class ChatTestHarness private constructor(
    val driver: JdbcSqliteDriver,
    val database: ChatDatabase,
    val idGenerator: FakeIdGenerator,
    val clock: FakeClock,
) {
    val repository = SqlDelightChatRepository(database, idGenerator, clock)

    companion object {
        /** Fresh in-memory database with FK enforcement ON. */
        fun inMemory(): ChatTestHarness = create(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY), createSchema = true)

        /** File-backed database; pass createSchema=false to reopen an existing file. */
        fun file(file: File, createSchema: Boolean = true): ChatTestHarness =
            create(JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}"), createSchema)

        private fun create(driver: JdbcSqliteDriver, createSchema: Boolean): ChatTestHarness {
            driver.execute(null, "PRAGMA foreign_keys = ON", 0)
            if (createSchema) {
                ChatDatabase.Schema.create(driver)
            }
            return ChatTestHarness(driver, ChatDatabase(driver), FakeIdGenerator(), FakeClock())
        }
    }
}
