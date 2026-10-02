package com.example.ui.launcher

import com.example.data.mock.MockData
import com.example.data.model.AiluaApp
import com.example.data.model.AppStatus
import com.example.navigation.AppRouter

/** Static app identity shared by the drawer, workspace, hotseat and folders. */
data class LauncherAppEntry(
    val id: String,
    val displayName: String,
    val iconKey: String,
    val category: String?,
    val route: String?,
    val status: AppStatus,
)

object LauncherAppCatalog {
    private val aliases = mapOf(
        "messages" to "chat",
        "call_history" to "companion_call",
        "call" to "companion_call",
        "creator" to "character_creation",
        "character_creator" to "character_creation",
        "world_book" to "lore_books",
    )

    private val libraryApps: List<AiluaApp> = MockData.appLibraryList
    private val libraryEntries: List<LauncherAppEntry> = libraryApps.map { app ->
        LauncherAppEntry(
            id = app.id,
            displayName = app.name.substringBefore('·').trim().ifEmpty { app.name },
            iconKey = app.iconKey,
            category = app.category,
            route = app.route,
            status = app.status,
        )
    }
    private val systemEntries = listOf(
        LauncherAppEntry("apps", "应用", "apps", null, "apps", AppStatus.AVAILABLE),
        LauncherAppEntry("group_chat", "群聊", "chat", null, "group_chat", AppStatus.AVAILABLE),
        LauncherAppEntry("reality", "现实感知", "bridge", null, "reality", AppStatus.AVAILABLE),
        LauncherAppEntry("settings", "设置", "settings", null, "settings", AppStatus.AVAILABLE),
    )
    private val entriesById = (libraryEntries + systemEntries).associateBy { it.id }

    /** Resolves old persisted desktop source IDs without rewriting the database. */
    fun canonicalId(id: String): String = aliases[id] ?: id

    fun get(id: String): LauncherAppEntry? = entriesById[canonicalId(id)]

    fun label(id: String): String = when (id) {
        "messages" -> "消息"
        "call_history" -> "通话记录"
        else -> get(id)?.displayName ?: id
    }

    fun all(): List<LauncherAppEntry> = libraryEntries + systemEntries

    /** The drawer is a projection of the complete app library, regardless of placement. */
    fun drawerApps(): List<AiluaApp> = libraryApps

    fun canAddToHome(id: String): Boolean {
        val entry = get(id) ?: return false
        return entry.id in libraryAppsById &&
            entry.status in setOf(AppStatus.AVAILABLE, AppStatus.BETA) &&
            entry.route != null &&
            AppRouter.destinationOrNull(entry.id) != null
    }

    private val libraryAppsById = libraryApps.mapTo(mutableSetOf()) { it.id }
}
