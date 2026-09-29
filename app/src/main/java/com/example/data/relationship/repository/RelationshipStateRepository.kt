package com.example.data.relationship.repository

import com.example.data.local.AiluaLocalStore
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.LIFE_EVENT_ACTOR_USER
import com.example.data.model.isUserActivity
import com.example.data.model.sortedChronologically
import com.example.data.relationship.engine.RelationshipReducer
import com.example.data.relationship.model.RelationshipState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RelationshipStateRepository {
    private val _states = MutableStateFlow<List<RelationshipState>>(emptyList())
    val states: StateFlow<List<RelationshipState>> = _states.asStateFlow()

    fun restore() { _states.value = AiluaLocalStore.savedRelationships.value }

    fun rebuild(events: List<LifeEvent>) {
        if (_states.value.isNotEmpty()) return
        events.sortedChronologically().forEach(::observe)
    }

    fun recordMemory(characterId: String, sourceId: String, content: String, date: String, time: String) {
        com.example.data.engine.WorldStateRepository.appendLifeEvent(LifeEvent(
            id = "relationship_memory_${characterId}_$sourceId", characterId = characterId,
            time = time, type = LifeEventType.MEMORY, title = "共同记忆", description = content.take(160),
            worldDateLabel = date, relatedCharacterIds = listOf("user"), sourceAppId = "memory",
            metadata = mapOf("actor" to LIFE_EVENT_ACTOR_USER),
        ))
    }

    fun recordMomentComment(characterId: String, commentId: String, content: String, date: String, time: String) {
        com.example.data.engine.WorldStateRepository.appendLifeEvent(LifeEvent(
            id = "relationship_comment_$commentId", characterId = characterId,
            time = time, type = LifeEventType.SOCIAL, title = "你评论了动态", description = content.take(160),
            worldDateLabel = date, relatedCharacterIds = listOf("user"), sourceAppId = "moments",
            metadata = mapOf("actor" to LIFE_EVENT_ACTOR_USER),
        ))
    }

    fun observe(event: LifeEvent) {
        val targets = event.relatedCharacterIds.filter { it != event.characterId }.toMutableSet()
        if (event.type == LifeEventType.MESSAGE && targets.isEmpty()) targets += "user"
        if (event.isUserActivity() && event.type != LifeEventType.MESSAGE && event.sourceAppId !in setOf("memory", "moments")) return
        if (targets.isEmpty()) return
        var updated = _states.value
        targets.forEach { target ->
            val pair = listOf(event.characterId, target).sorted()
            val index = updated.indexOfFirst { it.fromCharacterId == pair[0] && it.toCharacterId == pair[1] }
            val state = if (index >= 0) updated[index] else RelationshipState(pair[0], pair[1])
            val reduced = RelationshipReducer.apply(state, event)
            updated = if (index >= 0) updated.toMutableList().also { it[index] = reduced } else updated + reduced
        }
        if (updated != _states.value) {
            _states.value = updated
            AiluaLocalStore.saveRelationships(updated)
        }
    }
}
