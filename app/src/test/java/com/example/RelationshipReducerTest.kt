package com.example

import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.relationship.engine.RelationshipReducer
import com.example.data.relationship.model.RelationshipDelta
import com.example.data.relationship.model.RelationshipState
import org.junit.Assert.*
import org.junit.Test

class RelationshipReducerTest {
    private fun event(id: String) = LifeEvent(id, "mira", "21:30", LifeEventType.MESSAGE, "聊天", "你好", worldDateLabel = "9月25日")

    @Test fun boundedAndDedupe() {
        val start = RelationshipState("mira", "user")
        val first = RelationshipReducer.apply(start, event("one"), RelationshipDelta(affinity = 20, trust = 20, familiarity = 20))
        assertEquals(3, first.affinity)
        assertEquals(3, first.trust)
        assertEquals(2, first.familiarity)
        assertEquals(first, RelationshipReducer.apply(first, event("one")))
    }

    @Test fun pairsStayIndependent() {
        val user = RelationshipReducer.apply(RelationshipState("mira", "user"), event("one"))
        val other = RelationshipReducer.apply(RelationshipState("mira", "yuna"), event("two"))
        assertEquals("user", user.toCharacterId)
        assertEquals("yuna", other.toCharacterId)
        assertEquals(1, user.interactionCount)
        assertEquals(1, other.interactionCount)
    }

    @Test fun processedIdsRemainBoundedAndRecentIdsStillDedupe() {
        val result = (1..300).fold(RelationshipState("mira", "user")) { state, number ->
            RelationshipReducer.apply(state, event("event_$number"))
        }
        assertEquals(256, result.processedEventIds.size)
        assertFalse("event_1" in result.processedEventIds)
        assertTrue("event_300" in result.processedEventIds)
        assertEquals(result, RelationshipReducer.apply(result, event("event_300")))
    }
}
