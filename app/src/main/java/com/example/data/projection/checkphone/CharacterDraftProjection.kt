package com.example.data.projection.checkphone

import com.example.data.model.CheckPhoneData
import com.example.data.model.LifeEvent
import com.example.data.model.WorldPlan
import com.example.data.mock.MockData
import com.example.data.projection.eligiblePhoneEvents

/** No send action or synthetic draft: each runtime item has a saved event or plan ID. */
fun projectCharacterDrafts(
    characterId: String,
    events: List<LifeEvent>,
    worldPlan: WorldPlan?,
    combined: CheckPhoneData,
    seedEventIds: Set<String> = MockData.unifiedLifeEvents.mapTo(HashSet()) { it.id },
    firedActionIds: Set<String> = emptySet(),
): List<CharacterDraft> {
    val runtime = eligiblePhoneEvents(characterId, events, seedEventIds).mapNotNull { event ->
        event.metadata["draft"]?.trim()?.takeIf(String::isNotEmpty)?.let { text ->
            CharacterDraft("draft:${event.id}", characterId, text, event.time, event.id,
                source = PhoneTraceSource.RUNTIME_EVENT)
        }
    }
    // The future action is already stored in WorldPlan. Its explicit draft metadata is a
    // character's unsent text, even though the public LifeEvent has not fired yet.
    val planned = worldPlan?.actions.orEmpty().filter {
        it.characterId == characterId && it.id !in firedActionIds
    }
        .mapNotNull { action ->
            action.metadata["draft"]?.trim()?.takeIf(String::isNotEmpty)?.let { text ->
                CharacterDraft("plan-draft:${action.id}", characterId, text,
                    "%02d:%02d".format(action.triggerMinutes / 60, action.triggerMinutes % 60),
                    source = PhoneTraceSource.WORLD_PLAN)
            }
        }
    val authored = combined.unsentDrafts.drop(runtime.size).mapIndexed { index, text ->
        CharacterDraft("authored-draft:$characterId:$index", characterId, text, null,
            source = PhoneTraceSource.AUTHORED)
    }
    return (runtime + planned + authored).distinctBy { it.text }.take(20)
}
