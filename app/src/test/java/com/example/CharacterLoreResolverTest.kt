package com.example

import com.example.data.ai.runtime.CharacterLoreResolver
import com.example.data.ai.runtime.WorldChatPromptContext
import com.example.data.codec.CharacterCardJsonCodec
import com.example.data.local.AiluaLocalStore
import com.example.data.mock.OfficialCharacters
import com.example.data.model.CharacterCardData
import com.example.data.model.LoreActivationMode
import com.example.data.model.LoreEntry
import com.example.data.model.WorldBook
import com.example.data.registry.CharacterRegistry
import org.junit.Assert.*
import org.junit.Test

class CharacterLoreResolverTest {
    private fun book(vararg entries: LoreEntry) = WorldBook("test_book", "测试书", "测试", entries = entries.toList())

    private val place = LoreEntry("public_place", "阅览室", "这里有木桌与借阅柜。", keywords = listOf("阅览室"),
        category = "地点", locationIds = listOf("reading_room"), characterIds = listOf("noa"), priority = 99)
    private val originalPersona = LoreEntry("old_persona", "旧人物设定", "旧诺亚是档案学者。", keywords = listOf("诺亚"),
        characterIds = listOf("noa"), activationMode = LoreActivationMode.CHARACTER, category = "人物", priority = 99)

    @Test
    fun personalBookOverridesDuplicatePublicEntryAndWinsBudgetPriority() {
        val privateEntry = place.copy(content = "私有书指定的阅览室。", priority = 1)
        val shared = LoreEntry("shared_weather", "雨季", "街区正值雨季。", activationMode = LoreActivationMode.ALWAYS, priority = 500)
        val card = CharacterCardData(id = "custom", name = "读者", characterBook = book(privateEntry))
        val resolved = CharacterLoreResolver.resolve("custom", card, book(place, shared, originalPersona),
            locationId = "reading_room", recentText = "诺亚 阅览室")
        assertEquals(listOf("public_place", "shared_weather"), resolved.map { it.entry.id })
        assertEquals("私有书指定的阅览室。", resolved.first().entry.content)
        assertTrue(resolved.first().effectivePriority > resolved.last().effectivePriority)
    }

    @Test
    fun disabledPersonalEntryCannotBeResurrectedByPublicDuplicate() {
        val card = CharacterCardData(id = "custom", name = "读者", characterBook = book(place.copy(enabled = false)))
        val resolved = CharacterLoreResolver.resolve("custom", card, book(place), locationId = "reading_room")
        assertTrue(resolved.isEmpty())
    }

    @Test
    fun noBookCustomAndSavedBuiltinReceivePublicPlaceWithoutOriginalPersona() {
        listOf("custom", "noa").forEach { id ->
            val card = CharacterCardData(id = id, name = "编辑后的角色")
            val resolved = CharacterLoreResolver.resolve(id, card, book(place, originalPersona),
                locationId = "reading_room", recentText = "诺亚 阅览室")
            assertEquals(listOf("public_place"), resolved.map { it.entry.id })
        }
    }

    @Test
    fun uneditedLegacyCardCanKeepOwnLoreButNotAnotherCharactersPersona() {
        val another = originalPersona.copy(id = "another_persona", characterIds = listOf("mira"))
        val card = CharacterCardData(id = "noa", name = "诺亚")
        val resolved = CharacterLoreResolver.resolve("noa", card, book(originalPersona, another),
            recentText = "诺亚", allowLegacyCharacterLore = true)
        assertEquals(listOf("old_persona"), resolved.map { it.entry.id })
        val privateCard = card.copy(characterBook = book())
        assertTrue(CharacterLoreResolver.resolve("noa", privateCard, book(originalPersona),
            recentText = "诺亚", allowLegacyCharacterLore = true).isEmpty())
    }

    @Test
    fun importedV2SelectiveEntryRequiresBothKeysWithRequestedCase() {
        val card = importedCard("""{
            "keys":["Archive"], "secondary_keys":["Night"], "selective":true,
            "case_sensitive":true, "content":"双关键词条目", "enabled":true, "insertion_order":0
        }""")
        fun results(text: String) = CharacterLoreResolver.resolve("custom", card, book(), recentText = text)
        assertTrue(results("Archive").isEmpty())
        assertTrue(results("Night").isEmpty())
        assertTrue(results("archive Night").isEmpty())
        assertTrue(results("Archive night").isEmpty())
        assertEquals("双关键词条目", results("Archive at Night").single().entry.content)
        val noSecondary = card.copy(characterBook = card.characterBook!!.copy(entries = card.characterBook!!.entries.map {
            it.copy(secondaryKeywords = emptyList())
        }))
        assertTrue(CharacterLoreResolver.resolve("custom", noSecondary, book(), recentText = "Archive Night").isEmpty())
    }

    @Test
    fun importedV2ConstantAndDefaultCaseInsensitiveMatchingRespectDisabled() {
        val card = importedCard("""{
            "keys":["Archive"], "secondary_keys":["ignored"], "selective":false,
            "content":"默认不区分大小写", "enabled":true, "insertion_order":0
        }, {
            "keys":[], "secondary_keys":[], "selective":true, "constant":true,
            "content":"常驻", "enabled":true, "insertion_order":1
        }, {
            "keys":[], "constant":true, "content":"已禁用", "enabled":false, "insertion_order":2
        }""")
        val contents = CharacterLoreResolver.resolve("custom", card, book(), recentText = "archive").map { it.entry.content }
        assertEquals(setOf("默认不区分大小写", "常驻"), contents.toSet())
        assertEquals(listOf("常驻"), CharacterLoreResolver.resolve("custom", card, book()).map { it.entry.content })
    }

    @Test
    fun productionContextReadsSavedBookAndDoesNotRestoreRemovedOfficialBook() {
        val previous = AiluaLocalStore.customCards.value.firstOrNull { it.data.id == "noa" }
        val entry = LoreEntry("saved_card_lore", "自定场景", "保存卡自己的世界书内容。", activationMode = LoreActivationMode.ALWAYS)
        val edited = OfficialCharacters.cardNoa.copy(data = OfficialCharacters.cardNoa.data.copy(characterBook = book(entry)))
        try {
            CharacterRegistry.saveCharacterCard(edited)
            assertFalse(CharacterRegistry.isUnmodifiedBuiltIn("noa"))
            val withBook = WorldChatPromptContext.activeLore("noa", "place_moonlight", "诺亚 月光书阁", null)
            assertTrue(withBook.any { it.entry.id == "saved_card_lore" })
            assertTrue(withBook.any { it.entry.id == "lore_moonlight_archive" })
            assertFalse(withBook.any { it.entry.id == "official_lore_noa_work" })
            CharacterRegistry.saveCharacterCard(edited.copy(data = edited.data.copy(characterBook = null)))
            val withoutBook = WorldChatPromptContext.activeLore("noa", "place_moonlight", "诺亚 月光书阁", null)
            assertFalse(withoutBook.any { it.entry.id == "saved_card_lore" || it.entry.id == "official_lore_noa_work" })
            assertTrue(withoutBook.any { it.entry.id == "lore_moonlight_archive" })
        } finally {
            AiluaLocalStore.deleteCustomCard("noa")
            previous?.let(AiluaLocalStore::saveCustomCard)
            CharacterRegistry.refresh()
        }
    }

    private fun importedCard(entries: String): CharacterCardData = CharacterCardJsonCodec.decode("""{
        "spec":"chara_card_v2", "spec_version":"2.0", "data":{
            "id":"custom", "name":"测试角色",
            "character_book":{"name":"随卡世界书", "description":"测试", "entries":[$entries]}
        }
    }""").data
}
