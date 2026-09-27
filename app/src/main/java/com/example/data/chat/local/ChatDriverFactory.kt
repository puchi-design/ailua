package com.example.data.chat.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * ChatDriverFactory — Android-only driver wiring for the chat database (P3C-3 §7).
 *
 * This is the deliberately "dirty" layer: `android.content.Context` is allowed
 * HERE and nowhere else in the chat runtime (P3C-3 §14). Domain models,
 * repository contract, and the SQL schema stay platform-neutral; the future
 * iOS/Wasm ports swap this factory for their native driver (§20 mapping table).
 *
 * SQLite defaults foreign-key enforcement to OFF per connection, so this factory
 * explicitly turns it ON for every opened database — chat integrity (no orphan
 * turns/variants, strict cascade) depends on it (P3C-3.1).
 *
 * Not wired into the ViewModel/UI yet — P3C-4 performs that integration.
 *
 * @param databaseName injectable so tests create an isolated database file
 * instead of touching the user's real chat database.
 */
class ChatDriverFactory(
    private val context: Context,
    private val databaseName: String = DEFAULT_DATABASE_NAME,
) {
    fun createDriver(): SqlDriver =
        AndroidSqliteDriver(
            schema = ChatDatabase.Schema,
            context = context,
            name = databaseName,
            callback = object : AndroidSqliteDriver.Callback(ChatDatabase.Schema) {
                override fun onConfigure(db: SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }
            },
        )

    companion object {
        const val DEFAULT_DATABASE_NAME: String = "ailua_chat.db"
    }
}
