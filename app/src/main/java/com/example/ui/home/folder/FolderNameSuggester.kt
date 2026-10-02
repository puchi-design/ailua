package com.example.ui.home.folder

import com.example.ui.launcher.LauncherAppCatalog

/** Uses a shared app category for the initial name; the user can rename it. */
object FolderNameSuggester {
    fun suggest(firstSourceId: String?, secondSourceId: String?): String? {
        val first = firstSourceId?.let(LauncherAppCatalog::get)?.category
        val second = secondSourceId?.let(LauncherAppCatalog::get)?.category
        return first?.takeIf { it.isNotBlank() && it == second }
    }
}
