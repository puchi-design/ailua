package com.example.data.projection

import com.example.data.mock.MockData
import com.example.data.model.DiaryEntry
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.sortedChronologically
import com.example.data.registry.CharacterRegistry

/**
 * DiaryProjection
 *
 * Pure projection: seed diary entries + runtime DIARY facts from the LifeEvent ledger.
 * No Compose, no Context, no storage, no LLM — runtime entries are direct renderings
 * of facts that actually happened (P3C may later author them into prose).
 *
 * Rules:
 * - only DIARY events of the requested character become entries
 * - seed world LifeEvents are excluded: they already exist as authored seed entries
 *   or as Living timeline facts and must not double-render as diary pages
 * - weather/mood/location only carry values that exist in the fact (metadata)
 * - an empty list is a legal state (custom characters without diary facts)
 */
fun projectDiary(
    characterId: String,
    runtimeEvents: List<LifeEvent>,
    seedEntries: List<DiaryEntry> = MockData.diaryEntries,
    seedEventIds: Set<String> = MockData.unifiedLifeEvents.mapTo(HashSet()) { it.id },
    authorNameOf: (String) -> String = { CharacterRegistry.getCharacter(it).name }
): List<DiaryEntry> {
    val seeds = seedEntries.filter { it.characterId == characterId }

    val runtime = runtimeEvents
        .filter {
            it.type == LifeEventType.DIARY &&
                it.characterId == characterId &&
                it.id !in seedEventIds
        }
        .sortedChronologically()
        .reversed()
        .map { ev ->
            DiaryEntry(
                id = ev.id,
                characterId = ev.characterId,
                authorName = authorNameOf(ev.characterId),
                date = ev.worldDateLabel,
                weather = ev.metadata["weather"] ?: "",
                mood = ev.metadata["mood"] ?: "",
                title = ev.title,
                content = ev.description,
                excerpt = ev.description,
                imageReference = ev.imageReference,
                relatedMemoryIds = emptyList()
            )
        }

    return runtime + seeds
}
