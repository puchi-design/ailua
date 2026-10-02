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

    // Navigation labels only; persisted IDs, routes and original search aliases stay intact.
    private val displayLabels = mapOf(
        "chat" to "消息", "gallery" to "相册", "check_phone" to "他的手机",
        "relations" to "关系", "companion_call" to "通话", "world_map" to "地点",
        "lore_books" to "世界书", "character_creation" to "角色工坊", "theater" to "剧场",
        "reality_bridge" to "现实连接",
    )

    private val libraryApps: List<AiluaApp> = MockData.appLibraryList
    private val libraryEntries: List<LauncherAppEntry> = libraryApps.map { app ->
        LauncherAppEntry(
            id = app.id,
            displayName = displayLabels[app.id] ?: app.name.substringBefore('·').trim().ifEmpty { app.name },
            iconKey = app.iconKey,
            category = app.category,
            route = app.route,
            status = app.status,
        )
    }
    private val systemEntries = listOf(
        LauncherAppEntry("apps", "应用", "apps", null, "apps", AppStatus.AVAILABLE),
        LauncherAppEntry("group_chat", "群聊", "chat", null, "group_chat", AppStatus.AVAILABLE),
        LauncherAppEntry("reality", "现实连接", "bridge", null, "reality", AppStatus.AVAILABLE),
        LauncherAppEntry("settings", "设置", "settings", null, "settings", AppStatus.AVAILABLE),
    )
    private val entriesById = (libraryEntries + systemEntries).associateBy { it.id }

    /** Resolves old persisted desktop source IDs without rewriting the database. */
    fun canonicalId(id: String): String = aliases[id] ?: id

    fun get(id: String): LauncherAppEntry? = entriesById[canonicalId(id)]

    fun label(id: String): String = when (id) {
        "messages" -> "消息"
        "call_history" -> "通话"
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
