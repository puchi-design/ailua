package com.example

import com.example.data.mock.MockData
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
    fun drawerKeepsEntireLibraryButOnlyRoutedAvailableAppsCanBePlaced() {
        assertEquals(MockData.appLibraryList.map { it.id }, LauncherAppCatalog.drawerApps().map { it.id })
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
