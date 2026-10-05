package com.example.data.ai.runtime

import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.mock.WorldData
import com.example.data.model.CharacterCardData
import com.example.data.model.LifeEvent
import com.example.data.model.LoreActivationResult
import com.example.data.projection.CharacterPresence
import com.example.data.projection.projectPresence
import com.example.data.registry.CharacterRegistry

/**
 * ChatPromptContext — the prompt-assembly data seam for the chat runtime
 * (P3C-4 §6).
 *
 * Production ([WorldChatPromptContext]) reads ONLY real sources: the
 * CharacterRegistry card, WorldStateRepository life events, projectPresence,
 * and the current card's lore book plus public world settings. Tests inject fixed values.
 *
 * Memory (P3C-5) is deliberately absent here — runtime passes
 * `emptyList()` until the memory DB exists. No fake memories, ever.
 */
interface ChatPromptContext {
    /** Character card for prompting; `null` when the character does not exist. */
    fun characterCard(characterId: String): CharacterCardData?

    /** Current presence (activity/location) projection, or `null`. */
    fun presence(characterId: String): CharacterPresence?

    /** Life events for [characterId], chronological ascending. */
    fun lifeEvents(characterId: String): List<LifeEvent>

    /** Active lore for the current turn context (§6 inputs). */
    fun activeLore(
        characterId: String,
        locationId: String?,
        recentUserText: String,
        lifeEventTitle: String?,
    ): List<LoreActivationResult>

    /** Virtual-world `currentDate` + `currentTime` pair for macros. */
    fun temporal(): Pair<String, String>

    /** Persona name; `""` when unknown. */
    fun userName(): String
}

/**
 * Production prompt context over the existing real sources (P3C-4 §6).
 * Never reads MockData for chat content — cards come from CharacterRegistry,
 * lore from the current card and public world book, events from WorldStateRepository.
 */
object WorldChatPromptContext : ChatPromptContext {

    override fun characterCard(characterId: String): CharacterCardData? =
        CharacterRegistry.getCard(characterId)?.data

    override fun presence(characterId: String): CharacterPresence? {
        val profile = CharacterRegistry.getCharacter(characterId)
        return projectPresence(profile, WorldStateRepository.eventsForCharacter(characterId).filter(::visibleToCharacter))
    }

    override fun lifeEvents(characterId: String): List<LifeEvent> =
        WorldStateRepository.eventsForCharacter(characterId).filter(::visibleToCharacter)

    /** Looking through the character's phone is a private user action, not something the character can recall. */
    private fun visibleToCharacter(event: LifeEvent): Boolean =
        event.sourceAppId != "check_phone" && event.metadata["prompt_visibility"] != "hidden"

    override fun activeLore(
        characterId: String,
        locationId: String?,
        recentUserText: String,
        lifeEventTitle: String?,
    ): List<LoreActivationResult> {
        val unmodifiedBuiltIn = CharacterRegistry.isUnmodifiedBuiltIn(characterId)
        val sourceCard = CharacterRegistry.getCard(characterId)?.data
        // Official personal entries also appear in the World Book screen. Apply the same
        // switch to them, while leaving imported and user-edited card books untouched.
        val card = if (unmodifiedBuiltIn) sourceCard?.copy(
            characterBook = sourceCard.characterBook?.let { WorldData.withOfficialEnabledOverrides(it) }
        ) else sourceCard
        return CharacterLoreResolver.resolve(
            characterId = characterId,
            card = card,
            worldBook = WorldData.activeWorldBook(),
            locationId = locationId,
            recentText = recentUserText,
            lifeEventTitle = lifeEventTitle,
            allowLegacyCharacterLore = unmodifiedBuiltIn,
        )
    }

    override fun temporal(): Pair<String, String> {
        val clock = WorldHeartbeatEngine.worldClock.value
        return clock.dateLabel to clock.timeFormatted
    }

    override fun userName(): String = ""
}
