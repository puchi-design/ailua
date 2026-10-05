package com.example

import com.example.data.mock.MockData
import com.example.data.mock.OfficialCharacters
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.projection.EMPTY_CHECK_PHONE_DATA
import com.example.data.projection.projectCheckPhone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CheckPhoneProjectionTest
 *
 * P3B-2 CheckPhone projection contract:
 * - Every official character keeps their own authored seed; unknown IDs start empty (never another character's)
 * - traces project only from explicit facts (PHOTO -> gallery, metadata keys -> sections)
 * - seed world LifeEvents are excluded from trace generation
 * - no fabricated search history / drafts / browsing
 */
class CheckPhoneProjectionTest {

    private fun event(
        id: String,
        characterId: String = "mira",
        type: LifeEventType = LifeEventType.THOUGHT,
        time: String = "12:00",
        worldMinutesOfDay: Int = 12 * 60,
        title: String = "title_$id",
        description: String = "desc_$id",
        imageReference: String? = null,
        metadata: Map<String, String> = emptyMap()
    ) = LifeEvent(
        id = id,
        characterId = characterId,
        time = time,
        type = type,
        title = title,
        description = description,
        imageReference = imageReference,
        worldDateLabel = "9月25日",
        worldMinutesOfDay = worldMinutesOfDay,
        metadata = metadata
    )

    private val seedIds = MockData.unifiedLifeEvents.mapTo(HashSet()) { it.id }

    @Test
    fun miraKeepsCurrentOfficialPhoneDataWithoutRuntimeFacts() {
        val data = projectCheckPhone("mira", emptyList())
        val official = OfficialCharacters.checkPhoneByCharacter.getValue("mira")
        assertEquals(official.searchHistory, data.searchHistory)
        assertEquals(official.unsentDrafts, data.unsentDrafts)
        assertEquals(official.privateGallery, data.privateGallery)
        assertEquals(official.recentlyPlayed, data.recentlyPlayed)
    }

    @Test
    fun otherCharacterNeverFallsBackToMiraData() {
        val data = projectCheckPhone("unknown_test_character", emptyList())
        assertTrue(data.searchHistory.isEmpty())
        assertTrue(data.unsentDrafts.isEmpty())
        assertTrue(data.notes.isEmpty())
        assertTrue(data.recentlyPlayed.isEmpty())
        assertTrue(data.privateGallery.isEmpty())
        assertTrue(data.browsingHistory.isEmpty())
        assertTrue(data.savedItems.isEmpty())
        assertTrue(data.hiddenThoughts.isEmpty())
        assertEquals(EMPTY_CHECK_PHONE_DATA, data)
    }

    @Test
    fun officialCharactersUseIndependentAuthoredPhoneSeeds() {
        OfficialCharacters.sixRosterIds.forEach { id ->
            val own = OfficialCharacters.checkPhoneByCharacter.getValue(id)
            assertEquals(own, projectCheckPhone(id, emptyList()))
            if (id != "mira") assertTrue(own.searchHistory != MockData.checkPhoneData.searchHistory)
        }
    }

    @Test
    fun searchMetadataProjectsToSearchHistory() {
        val data = projectCheckPhone(
            "yuna",
            listOf(event("e1", characterId = "yuna", metadata = mapOf("search_query" to "海边露营地"))),
            seedData = EMPTY_CHECK_PHONE_DATA,
        )
        assertEquals(listOf("海边露营地"), data.searchHistory)
    }

    @Test
    fun draftAndNoteMetadataProjectToSections() {
        val data = projectCheckPhone(
            "yuna",
            listOf(
                event("e1", characterId = "yuna", metadata = mapOf("draft" to "想说但没发送的话")),
                event("e2", characterId = "yuna", metadata = mapOf("note" to "记得浇花"))
            ),
            seedData = EMPTY_CHECK_PHONE_DATA,
        )
        assertEquals(listOf("想说但没发送的话"), data.unsentDrafts)
        assertEquals(listOf("记得浇花"), data.notes)
    }

    @Test
    fun photoFactProjectsToPrivateGallery() {
        val data = projectCheckPhone(
            "yuna",
            listOf(
                event(
                    "e1",
                    characterId = "yuna",
                    type = LifeEventType.PHOTO,
                    time = "18:30",
                    title = "晚霞",
                    description = "随手拍下",
                    imageReference = "flowers"
                )
            ),
            seedData = EMPTY_CHECK_PHONE_DATA,
        )
        val photo = data.privateGallery.single()
        assertEquals("晚霞", photo.title)
        assertEquals("18:30", photo.time)
        assertEquals("flowers", photo.imageType)
        assertEquals("随手拍下", photo.note)
    }

    @Test
    fun musicMetadataProjectsToTrack() {
        val data = projectCheckPhone(
            "yuna",
            listOf(
                event(
                    "e1",
                    characterId = "yuna",
                    metadata = mapOf(
                        "music_title" to "雨的演奏",
                        "music_artist" to "Mira",
                        "music_cover" to "rain_window",
                        "music_duration" to "3:42"
                    )
                )
            ),
            seedData = EMPTY_CHECK_PHONE_DATA,
        )
        val track = data.recentlyPlayed.single()
        assertEquals("雨的演奏", track.title)
        assertEquals("Mira", track.artist)
        assertEquals("rain_window", track.albumCoverType)
        assertEquals("3:42", track.duration)
    }

    @Test
    fun factWithoutMetadataGeneratesNothing() {
        val data = projectCheckPhone(
            "yuna",
            listOf(
                event("e1", characterId = "yuna", type = LifeEventType.THOUGHT),
                event("e2", characterId = "yuna", type = LifeEventType.MEAL)
            ),
            seedData = EMPTY_CHECK_PHONE_DATA,
        )
        assertEquals(EMPTY_CHECK_PHONE_DATA, data)
    }

    @Test
    fun seedWorldEventsDoNotGenerateTraces() {
        // New seed PHOTO facts must not create duplicate runtime traces.
        val pulse4 = event("six_mira_photo", type = LifeEventType.PHOTO, imageReference = "flowers")
        val data = projectCheckPhone("mira", listOf(pulse4), seedEventIds = seedIds)
        assertEquals(OfficialCharacters.checkPhoneByCharacter.getValue("mira").privateGallery, data.privateGallery)
    }

    @Test
    fun otherCharactersFactsAreIgnored() {
        val data = projectCheckPhone(
            "yuna",
            listOf(
                event("m1", characterId = "mira", metadata = mapOf("search_query" to "mira的搜索"))
            ),
            seedData = EMPTY_CHECK_PHONE_DATA,
        )
        assertEquals(EMPTY_CHECK_PHONE_DATA, data)
    }

    @Test
    fun runtimeTracesPrependedToSeedData() {
        val data = projectCheckPhone(
            "mira",
            listOf(
                event(
                    "e1",
                    metadata = mapOf("search_query" to "新搜索"),
                    worldMinutesOfDay = 22 * 60,
                    time = "22:00"
                )
            ),
            seedEventIds = seedIds
        )
        assertEquals("新搜索", data.searchHistory.first())
        assertTrue(data.searchHistory.drop(1) == OfficialCharacters.checkPhoneByCharacter.getValue("mira").searchHistory)
    }

}
