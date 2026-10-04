package com.example.data.mock

import com.example.data.model.*

/** New default seeds only. LegacyMockData is an archive, never a fresh-install content pool. */
object MockData {
    val unifiedLifeEvents = OfficialCharacters.lifeEvents
    val diaryEntries = OfficialCharacters.diaryEntries
    val allCharacters = OfficialCharacters.profiles + LegacyOfficialCharacters.profiles.filterKeys { it == "yan" || it == "yeo" }
    val sampleCharacter = OfficialCharacters.profiles.getValue("hewenchuan")
    val characterMira = OfficialCharacters.profiles.getValue("mira")
    val characterYuna = OfficialCharacters.profiles.getValue("yuna")
    val characterNoa = OfficialCharacters.profiles.getValue("noa")
    val characterYan = LegacyOfficialCharacters.profiles.getValue("yan")
    val characterYeo = LegacyOfficialCharacters.profiles.getValue("yeo")
    val appLibraryList = LegacyMockData.appLibraryList

    val contactsList = OfficialCharacters.cards.map { card ->
        val profile = OfficialCharacters.profiles.getValue(card.data.id)
        ContactItem("six_contact_${profile.id}", profile.id, profile.name, profile.name, profile.avatarId,
            profile.currentActivity, if (profile.id in OfficialCharacters.friendshipIds) "朋友" else "初识", 1,
            "今天", onlineState = "有自己的生活")
    }

    val conversationsList = OfficialCharacters.cards.map { card ->
        Conversation("six_conversation_${card.data.id}", ConversationType.PRIVATE, card.data.name,
            characterId = card.data.id, latestMessage = card.data.firstMessage, latestTime = "初识",
            avatarId = card.data.avatarReference, characterStatus = OfficialCharacters.profiles.getValue(card.data.id).currentActivity)
    } + Conversation("six_friends_group", ConversationType.GROUP, "周末见", memberIds = OfficialCharacters.friendshipIds,
        latestMessage = "先对日程，想参加再说。", latestTime = "今天")

    // Authored character-to-character examples, never inserted as user chat turns or memory.
    val sampleGroupChatMessages = listOf(
        ChatMessage("six_group_yuna_1", MessageSender.CHARACTER, "yuna", "许朝颜", text = "周末看展吗？先问日程，这次不擅自订票。", timestamp = "18:10"),
        ChatMessage("six_group_mira_1", MessageSender.CHARACTER, "mira", "苏晚宁", text = "上午有订单，下午可以。我先把点心送完。", timestamp = "18:12"),
        ChatMessage("six_group_noa_1", MessageSender.CHARACTER, "noa", "宋知微", text = "两点以后。别选需要排两小时队的。", timestamp = "18:14"),
    )

    val relationsList = listOf(
        RelationLink("six_relation_mira_hewenchuan", "mira", "hewenchuan", "苏晚宁", "贺闻川", "街区朋友", 55, "讨论工作室收纳", "各有自己的工作，不证明用户参加过"),
        RelationLink("six_relation_yuna_zhoujianye", "yuna", "zhoujianye", "许朝颜", "周见野", "互相嘴欠的朋友", 65, "演出照片与灯架归还", "拍摄协作，不替用户安排恋爱"),
        RelationLink("six_relation_noa_peixubai", "noa", "peixubai", "宋知微", "裴叙白", "专业协作", 50, "讨论纪录片混音", "能聊专业，也觉得对方难沟通"),
        RelationLink("six_relation_mira_yuna", "mira", "yuna", "苏晚宁", "许朝颜", "朋友", 70, "约周末展览", "三人有自己的朋友群"),
        RelationLink("six_relation_yuna_noa", "yuna", "noa", "许朝颜", "宋知微", "朋友", 70, "对齐日程", "不要求朋友关系排他"),
        RelationLink("six_relation_noa_mira", "noa", "mira", "宋知微", "苏晚宁", "朋友", 70, "聊订单与休息", "观点不同也能互相照顾"),
    )

    val checkPhoneData = OfficialCharacters.checkPhoneByCharacter.getValue("mira")
    val sampleMoments: List<MomentPost> get() = getMomentsFromLifeEvents()
    val sampleChatMessages: List<ChatMessage> get() = getChatMessagesForCharacter("hewenchuan")

    fun getTimelineForCharacter(characterId: String): List<TimelineEvent> =
        unifiedLifeEvents.filter { it.characterId == characterId || characterId in it.relatedCharacterIds }.map { event ->
            TimelineEvent(event.id, event.time, event.title, event.description, location = event.location, relatedCharacterIds = event.relatedCharacterIds)
        }.ifEmpty { allCharacters[characterId]?.timeline.orEmpty() }

    fun getChatMessagesForCharacter(characterId: String): List<ChatMessage> {
        val card = OfficialCharacters.cards.firstOrNull { it.data.id == characterId }
            ?: return if (characterId in setOf("yan", "yeo")) LegacyMockData.getChatMessagesForCharacter(characterId) else emptyList()
        return listOf(ChatMessage("six_intro_${card.data.id}", MessageSender.CHARACTER, card.data.id, card.data.name,
            text = card.data.firstMessage, timestamp = "初识"))
    }

    fun getMomentsFromLifeEvents(): List<MomentPost> = unifiedLifeEvents.filter {
        it.type == LifeEventType.MOMENT || it.type == LifeEventType.PHOTO
    }.map { event ->
        val author = OfficialCharacters.profiles.getValue(event.characterId)
        MomentPost("six_moment_${event.id}", event.characterId, author.name, event.time, "生活", event.location.orEmpty(),
            event.description, event.imageReference.orEmpty(), likesCount = 0, comments = emptyList())
    }
}
