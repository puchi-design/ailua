package com.example.ui.themecenter

import com.example.ui.themeengine.ThemeSelection

/** Static catalog content. Previews never read a user's workspace, character or world state. */
data class ThemePreviewFixture(
    val selection: ThemeSelection,
    val time: String = "9:41",
    val date: String = "10月3日 · 周六",
    val weather: String = "小雨",
    val characterName: String = "沈砚",
    val location: String = "旧书店",
    val status: String = "在书店整理新到的旧书",
    val quote: String = "雨有点大。书先替你留着。",
) {
    val apps: List<ThemePreviewApp> = listOf(
        ThemePreviewApp("chat", "消息"),
        ThemePreviewApp("living", "生活"),
        ThemePreviewApp("moments", "动态"),
        ThemePreviewApp("gallery", "相册"),
    )

    val dockApps: List<ThemePreviewApp> = apps + ThemePreviewApp("apps", "应用库")
}

data class ThemePreviewApp(val iconKey: String, val name: String)
