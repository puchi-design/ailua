package com.example.data.engine

import com.example.data.model.LifeEventType
import com.example.data.model.PlannedWorldAction
import com.example.data.model.WorldPlan

/** New six_* IDs never relabel an old action or its fired marker. */
object OfficialDemoWorldPlan {
    val actions = listOf(
        action("peixubai_mix", "peixubai", 22 * 60 + 10, "裴叙白归档城市采样", "这一轨保留了桥洞里的半秒安静，不需要把每处都填满。", "蓝桥声音工作室", LifeEventType.THOUGHT),
        action("mira_pack", "mira", 22 * 60 + 15, "苏晚宁装好明天的点心", "预约盒上写好名字，余下两块留给自己。", "晚宁烘焙工作室", LifeEventType.MEAL),
        action("yuna_plan", "yuna", 22 * 60 + 20, "许朝颜确认周末展览", "先对朋友们的日程，没确认前不替任何人订票。", "河岸摄影工作室", LifeEventType.SOCIAL),
        action("noa_edit", "noa", 22 * 60 + 25, "宋知微删掉多余的旁白", "采访对象没有说过那句话，不能为了文采替他发言。", "知微影像编辑室", LifeEventType.THOUGHT),
        action("zhoujianye_call", "zhoujianye", 22 * 60 + 45, "周见野想打个电话", "排练存好了。现在方便聊两句吗？不方便就明天说。", "回声排练室", LifeEventType.MESSAGE, ScheduledActionType.INCOMING_CALL),
        action("hewenchuan_sleep", "hewenchuan", 23 * 60 + 40, "贺闻川结束今天的工作", "台灯关了，手机留在玄关充电。他准备休息。", "青石街北侧的独居公寓", LifeEventType.SLEEP),
    )

    fun createPlan(date: String, minutes: Int): WorldPlan = WorldPlan(
        createdWorldDate = date, createdMinutes = minutes,
        // Never replay missed introductions or silently move them to tomorrow.
        actions = actions.filter { it.triggerTimeMinutes > minutes }.map { action ->
            PlannedWorldAction(
                id = action.id, characterId = action.characterId, triggerWorldDate = date,
                triggerMinutes = action.triggerTimeMinutes, type = action.type, lifeEventType = action.lifeEventType,
                title = action.title, description = action.description, location = requireNotNull(action.location),
                metadata = action.metadata, imageReference = action.imageReference, relatedCharacterIds = action.relatedCharacterIds,
            )
        },
    )

    private fun action(id: String, character: String, minute: Int, title: String, description: String,
        location: String, event: LifeEventType, type: ScheduledActionType = ScheduledActionType.LIFE_EVENT) =
        ScheduledWorldAction("six_sched_$id", minute, "%02d:%02d".format(minute / 60, minute % 60), type,
            character, "six_$id", title, description, lifeEventType = event, location = location,
            metadata = emptyMap())
}
