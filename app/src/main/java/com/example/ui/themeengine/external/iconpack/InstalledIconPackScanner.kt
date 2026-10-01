package com.example.ui.themeengine.external.iconpack

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class InstalledIconPackInfo(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val appIcon: Drawable?
)

/** Finds visible installed ADW/Nova/other common launcher icon packs without QUERY_ALL_PACKAGES. */
class InstalledIconPackScanner(private val context: Context) {
    fun scan(): List<InstalledIconPackInfo> {
        val pm = context.packageManager
        val candidates = LinkedHashSet<String>()
        ACTIONS.forEach { action ->
            val intent = Intent(action)
            pm.queryIntentActivities(intent, 0).forEach { info ->
                info.activityInfo?.packageName?.let(candidates::add)
            }
        }
        val parser = AndroidIconPackParser(context)
        return candidates.mapNotNull { name ->
            if (parser.parse(name) == null) return@mapNotNull null
            try {
                val info = pm.getPackageInfo(name, 0)
                val app = info.applicationInfo ?: return@mapNotNull null
                InstalledIconPackInfo(
                    packageName = name,
                    label = app.loadLabel(pm).toString(),
                    versionName = info.versionName,
                    appIcon = runCatching { app.loadIcon(pm) }.getOrNull()
                )
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }.sortedBy { it.label.lowercase() }
    }

    companion object {
        val ACTIONS = listOf(
            "org.adw.ActivityStarter.THEMES",
            "com.novalauncher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.gau.go.launcherex.theme",
            "com.anddoes.launcher.THEME",
            "com.dlto.atom.launcher.THEME",
            "com.apusapps.launcher.THEME",
            "app.lawnchair.icons.THEMED_ICON"
        )
    }
}
