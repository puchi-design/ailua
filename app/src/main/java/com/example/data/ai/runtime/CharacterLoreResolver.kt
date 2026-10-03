package com.example.data.ai.runtime

import com.example.data.model.CharacterCardData
import com.example.data.model.LoreActivationMode
import com.example.data.model.LoreActivationResult
import com.example.data.model.LoreEntry
import com.example.data.model.WorldBook
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** Pure lore selection. Reading the registry and deciding whether a card is edited belong to the caller. */
object CharacterLoreResolver {
    fun resolve(
        characterId: String,
        card: CharacterCardData?,
        worldBook: WorldBook,
        locationId: String? = null,
        recentText: String = "",
        lifeEventTitle: String? = null,
        allowLegacyCharacterLore: Boolean = false,
    ): List<LoreActivationResult> {
        val personalBook = card?.characterBook
        // An explicit disabled/private entry also suppresses its public counterpart.
        val personalIds = personalBook?.entries?.map { it.id }?.toSet().orEmpty()
        val publicBook = worldBook.copy(entries = worldBook.entries.filter { entry ->
            entry.id !in personalIds && (entry.isPublicWorldSetting() ||
                (personalBook == null && allowLegacyCharacterLore && characterId in entry.characterIds))
        })
        val personal = personalBook?.let { activate(it, characterId, locationId, recentText, lifeEventTitle, true) }.orEmpty()
        val public = activate(publicBook, characterId, locationId, recentText, lifeEventTitle, false)

        // PromptAssembler reserves 0..199 for lore priority. Keep the card's own book
        // above shared context when the prompt budget trims lower-priority blocks.
        return personal.map { it.copy(effectivePriority = 100 + it.effectivePriority.coerceIn(0, 99)) } +
            public.map { it.copy(effectivePriority = it.effectivePriority.coerceIn(0, 99)) }
    }

    private fun LoreEntry.isPublicWorldSetting(): Boolean =
        category == "地点" || (characterIds.isEmpty() && activationMode != LoreActivationMode.CHARACTER &&
            category !in setOf("人物", "人物关系", "习惯", "秘密", "共同记忆"))

    /** Public lore keeps the existing local rules; imported private books also honor V2 matching flags. */
    private fun activate(
        worldBook: WorldBook,
        characterId: String? = null,
        locationId: String? = null,
        recentText: String = "",
        lifeEventTitle: String? = null,
        respectV2Matching: Boolean,
    ): List<LoreActivationResult> = worldBook.entries.filter { it.enabled }.mapNotNull { entry ->
        val reasons = mutableListOf<String>()
        if (entry.activationMode == LoreActivationMode.ALWAYS) reasons += "常驻激活"
        if (locationId != null && locationId in entry.locationIds) reasons += "当前激活：地点 = ${entry.title}"
        val caseSensitive = respectV2Matching && (entry.sourceJson?.get("case_sensitive") as? JsonPrimitive)?.booleanOrNull == true
        val selective = respectV2Matching && (entry.sourceJson?.get("selective") as? JsonPrimitive)?.booleanOrNull == true
        val secondaryMatches = !selective || entry.secondaryKeywords.any { key ->
            key.isNotBlank() && (recentText.contains(key, ignoreCase = !caseSensitive) ||
                lifeEventTitle?.contains(key, ignoreCase = !caseSensitive) == true)
        }
        val matched = entry.keywords.filter { it.isNotBlank() && recentText.contains(it, ignoreCase = !caseSensitive) }
        if (matched.isNotEmpty() && secondaryMatches) reasons += "触发词：${matched.joinToString(" / ")}"
        if (characterId != null && characterId in entry.characterIds && entry.activationMode == LoreActivationMode.CHARACTER) {
            reasons += "角色关联：$characterId"
        }
        if (lifeEventTitle != null && secondaryMatches && entry.keywords.any {
                it.isNotBlank() && lifeEventTitle.contains(it, ignoreCase = respectV2Matching && !caseSensitive)
            }) {
            reasons += "生活脉搏联动：$lifeEventTitle"
        }
        reasons.takeIf { it.isNotEmpty() }?.let { LoreActivationResult(entry, it, entry.priority) }
    }.sortedByDescending { it.effectivePriority }
}
