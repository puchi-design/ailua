package com.example

import com.example.data.ai.prompt.PromptAssembler
import com.example.data.ai.prompt.PromptAssemblyInput
import com.example.data.ai.prompt.PromptBudget
import com.example.data.character.CharacterBehaviorRuntime
import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.character.runtime.RuntimeSource
import com.example.data.codec.AiluaCharacterExtensionCodec
import com.example.data.mock.OfficialCharacters
import com.example.data.model.AiluaBehavior
import com.example.data.model.AiluaCharacterExtension
import com.example.data.model.AiluaIdentity
import com.example.data.model.AiluaRelationship
import com.example.data.model.CharacterCardData
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.*
import org.junit.Test

class RomancePromptRuntimeTest {
    @Test
    fun everyOfficialCardHasEightDistinctScenarioExamplesAndContextualVoiceGuard() {
        val categories = listOf("普通", "疲惫", "高兴", "深夜", "吃醋", "误会", "久未联系", "升温")
        val examples = OfficialCharacters.cards.associate { card ->
            card.data.id to card.data.exampleMessages.split("<START>").filter { it.isNotBlank() }
        }
        examples.forEach { (id, exchanges) ->
            assertEquals("$id needs all eight situations", categories.size, exchanges.size)
            assertEquals(8, exchanges.toSet().size)
            exchanges.forEach { assertTrue(it.contains("{{user}}:")); assertTrue(it.contains("{{char}}:")) }
        }
        categories.forEachIndexed { index, category ->
            val replies = examples.values.map { it[index].substringAfter("{{char}}:").trim() }
            assertEquals("$category responses must not share one template", 6, replies.toSet().size)
        }
        OfficialCharacters.cards.forEach { card ->
            val instructions = card.data.systemPrompt
            listOf("辛苦了", "我会一直陪着你", "你已经做得很好了", "无论如何我都支持你", "依据真实互动", "不随时围着用户转")
                .forEach { assertTrue("${card.data.id}: missing $it", instructions.contains(it)) }
        }
    }

    @Test
    fun runtimeCarriesDistinctVoiceJealousyConflictAndProgressionWithoutWholeJson() {
        val expectedStyles = mapOf("hewenchuan" to "slow_trust", "zhoujianye" to "expressive", "peixubai" to "reserved")
        val rendered = OfficialCharacters.cards.filter { it.data.id in OfficialCharacters.romanceIds }.map { card ->
            val runtime = CharacterRuntimeResolver.resolve(card.data)
            assertEquals(RuntimeSource.OFFICIAL, runtime.source)
            assertEquals(expectedStyles.getValue(card.data.id), runtime.relationship.progressionStyle)
            val prompt = CharacterBehaviorRuntime.prompt(runtime)
            assertTrue(prompt.contains(runtime.speech.sentenceLength))
            assertTrue(prompt.contains(runtime.behavior.jealousyPatterns.first()))
            assertTrue(prompt.contains(runtime.behavior.conflictPatterns.first()))
            assertTrue(prompt.contains("足够关系基础"))
            assertTrue(prompt.contains("不辱骂、控制、强迫或威胁"))
            assertFalse(prompt.contains("\"schema\""))
            assertFalse(prompt.contains("asset_pack"))
            prompt
        }
        assertEquals(3, rendered.toSet().size)
    }

    @Test
    fun authoredV2FieldsExamplesAndPostHistoryRemainInTheirOwnPromptSlots() {
        OfficialCharacters.cards.forEach { card ->
            val data = card.data
            val result = PromptAssembler.assemble(PromptAssemblyInput(character = data, userName = "读者"))
            val core = result.includedBlocks.single { it.id == "character_core" }.content
            listOf(data.description, data.personality, data.scenario, data.systemPrompt).forEach { assertTrue(core.contains(it)) }
            val examples = result.includedBlocks.single { it.id == "example_messages" }.content
            assertEquals(8, Regex("<START>").findAll(examples).count())
            assertTrue(examples.contains("读者:"))
            assertFalse(examples.contains("{{char}}"))
            assertEquals(data.postHistoryInstructions, result.includedBlocks.single { it.id == "post_history" }.content)
            assertEquals(1, result.includedBlocks.count { it.id == "ailua_behavior" })
            assertFalse(result.overflow)
        }
    }

    @Test
    fun longImportedFieldsCannotCrowdOutTheFixedRelationshipRules() {
        val longText = "人物详细设定".repeat(3000)
        val extension = AiluaCharacterExtension(
            relationship = AiluaRelationship(routeType = "romance"),
            behavior = AiluaBehavior(coreDesire = longText, flaws = List(100) { longText }, boundaries = List(100) { longText }),
        )
        val profile = CharacterRuntimeResolver.fromExtension(extension)
        val prompt = CharacterBehaviorRuntime.prompt(profile)
        assertTrue(prompt.length <= CharacterBehaviorRuntime.MAX_INSTRUCTION_CHARS)
        assertTrue(prompt.startsWith("角色行为约定"))
        assertTrue(prompt.contains("双方意愿"))
        assertTrue(prompt.contains("不辱骂、控制、强迫或威胁"))
    }

    @Test
    fun ordinaryCardsKeepTheirOwnVoiceAndDoNotAcquireGenderOrRomanceFromTags() {
        val data = CharacterCardData(id = "ordinary", name = "旅行同伴", personality = "爽快直接", tags = listOf("romance", "male", "旧书"))
        val profile = CharacterRuntimeResolver.resolve(data)
        assertEquals("", profile.identity.gender)
        assertFalse(profile.relationship.romanceEnabled)
        val result = PromptAssembler.assemble(PromptAssemblyInput(character = data))
        val instructions = result.includedBlocks.single { it.id == "ailua_behavior" }
        assertTrue(instructions.content.contains("当前未启用恋爱路线"))
        assertFalse(instructions.content.contains("吃醋须"))
        assertFalse(instructions.required)
        assertTrue(result.includedBlocks.single { it.id == "character_core" }.content.contains("爽快直接"))
    }

    @Test
    fun anOverrideOfAnOfficialIdUsesTheUsersRelationshipAndArbitraryGender() {
        val custom = AiluaCharacterExtension(
            identity = AiluaIdentity(gender = "自定义性别"),
            relationship = AiluaRelationship(routeType = "family", initialRelation = "家人"),
            behavior = AiluaBehavior(coreDesire = "守好自己的菜园", jealousyPatterns = listOf("不应启用的恋爱片段")),
        )
        val data = OfficialCharacters.cardYan.data.copy(
            extensions = AiluaCharacterExtensionCodec.write(JsonObject(emptyMap()), custom),
        )
        val runtime = CharacterRuntimeResolver.resolve(data)
        assertEquals(RuntimeSource.IMPORTED, runtime.source)
        val prompt = CharacterBehaviorRuntime.prompt(runtime)
        assertTrue(prompt.contains("自定义性别"))
        assertTrue(prompt.contains("路线：family"))
        assertTrue(prompt.contains("守好自己的菜园"))
        assertFalse(prompt.contains("不应启用的恋爱片段"))
        assertFalse(prompt.contains("先累积信任"))
    }

    @Test
    fun fallbackInstructionsYieldToAuthoredFieldsUnderSmallBudget() {
        val data = CharacterCardData(id = "ordinary", name = "Plain", personality = "Keep this authored personality.")
        val generous = PromptAssembler.assemble(PromptAssemblyInput(character = data))
        val authoredCost = generous.includedBlocks.filter { it.required }.sumOf { (it.content.length + 3) / 4 }
        val small = PromptAssembler.assemble(PromptAssemblyInput(character = data), PromptBudget(authoredCost, 0))
        assertTrue(small.includedBlocks.any { it.id == "character_core" })
        assertTrue(small.droppedBlocks.any { it.id == "ailua_behavior" })
        assertFalse(small.overflow)
    }
}
