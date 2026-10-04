package com.example.ui.themeengine.icon

import com.example.ui.themeengine.ThemeCatalog
import com.example.ui.themeengine.ThemeResolver
import com.example.ui.themeengine.ThemeSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledIconCatalogTest {
    @Test
    fun allSixOfficialPresetsSelectTheirOwnPackAndStylesRemainMixable() {
        val expected = mapOf(
            "default" to "default_icons",
            "soft_home" to "soft_home_icons",
            "rainy_study" to "rainy_study_icons",
            "sakura_diary" to "sakura_icons",
            "y2k_love" to "y2k_icons",
            "midnight_glass" to "midnight_icons",
        )
        expected.forEach { (preset, style) ->
            assertEquals(style, ThemeCatalog.byId(preset).iconStyleId)
            assertEquals(style, ThemeResolver.resolveIconStyleId(ThemeSelection(preset)))
        }
        assertEquals("y2k_icons", ThemeResolver.resolveIconStyleId(
            ThemeSelection("default", iconStyleOverrideId = "y2k_icons")))
        assertEquals("y2k_love", ThemeCatalog.byId("y2k_love").id)
        assertEquals("milk", ThemeCatalog.DEFAULT_ID)
    }

    @Test
    fun registryContainsSixCompletePacksWithoutGuessedUnknownKeys() {
        assertEquals(6, BundledIconCatalog.packs.size)
        assertEquals(18, BundledIconCatalog.iconKeys.size)
        val resources = BundledIconCatalog.packs.flatMap { pack ->
            BundledIconCatalog.iconKeys.map { key ->
                val resource = BundledIconCatalog.resource(pack.iconStyleId, key)!!
                assertEquals(resource, BundledIconCatalog.resourceForAssetKey(resource.asset.key))
                assertEquals(pack.iconStyleId == "y2k_icons", resource.isPixelArt)
                resource.drawableName
            }
        }
        assertEquals(108, resources.distinct().size)
        assertNull(BundledIconCatalog.resource("default_icons", "dream"))
        assertNull(BundledIconCatalog.resource("default_icons", "games"))
        assertNull(BundledIconCatalog.resourceForAssetKey("ti_default_unknown"))
        assertNull(BundledIconCatalog.resourceForAssetKey("../ti_default_chat"))
    }

    @Test
    fun persistedRoutesAndRealLauncherAliasesUseCanonicalIconAssets() {
        val aliases = mapOf(
            "messages" to "chat", "call_history" to "call", "companion_call" to "call",
            "world_map" to "world", "world_3d" to "world", "lore_books" to "lore",
            "world_book" to "lore", "character_creation" to "creator",
            "character_creator" to "creator", "agent" to "assistant", "agent_tasks" to "assistant",
        )
        aliases.forEach { (alias, key) ->
            assertEquals("ti_default_$key",
                BundledIconCatalog.resource("default_icons", alias)?.drawableName)
        }
        assertEquals(listOf("messages", "chat", "group_chat"), BundledIconCatalog.lookupKeys("messages"))
        assertEquals(AiluaIconAliasRegistry.aliasesFor("call"), AiluaIconAliasRegistry.aliasesFor("call_history"))
        assertEquals(AiluaIconAliasRegistry.aliasesFor("world"), AiluaIconAliasRegistry.aliasesFor("world_map"))
        assertEquals(AiluaIconAliasRegistry.aliasesFor("assistant"), AiluaIconAliasRegistry.aliasesFor("agent"))
    }

    @Test
    fun explicitStyleOverridesPresetAndLegacyStylesRetainVectorTreatment() {
        val selection = ThemeSelection("midnight_glass", iconStyleOverrideId = "soft_home_icons")
        assertEquals(listOf("ti_soft_chat", "ti_default_chat"),
            BundledIconCatalog.candidates("chat", selection).map { it.drawableName })
        for (legacy in listOf("milk", "glass", "diary", "mono", "identity")) {
            assertTrue(BundledIconCatalog.candidates("chat",
                ThemeSelection("default", iconStyleOverrideId = legacy)).isEmpty())
        }
        for (legacyPreset in listOf("milk", "glass", "diary", "mono")) {
            assertTrue(BundledIconCatalog.candidates("chat", ThemeSelection(legacyPreset)).isEmpty())
        }
        assertEquals(listOf("ti_default_chat"),
            BundledIconCatalog.candidates("chat", ThemeSelection("default")).map { it.drawableName })
        assertEquals(listOf("ti_midnight_chat", "ti_default_chat"),
            BundledIconCatalog.candidates("chat", ThemeSelection("midnight_glass", iconStyleOverrideId = "bad"))
                .map { it.drawableName })
        for ((preset, prefix) in listOf("rainy_study" to "rain", "sakura_diary" to "sakura")) {
            assertEquals(listOf("ti_${prefix}_chat", "ti_default_chat"),
                BundledIconCatalog.candidates("messages", ThemeSelection(preset)).map { it.drawableName })
        }
    }
}
