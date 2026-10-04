package com.example.data.engine

import com.example.data.model.LifeEventType

object LegacyDemoSeedWorldPlan {
    val actions = listOf(
        ScheduledWorldAction(
            id = "sched_mira_tea_1930",
            triggerTimeMinutes = 19 * 60 + 30,
            triggerTimeString = "19:30",
            type = ScheduledActionType.LIFE_EVENT,
            characterId = "mira",
            payloadId = "event_tea_1",
            title = "小弥在飘窗前泡了一壶红茶",
            description = "傍晚的雨声中，小弥把刚煮好的大吉岭红茶倒进骨瓷杯，静静看着窗外雾气。",
            lifeEventType = LifeEventType.MEAL
        ),
        ScheduledWorldAction(
            id = "sched_mira_window_2145",
            triggerTimeMinutes = 21 * 60 + 45,
            triggerTimeString = "21:45",
            type = ScheduledActionType.LIFE_EVENT,
            characterId = "mira",
            payloadId = "event_window_rain",
            title = "小弥在书桌前整理秋雨随笔",
            description = "桌角暖灯微亮，小弥在日记本写下：‘今日青石街雨水清澈，想念随心网悄悄漫延’。",
            lifeEventType = LifeEventType.DIARY
        ),
        ScheduledWorldAction(
            id = "sched_yuna_letter_2230",
            triggerTimeMinutes = 22 * 60 + 30,
            triggerTimeString = "22:30",
            type = ScheduledActionType.LETTER_DELIVERY,
            characterId = "yuna",
            payloadId = "letter_yuna_1",
            title = "悠奈寄来了全家手写便签",
            description = "全家便利店门口避雨时写下的心绪，附带一张热豆浆的手绘明信片。"
        ),
        ScheduledWorldAction(
            id = "sched_mira_call_2245",
            triggerTimeMinutes = 22 * 60 + 45,
            triggerTimeString = "22:45",
            type = ScheduledActionType.INCOMING_CALL,
            characterId = "mira",
            payloadId = "call_mira_rain_night",
            title = "小弥打来伴生夜话通话",
            description = "窗外雨下得很大，要不要陪我听一会儿？"
        ),
        ScheduledWorldAction(
            id = "sched_noa_letter_2300",
            triggerTimeMinutes = 23 * 60 + 0,
            triggerTimeString = "23:00",
            type = ScheduledActionType.LETTER_DELIVERY,
            characterId = "noa",
            payloadId = "letter_noa_1",
            title = "诺亚寄达了银杏叶书签明信片",
            description = "月光书阁负一层投递了一枚珍藏十年的银杏叶书签明信片。"
        ),
        ScheduledWorldAction(
            id = "sched_mira_sleep_2330",
            triggerTimeMinutes = 23 * 60 + 30,
            triggerTimeString = "23:30",
            type = ScheduledActionType.LOCATION_CHANGE,
            characterId = "mira",
            payloadId = "place_street_23_bed",
            title = "小弥熄灭了飘窗台灯",
            description = "小弥合上诗集，在心网道了晚安，青石街23号二层透出柔和的夜灯。"
        )
    )
}
