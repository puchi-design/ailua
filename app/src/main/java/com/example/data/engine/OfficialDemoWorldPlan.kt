package com.example.data.engine

import com.example.data.model.LifeEventType
import com.example.data.model.PlannedWorldAction
import com.example.data.model.WorldPlan

/** Separate IDs keep old scheduled actions and their fired markers intact. */
object OfficialDemoWorldPlan {
    val actions = listOf(
        action("yan_close", "yan", 21 * 60 + 45, "沈砚收起修复工具", "最后一册书还没干，他把窗缝关小了一点。", "旧页书店", LifeEventType.THOUGHT),
        action("yeo_photo", "yeo", 22 * 60, "周野挑选今天的照片", "风把倒影吹散了。他偏偏留下了这张失焦的底片。", "河岸摄影工作室", LifeEventType.PHOTO),
        action("noa_note", "noa", 22 * 60 + 20, "诺亚补完档案索引", "页码有一处错了。他在便签上写了更正，没有再附一段解释。", "月光书阁", LifeEventType.THOUGHT),
        action("yeo_call", "yeo", 22 * 60 + 45, "周野打来电话", "片子都备份好了。要不要听听我今天拍照时遇见的事？", "河岸摄影工作室", LifeEventType.MESSAGE,
            ScheduledActionType.INCOMING_CALL),
        action("yan_sleep", "yan", 23 * 60 + 40, "沈砚关掉台灯", "茶杯洗好，手机放在玄关充电。他结束了今天的工作。", "旧页书店楼上的小公寓", LifeEventType.SLEEP),
    )

    /** A dated first-evening plan, persisted before the runtime checks its future queue. */
    fun createPlan(date: String, minutes: Int): WorldPlan = WorldPlan(
        createdWorldDate = date,
        createdMinutes = minutes,
        // An existing clock can already be later in the evening. Never replay missed facts
        // or silently move this one-time introduction to the following day.
        actions = actions.filter { it.triggerTimeMinutes > minutes }.map { action ->
            PlannedWorldAction(
                id = action.id, characterId = action.characterId,
                triggerWorldDate = date, triggerMinutes = action.triggerTimeMinutes,
                type = action.type, lifeEventType = action.lifeEventType,
                title = action.title, description = action.description,
                location = requireNotNull(action.location), metadata = action.metadata,
                imageReference = action.imageReference, relatedCharacterIds = action.relatedCharacterIds,
            )
        },
    )

    private fun action(id: String, character: String, minute: Int, title: String, description: String,
        location: String, event: LifeEventType, type: ScheduledActionType = ScheduledActionType.LIFE_EVENT) =
        ScheduledWorldAction("official_sched_$id", minute, "%02d:%02d".format(minute / 60, minute % 60), type,
            character, "official_$id", title, description, lifeEventType = event, location = location)
}
