package com.example

import com.example.data.model.LIFE_EVENT_ACTOR_USER
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.PlannedWorldAction
import com.example.data.model.WorldPlan
import com.example.data.projection.EMPTY_CHECK_PHONE_DATA
import com.example.data.projection.checkphone.CharacterDraftStatus
import com.example.data.projection.checkphone.PhoneTraceSource
import com.example.data.projection.checkphone.projectCharacterDrafts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterDraftProjectionTest {
    private fun thought(id: String, draft: String, actor: String? = null) = LifeEvent(
        id = id, characterId = "hewenchuan", time = "23:48", type = LifeEventType.THOUGHT,
        title = "夜间整理", description = "今天还有话没说完", worldDateLabel = "10月5日",
        worldMinutesOfDay = 23 * 60 + 48,
        metadata = mapOf("draft" to draft) + (actor?.let { mapOf("actor" to it) } ?: emptyMap())
    )

    @Test fun persistedWorldDraftSurvivesReprojectionAndKeepsEventIdentity() {
        val saved = listOf(thought("event-1", "想问你到家了没有。"))
        val first = projectCharacterDrafts("hewenchuan", saved, null, EMPTY_CHECK_PHONE_DATA)
        val afterRestart = projectCharacterDrafts("hewenchuan", saved.map { it.copy() }, null,
            EMPTY_CHECK_PHONE_DATA)
        assertEquals(first, afterRestart)
        assertEquals("event-1", first.single().contextEventId)
        assertEquals(CharacterDraftStatus.UNSENT, first.single().status)
        assertEquals(PhoneTraceSource.RUNTIME_EVENT, first.single().source)
    }

    @Test fun savedWorldPlanCanProjectAnExplicitUnsentDraftBeforeActionFires() {
        val action = PlannedWorldAction("plan-1", "hewenchuan", "10月5日", 1420,
            lifeEventType = LifeEventType.THOUGHT, title = "收拾工作室", description = "准备关灯",
            location = "工作室", metadata = mapOf("draft" to "早点休息。"))
        val plan = WorldPlan("10月5日", 900, listOf(action))
        val drafts = projectCharacterDrafts("hewenchuan", emptyList(), plan, EMPTY_CHECK_PHONE_DATA)
        assertEquals("早点休息。", drafts.single().text)
        assertEquals(PhoneTraceSource.WORLD_PLAN, drafts.single().source)
        assertTrue(projectCharacterDrafts("hewenchuan", emptyList(), plan, EMPTY_CHECK_PHONE_DATA,
            firedActionIds = setOf(action.id)).isEmpty())
    }

    @Test fun userActivityAndOtherCharacterDraftsNeverLeak() {
        val saved = listOf(thought("user", "用户私语", LIFE_EVENT_ACTOR_USER),
            thought("other", "其他角色的话").copy(characterId = "yuna"))
        val drafts = projectCharacterDrafts("hewenchuan", saved, null, EMPTY_CHECK_PHONE_DATA)
        assertTrue(drafts.isEmpty())
    }

}
