package com.example.ui.apps

import com.example.data.model.AiluaApp
import com.example.ui.launcher.LauncherAppCatalog

internal const val APP_LIBRARY_ALL = "全部"

internal fun appLibraryCategories(apps: List<AiluaApp>): List<String> =
    listOf(APP_LIBRARY_ALL) + apps.map { it.category }.distinct()

internal fun appLibraryDisplayName(app: AiluaApp): String =
    LauncherAppCatalog.get(app.id)?.displayName ?: app.name.split("·").firstOrNull()?.trim()?.ifBlank { app.name } ?: app.name

internal fun appMatchesQuery(app: AiluaApp, query: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return true
    return appLibraryDisplayName(app).contains(trimmed, ignoreCase = true) ||
        app.name.contains(trimmed, ignoreCase = true) ||
        app.category.contains(trimmed, ignoreCase = true)
}

internal fun filterAppLibrary(
    apps: List<AiluaApp>,
    query: String,
    category: String
): List<AiluaApp> = apps.filter { app ->
    val matchesCategory = category == APP_LIBRARY_ALL || app.category == category
    matchesCategory && appMatchesQuery(app, query)
}
