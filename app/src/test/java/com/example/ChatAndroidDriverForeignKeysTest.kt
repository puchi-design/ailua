package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.db.SqlDriver
import com.example.data.chat.local.ChatDatabase
import com.example.data.chat.local.ChatDriverFactory
import com.example.data.chat.local.SqlDelightChatRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * ChatAndroidDriverForeignKeysTest — PASS 3C-3.1 Android-driver integration test.
 *
 * Uses the REAL ChatDriverFactory (not the JDBC test harness) against an
 * isolated database file to prove that AndroidSqliteDriver opens the database
 * with SQLite foreign-key enforcement ON:
 *
 *  - a valid session accepts turns (turn + variant rows written);
 *  - `appendUserTurn("ghost-session", ...)` MUST fail — the only possible
 *    failure source is the FK constraint, since ids are unique and the
 *    repository performs no pre-validation;
 *  - the failed write leaves zero orphan turn/variant rows (transaction
 *    rollback), and counts show only the originally valid rows;
 *  - the database file is deleted on cleanup.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatAndroidDriverForeignKeysTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "test-ailua-chat-fk.db"

    private lateinit var driver: SqlDriver
    private lateinit var database: ChatDatabase
    private lateinit var repository: SqlDelightChatRepository

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
        driver = ChatDriverFactory(context, databaseName).createDriver()
        database = ChatDatabase(driver)
        repository = SqlDelightChatRepository(database, FakeIdGenerator(), FakeClock())
    }

    @After
    fun tearDown() {
        driver.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun androidDriverEnforcesForeignKeysAndLeavesNoOrphans() {
        val session = repository.getOrCreatePrivateSession("mira")
        repository.appendUserTurn(session.id, "hello")

        assertEquals(1L, database.chatTurnQueries.countTurns().executeAsOne())
        assertEquals(1L, database.chatVariantQueries.countVariants().executeAsOne())
        assertEquals(1L, database.chatSessionQueries.countSessions().executeAsOne())

        assertThrows(Exception::class.java) {
            repository.appendUserTurn("ghost-session", "orphan")
        }

        assertEquals(1L, database.chatTurnQueries.countTurns().executeAsOne())
        assertEquals(1L, database.chatVariantQueries.countVariants().executeAsOne())
        assertEquals(1L, database.chatSessionQueries.countSessions().executeAsOne())
    }
}
