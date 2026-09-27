package com.example

import com.example.data.ai.provider.FakeAiProvider
import com.example.data.ai.runtime.SendResult
import com.example.data.engine.UserActivityRecorder
import com.example.data.engine.WorldStateRepository
import com.example.data.model.LIFE_EVENT_ACTOR_USER
import com.example.data.model.LifeEventType
import com.example.data.model.isUserActivity
import com.example.data.projection.EMPTY_CHECK_PHONE_DATA
import com.example.data.projection.projectCheckPhone
import com.example.data.projection.projectLiving
import com.example.data.projection.projectMoments
import com.example.data.projection.projectPresence
import com.example.data.registry.CharacterRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P3D-3 cross-app continuity: user app actions become LifeEvent facts the
 * chat prompt can see, while character-side projections stay free of them.
 */
class UserActivityContinuityTest {

    private val mira = CharacterRegistry.getCharacter("mira")

    @Test
    fun photoImportBecomesUserPhotoFact() {
        val event = UserActivityRecorder.recordPhotoImport(title = "白瓷瓶里的栀子花")

        assertEquals(LifeEventType.PHOTO, event.type)
        assertEquals("gallery", event.sourceAppId)
        assertTrue(event.isUserActivity())
        assertEquals(LIFE_EVENT_ACTOR_USER, event.metadata["actor"])
        assertTrue(event.description.contains("栀子花"))

        // Prompt-visible for the character.
        assertTrue(WorldStateRepository.eventsForCharacter(event.characterId).any { it.id == event.id })
    }

    @Test
    fun userPhotoFactStaysOutOfCharacterProjections() {
        val event = UserActivityRecorder.recordPhotoImport(title = "不该出现在角色相册")
        val events = WorldStateRepository.events.value

        val presence = projectPresence(mira, events)
        assertFalse(presence.currentEventId == event.id)

        val phone = projectCheckPhone(
            characterId = "mira",
            runtimeEvents = events,
            seedEventIds = emptySet(),
            seedData = EMPTY_CHECK_PHONE_DATA,
        )
        assertFalse(phone.privateGallery.any { it.title.contains("不该出现在角色相册") })

        val moments = projectMoments(seedPosts = emptyList(), runtimeEvents = listOf(event))
        assertTrue(moments.isEmpty())

        val living = projectLiving(
            character = mira,
            seedTimeline = emptyList(),
            runtimeEvents = listOf(event),
            seedEventIds = emptySet(),
        )
        assertNull(living.currentEvent)
        assertEquals(mira.currentActivity, living.currentActivity)
    }

    @Test
    fun chatMessageBecomesUserMessageFact() {
        val event = UserActivityRecorder.recordChatMessage(
            characterId = "mira",
            characterName = "小弥",
            userText = "今晚的雨声好好听",
        )

        assertEquals(LifeEventType.MESSAGE, event!!.type)
        assertEquals("chat", event.sourceAppId)
        assertTrue(event.isUserActivity())
        assertTrue(event.title.contains("小弥"))
        assertTrue(event.description.contains("今晚的雨声好好听"))
        assertTrue(WorldStateRepository.eventsForCharacter("mira").any { it.id == event.id })
    }

    @Test
    fun blankChatMessageRecordsNothing() {
        assertNull(UserActivityRecorder.recordChatMessage("mira", "小弥", "   "))
    }

    @Test
    fun placeVisitBecomesLocationChangeFact() {
        val event = UserActivityRecorder.recordPlaceVisit(
            characterId = "mira",
            placeName = "琉璃茶馆",
        )

        assertEquals(LifeEventType.LOCATION_CHANGE, event.type)
        assertEquals("琉璃茶馆", event.location)
        assertTrue(event.isUserActivity())
        assertTrue(event.title.contains("琉璃茶馆"))
    }

    @Test
    fun promptLifeEventListIsCappedToNewestTwelve() = runBlocking {
        val fixture = ChatRuntimeFixture()
        fixture.promptContext.lifeEvents = (1..20).map { runtimeLifeEvent("mira", "事件$it") }
        val fake = FakeAiProvider.scripted("好的")
        fixture.use(fake)

        assertEquals(SendResult.Completed, fixture.runtime.send("mira", "你好"))

        val prompt = fake.requests.single().messages.joinToString("\n") { it.content }
        assertTrue(prompt.contains("事件20"))
        assertTrue(prompt.contains("事件9"))
        assertFalse(prompt.contains("事件8"))
    }
}
