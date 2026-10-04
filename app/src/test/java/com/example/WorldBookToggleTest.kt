package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.runtime.WorldChatPromptContext
import com.example.data.local.AiluaLocalStore
import com.example.data.mock.WorldData
import com.example.data.model.CharacterCard
import com.example.data.model.CharacterCardData
import com.example.data.model.LoreActivationMode
import com.example.data.model.LoreEntry
import com.example.data.model.WorldBook
import com.example.data.registry.CharacterRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorldBookToggleTest {
    @Test
    fun officialLoreSwitchPersistsAndChangesChatActivation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("ailua_os_store", Context.MODE_PRIVATE)
        val previous = prefs.getString(AiluaLocalStore.KEY_WORLD_LORE_ENABLED_OVERRIDES, null)
        try {
            prefs.edit().remove(AiluaLocalStore.KEY_WORLD_LORE_ENABLED_OVERRIDES).commit()
            AiluaLocalStore.init(context)
            AiluaLocalStore.loadFromDisk()
            assertTrue(CharacterRegistry.isUnmodifiedBuiltIn("mira"))
            assertTrue(WorldData.configurableOfficialLoreEntryIds.contains("six_lore_mira_work"))
            assertFalse(WorldData.configurableOfficialLoreEntryIds.contains("six_lore_girls_circle"))

            fun activatedIds() = WorldChatPromptContext.activeLore(
                characterId = "mira", locationId = "place_moonlight",
                recentUserText = "苏晚宁 月光书阁", lifeEventTitle = null,
            ).map { it.entry.id }.toSet()

            assertTrue("six_lore_moonlight" in activatedIds())
            assertTrue("six_lore_mira_work" in activatedIds())
            assertTrue(AiluaLocalStore.setWorldLoreEnabled("six_lore_moonlight", false))
            assertTrue(AiluaLocalStore.setWorldLoreEnabled("six_lore_mira_work", false))
            assertFalse("six_lore_moonlight" in activatedIds())
            assertFalse("six_lore_mira_work" in activatedIds())
            assertTrue(WorldData.defaultWorldBook.entries.first { it.id == "six_lore_moonlight" }.enabled)

            val customId = "worldbook_toggle_custom"
            val previousCustom = AiluaLocalStore.customCards.value.firstOrNull { it.data.id == customId }
            try {
                CharacterRegistry.saveCharacterCard(CharacterCard(data = CharacterCardData(
                    id = customId,
                    name = "自定义读者",
                    characterBook = WorldBook("custom_book", "自定义世界书", "",
                        entries = listOf(LoreEntry("six_lore_moonlight", "自己的地点", "自定义内容",
                            activationMode = LoreActivationMode.ALWAYS))),
                )))
                val privateEntry = WorldChatPromptContext.activeLore(customId, null, "", null)
                    .first { it.entry.id == "six_lore_moonlight" }
                assertEquals("自定义内容", privateEntry.entry.content)
            } finally {
                AiluaLocalStore.deleteCustomCard(customId)
                previousCustom?.let(AiluaLocalStore::saveCustomCard)
                CharacterRegistry.refresh()
            }

            val saved = prefs.getString(AiluaLocalStore.KEY_WORLD_LORE_ENABLED_OVERRIDES, null)
            assertTrue(!saved.isNullOrBlank())
            assertTrue(AiluaLocalStore.setWorldLoreEnabled("six_lore_moonlight", true))
            prefs.edit().putString(AiluaLocalStore.KEY_WORLD_LORE_ENABLED_OVERRIDES, saved).commit()
            AiluaLocalStore.loadFromDisk()
            assertEquals(false, AiluaLocalStore.worldLoreEnabledOverrides.value["six_lore_moonlight"])
            assertFalse("six_lore_moonlight" in activatedIds())
            assertFalse("six_lore_mira_work" in activatedIds())
        } finally {
            val editor = prefs.edit()
            if (previous == null) editor.remove(AiluaLocalStore.KEY_WORLD_LORE_ENABLED_OVERRIDES)
            else editor.putString(AiluaLocalStore.KEY_WORLD_LORE_ENABLED_OVERRIDES, previous)
            editor.commit()
            AiluaLocalStore.loadFromDisk()
        }
    }
}
