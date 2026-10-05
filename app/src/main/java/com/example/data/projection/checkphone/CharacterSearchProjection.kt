package com.example.data.projection.checkphone

import com.example.data.model.CheckPhoneData
import com.example.data.model.LifeEvent
import com.example.data.mock.MockData
import com.example.data.projection.eligiblePhoneEvents

/** Explicit runtime searches precede character-specific authored history. */
fun projectCharacterSearchHistory(
    characterId: String,
    events: List<LifeEvent>,
    combined: CheckPhoneData,
    seedEventIds: Set<String> = MockData.unifiedLifeEvents.mapTo(HashSet()) { it.id },
): List<PhoneSearchRecord> {
    val runtime = eligiblePhoneEvents(characterId, events, seedEventIds).mapNotNull { event ->
        event.metadata["search_query"]?.trim()?.takeIf(String::isNotEmpty)?.let { query ->
            PhoneSearchRecord("search:${event.id}", query, event.time, PhoneTraceSource.RUNTIME_EVENT)
        }
    }
    val authored = combined.searchHistory.drop(runtime.size).mapIndexed { index, query ->
        PhoneSearchRecord("authored-search:$characterId:$index", query, null, PhoneTraceSource.AUTHORED)
    }
    return (runtime + authored).distinctBy { it.query }.take(20)
}
