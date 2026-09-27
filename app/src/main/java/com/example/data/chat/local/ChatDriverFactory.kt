package com.example.data.chat.local

import android.content.Context
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
 * Not wired into the ViewModel/UI yet — P3C-4 performs that integration.
 */
class ChatDriverFactory(private val context: Context) {
    fun createDriver(): SqlDriver =
        AndroidSqliteDriver(ChatDatabase.Schema, context, "ailua_chat.db")
}
