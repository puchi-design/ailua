package com.example

import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.character.runtime.RuntimeSource
import com.example.data.model.AiluaCharacterExtension
import com.example.data.model.CallSession
import com.example.data.model.CallState
import com.example.data.model.GalleryAsset
import com.example.data.model.GalleryAssetType
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.PlannedWorldAction
import com.example.data.model.WorldPlan
import com.example.data.mock.OfficialCharacters
import com.example.data.projection.EMPTY_CHECK_PHONE_DATA
import com.example.data.projection.checkphone.PhoneSection
import com.example.data.projection.checkphone.PhoneTraceSource
import com.example.data.projection.checkphone.projectCheckPhonePlus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckPhonePlusProjectionTest {
    @Test fun reusesGalleryAndCallHistoryWithoutMixingUserMedia() {
        val id = "hewenchuan"
        val profile = CharacterRuntimeResolver.fromExtension(AiluaCharacterExtension(), id, "贺闻川")
        val gallery = listOf(
            GalleryAsset("owned", id, type = GalleryAssetType.PHOTO, title = "木样",
                caption = "一张照片", createdAtVirtualTime = "16:10", visualReference = "wood"),
            GalleryAsset("user", "user", type = GalleryAssetType.USER_IMPORTED, title = "用户照片",
                caption = "", createdAtVirtualTime = "16:11", visualReference = "user")
        )
        val calls = listOf(
            CallSession("owned-call", id, "贺闻川", state = CallState.ENDED, endedAt = 100),
            CallSession("other-call", "yuna", "许朝颜", state = CallState.ENDED, endedAt = 101)
        )
        val snapshot = projectCheckPhonePlus(id, emptyList(), profile, gallery, calls,
            seedData = EMPTY_CHECK_PHONE_DATA)
        assertEquals(listOf("owned"), snapshot.photos.map { it.id })
        assertEquals(listOf("owned-call"), snapshot.calls.map { it.id })
        assertTrue(snapshot.canSee(PhoneSection.SEARCH))
        assertFalse(snapshot.canSee(PhoneSection.PHOTOS))
    }

    @Test fun userEditedOfficialIdDoesNotInheritOriginalAuthoredPhoneHistory() {
        val imported = CharacterRuntimeResolver.fromExtension(AiluaCharacterExtension(), "mira", "新角色")
        val snapshot = projectCheckPhonePlus("mira", emptyList(), imported)
        assertTrue(snapshot.searches.isEmpty())
        assertTrue(snapshot.drafts.isEmpty())
    }

    @Test fun unchangedOfficialCardKeepsItsOwnInitialHistory() {
        val official = CharacterRuntimeResolver.fromExtension(AiluaCharacterExtension(),
            "hewenchuan", "贺闻川").copy(source = RuntimeSource.OFFICIAL)
        val snapshot = projectCheckPhonePlus("hewenchuan", emptyList(), official)
        assertEquals(OfficialCharacters.checkPhoneByCharacter.getValue("hewenchuan").searchHistory,
            snapshot.searches.map { it.query })
    }

    @Test fun usageIsDerivedFromPersistedEventsAndStableAfterReload() {
        val id = "yuna"
        val profile = CharacterRuntimeResolver.fromExtension(AiluaCharacterExtension(), id, "许朝颜")
        val persisted = listOf(LifeEvent(
            id = "phone-fact", characterId = id, time = "20:13", type = LifeEventType.PHOTO,
            title = "街角", description = "拍了街角", worldDateLabel = "10月5日",
            worldMinutesOfDay = 20 * 60 + 13,
            metadata = mapOf("search_query" to "展览开放时间", "note" to "联系摄影师")))
        val first = projectCheckPhonePlus(id, persisted, profile, seedData = EMPTY_CHECK_PHONE_DATA)
        val afterRestart = projectCheckPhonePlus(id, persisted.map { it.copy() }, profile,
            seedData = EMPTY_CHECK_PHONE_DATA)
        assertEquals(first, afterRestart)
        assertEquals(listOf("搜索", "备忘录", "相册"), first.usage.map { it.appName })
    }

    @Test fun sixOfficialAuthoredDraftsDoNotReuseOnePlaceholder() {
        val drafts = OfficialCharacters.sixRosterIds.map { id ->
            OfficialCharacters.checkPhoneByCharacter.getValue(id).unsentDrafts.single()
        }
        assertEquals(6, drafts.toSet().size)
    }

    @Test fun savedWorldPlanProducesLabeledCharacterTodoWithoutUserMemo() {
        val id = "hewenchuan"
        val profile = CharacterRuntimeResolver.fromExtension(AiluaCharacterExtension(), id, "贺闻川")
        val action = PlannedWorldAction("work-1", id, "10月5日", 900,
            lifeEventType = LifeEventType.THOUGHT, title = "寄送木样", description = "寄给客户",
            location = "工作室", metadata = mapOf("note" to "寄送木样"))
        val snapshot = projectCheckPhonePlus(id, emptyList(), profile,
            worldPlan = WorldPlan("10月5日", 800, listOf(action)),
            seedData = EMPTY_CHECK_PHONE_DATA)
        assertEquals("寄送木样", snapshot.notes.single().text)
        assertTrue(snapshot.notes.single().planned)
        assertEquals(PhoneTraceSource.WORLD_PLAN, snapshot.notes.single().source)
        val afterFiring = projectCheckPhonePlus(id, emptyList(), profile,
            worldPlan = WorldPlan("10月5日", 800, listOf(action)),
            seedData = EMPTY_CHECK_PHONE_DATA, firedActionIds = setOf(action.id))
        assertTrue(afterFiring.notes.isEmpty())
        val ordinaryActivity = action.copy(metadata = emptyMap())
        assertTrue(projectCheckPhonePlus(id, emptyList(), profile,
            worldPlan = WorldPlan("10月5日", 800, listOf(ordinaryActivity)),
            seedData = EMPTY_CHECK_PHONE_DATA).notes.isEmpty())
    }
}
