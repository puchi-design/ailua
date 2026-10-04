package com.example.data.mock

import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.local.AiluaLocalStore
import com.example.data.model.*

/** Default content is the six-person world. Archived stories remain readable by their original IDs. */
object WorldData {
    val cardHeWenchuan = OfficialCharacters.cardHeWenchuan
    val cardZhouJianye = OfficialCharacters.cardZhouJianye
    val cardPeiXubai = OfficialCharacters.cardPeiXubai
    val cardMira = OfficialCharacters.cardMira
    val cardYuna = OfficialCharacters.cardYuna
    val cardNoa = OfficialCharacters.cardNoa
    val cardYan = OfficialCharacters.cardYan
    val cardYeo = OfficialCharacters.cardYeo
    val allCards = OfficialCharacters.cards
    val archiveCards = OfficialCharacters.archiveCards

    val sampleLoreEntries = listOf(
        LoreEntry("six_lore_street", "青石街", "青石街是一片可步行的城市街区，茶馆、便利店、花店与公共书阁各有营业时间。地点属于公共世界，不证明任何用户到访过。",
            keywords = listOf("青石街", "茶馆", "便利店"), category = "地点"),
        LoreEntry("six_lore_mulan", "木兰茶馆", "街区的公共茶馆，工作日午后较安静。六位角色可能各自来休息，不固定充当任何人的同居空间。",
            keywords = listOf("木兰茶馆", "司康", "午茶"), locationIds = listOf("place_mulan"), category = "地点"),
        LoreEntry("six_lore_moonlight", "月光书阁", "街区公共的独立阅读空间，可以借阅旧书。书阁是公共地点，不定义宋知微的职业，也不证明用户与任何角色有共同的雨夜经历。",
            keywords = listOf("月光书阁", "公共书阁"), locationIds = listOf("place_moonlight"), category = "地点"),
    ) + OfficialCharacters.loreEntries
    val defaultWorldBook = WorldBook("wb_ailua_core", "青石街生活设定", "六位角色各有工作与朋友；不伪造用户共同恋爱记忆", scanDepth = 3, tokenBudget = 800, entries = sampleLoreEntries)

    /** Entries the production prompt can use: public settings or original official card lore. */
    val configurableOfficialLoreEntryIds: Set<String> = (
        sampleLoreEntries.filter { it.characterIds.isEmpty() || it.category == "地点" }.map { it.id } +
            allCards.flatMap { it.data.characterBook?.entries.orEmpty() }.map { it.id }
        ).toSet()

    /** The authored book remains immutable; user switches are projected at read time. */
    fun withOfficialEnabledOverrides(
        book: WorldBook,
        overrides: Map<String, Boolean> = AiluaLocalStore.worldLoreEnabledOverrides.value,
    ): WorldBook = if (overrides.isEmpty()) book else book.copy(entries = book.entries.map { entry ->
        overrides[entry.id]?.let { entry.copy(enabled = it) } ?: entry
    })

    fun activeWorldBook(
        overrides: Map<String, Boolean> = AiluaLocalStore.worldLoreEnabledOverrides.value,
    ): WorldBook = withOfficialEnabledOverrides(defaultWorldBook, overrides)

    // Established public place IDs remain usable by persisted LifeEvent/location references.
    // The new defaults stop assigning the old archive-librarian career to noa.
    val virtualPlaces = LegacyWorldData.virtualPlaces.filter { it.id !in OfficialCharacters.places.map { it.id } }.map { place ->
        when (place.id) {
            "place_street_23" -> place.copy(description = "青石街23号附近的公共生活街区，窗边阁楼可以预约活动。", residentCharacterIds = emptyList(), currentCharacterIds = emptyList(), recentEvents = listOf("街区的窗边亮起灯"))
            "place_mulan" -> place.copy(recentEvents = listOf("茶馆换了今日点心菜单"))
            "place_moonlight" -> place.copy(description = "公共独立书阁与旧物阅读空间。", residentCharacterIds = emptyList(), currentCharacterIds = emptyList(), recentEvents = listOf("公共阅览桌正在整理"))
            "place_flower_shop" -> place.copy(recentEvents = listOf("花店送来一批白色洋桔梗"))
            "place_convenience" -> place.copy(currentCharacterIds = emptyList(), recentEvents = listOf("新品补货到店"))
            "place_old_pages" -> place.copy(residentCharacterIds = emptyList(), currentCharacterIds = emptyList(), description = "保留旧书与修复台的公共书店。旧人物历史记录可继续引用。", recentEvents = listOf("书店正在整理委托"))
            "place_river_walk" -> place.copy(currentCharacterIds = emptyList(), recentEvents = listOf("河岸步道有风"))
            else -> place
        }
    } + OfficialCharacters.places

    /** Archived only: exact original node/choice/bookmark IDs and text are not rewritten. */
    val rainyNightStory = LegacyWorldData.rainyNightStory
    val archivedTheaterStories = listOf(rainyNightStory)
    val defaultTheaterStory = TheaterStory(
        id = "six_story_street_open", title = "街区的傍晚", subtitle = "演出散场以后",
        description = "公开街区活动结束，认识几位各自忙碌的人。此开场不承诺恋爱，也不把朋友当作奖励。",
        characterIds = OfficialCharacters.sixRosterIds, initialNodeId = "six_node_arrive", coverTag = "街区初识",
        nodes = mapOf(
            "six_node_arrive" to TheaterDialogueNode("six_node_arrive", "hewenchuan", "贺闻川", "hewenchuan",
                "活动结束了。这边还有几把没收的椅子，想坐会儿也行。你要先听演出聊两句，还是看看展览？",
                choices = listOf(TheaterChoice("six_choice_music", "聊聊今晚的音乐", "six_node_music", bondIncrease = 0), TheaterChoice("six_choice_exhibit", "看看街区展览", "six_node_exhibit", bondIncrease = 0))),
            "six_node_music" to TheaterDialogueNode("six_node_music", "zhoujianye", "周见野", "zhoujianye",
                "听出来最后那一下了吗？我改了三遍。许朝颜拍照时还嫌我乱动——她说得对。",
                choices = listOf(TheaterChoice("six_choice_girls", "问问周末有什么安排", "six_node_girls", bondIncrease = 0))),
            "six_node_exhibit" to TheaterDialogueNode("six_node_exhibit", "peixubai", "裴叙白", "peixubai",
                "那段回声在桥下录的。宋知微帮我删了一段说明。没有说明，听起来反而清楚。",
                choices = listOf(TheaterChoice("six_choice_friends", "向大家打个招呼", "six_node_girls", bondIncrease = 0))),
            "six_node_girls" to TheaterDialogueNode("six_node_girls", "yuna", "许朝颜", "yuna",
                "晚宁明天还有预约订单，知微要看展。我先把她俩的日程记清，别一兴奋又约个谁都去不了的点。你想来再说，不用现在决定。",
                choices = listOf(TheaterChoice("six_choice_bake", "和苏晚宁聊烘焙", "six_node_bake", bondIncrease = 0), TheaterChoice("six_choice_view", "听宋知微讲展映", "six_node_view", bondIncrease = 0))),
            "six_node_bake" to TheaterDialogueNode("six_node_bake", "mira", "苏晚宁", "mira",
                "明天试一个新配方。多烤出来才叫你拿，订单里的不能随便偷吃。今晚我得先回去把面团醒上。", isEnding = true, endingTitle = "各自回到生活"),
            "six_node_view" to TheaterDialogueNode("six_node_view", "noa", "宋知微", "noa",
                "展映不用先看一套所谓标准解读。先看你自己的感受，看完想聊，我们再聊。", isEnding = true, endingTitle = "下一次再聊"),
        ),
    )

    fun getActiveLore(
        characterId: String? = null,
        locationId: String? = null,
        recentText: String = "",
        lifeEventTitle: String? = null,
        worldBook: WorldBook = activeWorldBook()
    ): List<LoreActivationResult> {
        val results = mutableListOf<LoreActivationResult>()

        worldBook.entries.filter { it.enabled }.forEach { entry ->
            var isActivated = false
            val reasons = mutableListOf<String>()

            // 1. ALWAYS mode
            if (entry.activationMode == LoreActivationMode.ALWAYS) {
                isActivated = true
                reasons.add("常驻激活")
            }

            // 2. LOCATION mode
            if (locationId != null && entry.locationIds.contains(locationId)) {
                isActivated = true
                reasons.add("当前激活：地点 = ${entry.title}")
            }

            // 3. KEYWORD matching in recentText
            val matchedKeywords = entry.keywords.filter { kw ->
                recentText.contains(kw, ignoreCase = true)
            }
            if (matchedKeywords.isNotEmpty()) {
                isActivated = true
                reasons.add("触发词：${matchedKeywords.joinToString(" / ")}")
            }

            // 4. CHARACTER relevance
            if (characterId != null && entry.characterIds.contains(characterId)) {
                if (entry.activationMode == LoreActivationMode.CHARACTER) {
                    isActivated = true
                    reasons.add("角色关联：$characterId")
                }
            }

            // 5. EVENT relevance
            if (lifeEventTitle != null && entry.keywords.any { lifeEventTitle.contains(it) }) {
                isActivated = true
                reasons.add("生活脉搏联动：$lifeEventTitle")
            }

            if (isActivated) {
                results.add(
                    LoreActivationResult(
                        entry = entry,
                        activationReasons = reasons,
                        effectivePriority = entry.priority
                    )
                )
            }
        }

        // Sort by effective priority descending
        return results.sortedByDescending { it.effectivePriority }
    }

    /**
     * Helper to map CharacterCard to CharacterProfile.
     */
    fun cardToProfile(card: CharacterCard): CharacterProfile {
        val d = card.data
        val extension = CharacterRuntimeResolver.resolve(d)
        return CharacterProfile(
            id = if (d.id.isNotBlank()) d.id else "custom_${System.currentTimeMillis()}",
            name = d.name.substringBefore(" (").ifBlank { d.name },
            englishName = if (d.name.contains("(") && d.name.contains(")")) {
                d.name.substringAfter("(").substringBefore(")")
            } else d.name,
            title = extension.identity.occupation.ifBlank { d.tags.firstOrNull() ?: "自定义角色" },
            bio = d.description,
            currentActivity = d.scenario.take(30),
            mood = extension.speech.tone.firstOrNull() ?: "平静",
            location = extension.life.workplace.ifBlank { extension.life.home.ifBlank { "尚未设定" } },
            contextualQuote = d.firstMessage.take(40),
            bondLevel = 1,
            bondName = "初识",
            bondProgress = 0,
            daysTogether = 1,
            energyLevel = 100,
            personalityTags = d.tags,
            memories = emptyList(),
            timeline = emptyList(),
            avatarId = d.avatarReference.ifBlank { extension.visual.avatar.ifBlank { d.id } },
            isOnline = true,
            relationshipType = extension.relationship.initialRelation.ifBlank { "初识" }
        )
    }
}
