package com.example.ui.home.folder

import com.example.data.mock.MockData

/** Uses a shared app category for the initial name; the user can rename it. */
object FolderNameSuggester {
    private val aliases = mapOf(
        "messages" to "chat",
        "call_history" to "companion_call",
        "call" to "companion_call",
    )

    fun suggest(firstSourceId: String?, secondSourceId: String?): String? {
        val apps = MockData.appLibraryList.associateBy { it.id }
        val first = apps[aliases[firstSourceId] ?: firstSourceId]?.category
        val second = apps[aliases[secondSourceId] ?: secondSourceId]?.category
        return first?.takeIf { it.isNotBlank() && it == second }
    }
}
