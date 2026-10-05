package com.example.data.projection.checkphone

import com.example.data.mock.MockData
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.isUserActivity
import com.example.data.model.sortedChronologically

/** A virtual phone app appears here only when an actual character fact implies use. */
fun projectCharacterPhoneUsage(
    characterId: String,
    events: List<LifeEvent>,
    seedEventIds: Set<String> = MockData.unifiedLifeEvents.mapTo(HashSet()) { it.id },
): List<CharacterPhoneUsage> = events
    .filter { it.characterId == characterId && !it.isUserActivity() &&
        (it.id !in seedEventIds || it.metadata["seed"] == "six_roster_v1") }
    .sortedChronologically().reversed()
    .flatMap { event ->
        val apps = buildList {
            if (!event.metadata["search_query"].isNullOrBlank()) add("搜索")
            if (!event.metadata["draft"].isNullOrBlank()) add("消息")
            if (!event.metadata["note"].isNullOrBlank()) add("备忘录")
            if (!event.metadata["music_title"].isNullOrBlank()) add("音乐")
            if (event.type == LifeEventType.PHOTO) add("相册")
            if (event.sourceAppId == "moments" && event.type == LifeEventType.MOMENT) add("动态")
            if (event.sourceAppId == "diary" && event.type == LifeEventType.DIARY) add("日记")
            if (event.sourceAppId == "call" && event.sourceRefId != null) add("通话")
        }
        apps.distinct().mapIndexed { index, name ->
            CharacterPhoneUsage("usage:${event.id}:$index", name, event.time, event.id)
        }
    }
    .distinctBy { it.id }
    .take(30)
