package com.example

import com.example.navigation.AiluaDestinations
import com.example.navigation.AppRouter
import com.example.ui.launcher.LauncherAppCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherAppCatalogTest {
    @Test
    fun legacyDesktopIdsResolveToLibraryIdentity() {
        assertEquals("chat", LauncherAppCatalog.get("messages")?.id)
        assertEquals("companion_call", LauncherAppCatalog.get("call_history")?.id)
        assertEquals("character_creation", LauncherAppCatalog.get("creator")?.id)
        assertEquals("lore_books", LauncherAppCatalog.get("world_book")?.id)
        assertEquals("应用", LauncherAppCatalog.label("apps"))
    }

    @Test
    fun drawerShowsOnlyLaunchableAppsAndConnectsExistingRealityScreen() {
        assertTrue(LauncherAppCatalog.drawerApps().all { it.route != null })
        assertEquals(1, LauncherAppCatalog.drawerApps().count { it.id == "reality_bridge" })
        assertEquals("reality", LauncherAppCatalog.get("reality_bridge")?.route)
        assertEquals(AiluaDestinations.REALITY, AppRouter.resolve("reality_bridge"))
        assertTrue(LauncherAppCatalog.canAddToHome("reality_bridge"))
        assertEquals(1, LauncherAppCatalog.drawerApps().count { it.id == "notes" })
        assertEquals(AiluaDestinations.NOTES, AppRouter.resolve("notes"))
        assertTrue(LauncherAppCatalog.canAddToHome("notes"))
        assertFalse(LauncherAppCatalog.drawerApps().any { it.id == "agent_tasks" })
        assertFalse(LauncherAppCatalog.drawerApps().any { it.id == "world_3d" || it.id == "dreamscape" })
        assertTrue(LauncherAppCatalog.canAddToHome("world_map"))
        assertTrue(LauncherAppCatalog.canAddToHome("companion_call"))
        assertFalse(LauncherAppCatalog.canAddToHome("world_3d"))
        assertFalse(LauncherAppCatalog.canAddToHome("dreamscape"))
        assertFalse(LauncherAppCatalog.canAddToHome("apps"))
        assertFalse(LauncherAppCatalog.canAddToHome("unknown"))
    }

    @Test
    fun callLaunchPreservesDrawerBehavior() {
        assertEquals(AiluaDestinations.CALL_HISTORY, AppRouter.resolve("companion_call"))
        assertEquals("call/mira", AppRouter.launchRouteOrNull("companion_call", "mira"))
    }
}
