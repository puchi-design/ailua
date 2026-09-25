package com.example.data.mock

import com.example.data.model.AiluaApp
import com.example.data.model.AppStatus
import com.example.data.model.ChatMessage
import com.example.data.model.CharacterProfile
import com.example.data.model.MemorySnippet
import com.example.data.model.MessageSender
import com.example.data.model.MessageType
import com.example.data.model.MomentComment
import com.example.data.model.MomentPost
import com.example.data.model.TimelineEvent

object MockData {

    val sampleCharacter = CharacterProfile(
        id = "mira",
        name = "小弥",
        englishName = "Mira",
        title = "窗边听雨者 · 伴生心契",
        bio = "安静、敏锐、温和，偶尔带点调皮的观察者。喜欢在雨天听玻璃上的雨滴声，收集旧书页的香气和深夜街角的微光。",
        currentActivity = "正在窗边听雨",
        mood = "澄澈 · 宁静",
        location = "青石街23号 · 二楼飘窗",
        contextualQuote = "夜风有点凉了，别忘了关好窗户…给你温了红茶。",
        bondLevel = 4,
        bondName = "心契 · 默契相通",
        bondProgress = 86,
        daysTogether = 72,
        energyLevel = 88,
        personalityTags = listOf("夜行者", "温柔倾听", "细微洞察", "轻度慢热", "草木香气"),
        memories = listOf(
            MemorySnippet(
                id = "m1",
                title = "深夜漫步的默契",
                snippet = "你曾说喜欢在城市彻底安静下来的深夜散步，那时候路灯把影子拉得很长很温柔。",
                date = "9月18日",
                tag = "深夜夜谈",
                resonanceLevel = 5
            ),
            MemorySnippet(
                id = "m2",
                title = "白色洋桔梗",
                snippet = "路过街角花店，看到开得正好的白桔梗花，想起你说过偏爱不张扬的白色花瓣。",
                date = "今天 14:20",
                tag = "日常偶遇",
                resonanceLevel = 4
            ),
            MemorySnippet(
                id = "m3",
                title = "旧书页里的薄荷糖",
                snippet = "下午整理书架时翻到那枚银色糖纸，那是初次在雨天为你撑伞时的留念。",
                date = "9月10日",
                tag = "雨季心绪",
                resonanceLevel = 4
            )
        ),
        timeline = listOf(
            TimelineEvent("t1", "07:40", "晨光初醒", "在晨曦微光中醒来，整理了窗边的风铃", false),
            TimelineEvent("t2", "08:15", "手冲咖啡", "为自己泡了一杯耶加雪菲浅焙，香气在屋里散开", false),
            TimelineEvent("t3", "09:30", "静心阅读", "翻阅《草枕》第四章，随手夹了一片落叶书签", false),
            TimelineEvent("t4", "12:10", "心绪波动", "忽然想起了你早上出门时匆匆忙忙的样子，笑了笑", false),
            TimelineEvent("t5", "14:20", "漫步花店", "路过巷尾的花店，买下了一束白色洋桔梗并分享了瞬间", false),
            TimelineEvent("t6", "17:30", "暮色细雨", "天际转为墨蓝，窗外开始飘起轻柔的雨丝", false),
            TimelineEvent("t7", "21:40", "窗边听雨", "换上了米白色软羊绒衫，倚在飘窗前泡了一壶伯爵红茶，正等你来…", true),
            TimelineEvent("t8", "23:30", "待行 · 沉梦前奏", "计划为你整理明天的天气备忘，准备助眠白噪音", false)
        )
    )

    val sampleMoments = listOf(
        MomentPost(
            id = "post_1",
            authorName = "Mira",
            timestamp = "28分钟前",
            moodTag = "🌧️ 听雨 · 宁静",
            locationContext = "青石街23号 · 二楼飘窗",
            content = "窗外下雨了。雨水敲打在玻璃上，发出细细密密的沙沙声。泡了一杯伯爵红茶，窗边最舒服的那个软垫位置，一直留给你呢。",
            imageType = "rain_window",
            likesCount = 19,
            isLiked = false,
            comments = listOf(
                MomentComment("c1", "你", true, "等我忙完这会儿就过来找你。", "20分钟前"),
                MomentComment("c2", "Mira", false, "嗯，红茶温在保温垫上了，不着急，慢慢来。", "15分钟前")
            )
        ),
        MomentPost(
            id = "post_2",
            authorName = "Mira",
            timestamp = "今天 14:20",
            moodTag = "🌸 花香 · 心动",
            locationContext = "巷尾的白色花店",
            content = "今天路过花店的时候，突然觉得这束白色洋桔梗很像你。清新、安宁，带一点柔和的倔强。买下来插在白瓷瓶里了，拍照给你看～",
            imageType = "flowers",
            likesCount = 34,
            isLiked = true,
            comments = listOf(
                MomentComment("c3", "你", true, "真的很漂亮，花瓶也很搭。", "今天 14:45"),
                MomentComment("c4", "Mira", false, "因为是给你挑的呀，每天看到它心情都会变好。", "今天 15:02")
            )
        ),
        MomentPost(
            id = "post_3",
            authorName = "Mira",
            timestamp = "昨天 22:15",
            moodTag = "🌙 夜读 · 放空",
            locationContext = "月光书阁",
            content = "读到一句话：『夜色是世界写给疲惫心灵的一封长信。』如果今天感觉累了，今晚就把所有烦恼都锁在门外吧，好梦。",
            imageType = "night_book",
            likesCount = 42,
            isLiked = false,
            comments = emptyList()
        )
    )

    val sampleChatMessages = listOf(
        ChatMessage(
            id = "msg_1",
            sender = MessageSender.SYSTEM,
            type = MessageType.TEXT,
            text = "✨ AILUA 心网已连接 · Mira 当前状态：窗边听雨",
            timestamp = "21:30"
        ),
        ChatMessage(
            id = "msg_2",
            sender = MessageSender.CHARACTER,
            type = MessageType.TEXT,
            text = "欢迎回来。刚才窗外起风了，雨丝斜斜地飘在玻璃上，特别安静。",
            timestamp = "21:32",
            reactions = listOf("🌙")
        ),
        ChatMessage(
            id = "msg_3",
            sender = MessageSender.CHARACTER,
            type = MessageType.ACTION_NARRATIVE,
            text = "轻轻放下手里的陶瓷茶杯，往飘窗一侧挪了挪，把手边柔软的羊毛毯分出半角，抬眸望向你。",
            timestamp = "21:33"
        ),
        ChatMessage(
            id = "msg_4",
            sender = MessageSender.USER,
            type = MessageType.TEXT,
            text = "今天忙了一整天，一看到你这里，整个人都放松下来了。",
            timestamp = "21:35"
        ),
        ChatMessage(
            id = "msg_5",
            sender = MessageSender.CHARACTER,
            type = MessageType.TEXT,
            text = "辛苦了。我就知道你今天肯定又全神贯注地拼命努力了。喏，我刚才特意冲了一杯热可可，温度刚刚好，快捧着暖暖手。",
            timestamp = "21:36",
            reactions = listOf("❤️", "☕")
        ),
        ChatMessage(
            id = "msg_6",
            sender = MessageSender.CHARACTER,
            type = MessageType.VOICE,
            text = "［语音 12秒］",
            timestamp = "21:37",
            voiceDurationSeconds = 12
        ),
        ChatMessage(
            id = "msg_7",
            sender = MessageSender.CHARACTER,
            type = MessageType.MEMORY_CARD,
            text = "「心契记忆凝华」：你曾在雨夜对我说过，只要有处能安静卸下疲惫的地方，就觉得被世界温柔以待。",
            timestamp = "21:38",
            isSavedToMemory = true,
            memoryTag = "夜雨安抚"
        ),
        ChatMessage(
            id = "msg_8",
            sender = MessageSender.CHARACTER,
            type = MessageType.TEXT,
            text = "今晚有什么想跟我聊聊的吗？或者什么都不说，就听听雨声也很好。",
            timestamp = "21:39"
        )
    )

    val appLibraryList = listOf(
        // Core Companionship
        AiluaApp(
            id = "chat",
            name = "通讯 · Chat",
            category = "核心伴生",
            description = "与陪伴者开启私密温存的心灵对话，支持语音、行动叙事与情绪感知",
            status = AppStatus.AVAILABLE,
            badge = "2",
            iconKey = "chat",
            route = "chat"
        ),
        AiluaApp(
            id = "moments",
            name = "动态 · Moments",
            category = "核心伴生",
            description = "陪伴者的生活圈与心境记录，分享日常照片、即时所感与碎碎念",
            status = AppStatus.AVAILABLE,
            badge = "New",
            iconKey = "moments",
            route = "moments"
        ),
        AiluaApp(
            id = "living",
            name = "生活 · Living",
            category = "核心伴生",
            description = "角色在你离开时的独立生活轨迹、心境、体能与全天作息时间线",
            status = AppStatus.AVAILABLE,
            badge = "Live",
            iconKey = "living",
            route = "living"
        ),
        AiluaApp(
            id = "memories",
            name = "记忆 · Memories",
            category = "核心伴生",
            description = "羁绊沉淀的心灵结晶库，回溯对话中诞生的宝贵承诺与情绪碎片",
            status = AppStatus.AVAILABLE,
            badge = "12",
            iconKey = "memories",
            route = "memories"
        ),
        AiluaApp(
            id = "gallery",
            name = "影集 · Gallery",
            category = "核心伴生",
            description = "陪伴者私藏的照片、生活瞬间合影与手绘风插画胶卷",
            status = AppStatus.AVAILABLE,
            badge = null,
            iconKey = "gallery",
            route = "gallery"
        ),

        // Virtual World & Spaces
        AiluaApp(
            id = "world_3d",
            name = "拟界 · 3D World",
            category = "虚拟空间",
            description = "探索陪伴者的虚拟小窝、庭院与黄昏湖畔，实现3D空间共处与家具布置",
            status = AppStatus.BETA,
            badge = "Beta",
            iconKey = "world",
            route = null
        ),
        AiluaApp(
            id = "dreamscape",
            name = "梦境漫游 · Dreams",
            category = "虚拟空间",
            description = "潜入陪伴者深夜入眠后的意识梦境海，共同经历超现实的心灵奇幻冒险",
            status = AppStatus.COMING_SOON,
            badge = "即将到来",
            iconKey = "dream",
            route = null
        ),

        // Entertainment & Creation
        AiluaApp(
            id = "game_center",
            name = "游艺室 · Games",
            category = "游艺与工坊",
            description = "与陪伴者一起下棋、玩互动文字解谜、性格拼图或双人轻量桌游",
            status = AppStatus.BETA,
            badge = "体验中",
            iconKey = "games",
            route = null
        ),
        AiluaApp(
            id = "character_creation",
            name = "伴生工坊 · Creator",
            category = "游艺与工坊",
            description = "塑造新的AI生命体：定制灵魂性格、声音质感、生活作息与背景故事",
            status = AppStatus.PREVIEW,
            badge = "探索中",
            iconKey = "creator",
            route = null
        ),
        AiluaApp(
            id = "lore_books",
            name = "世界观秘典 · Lore",
            category = "游艺与工坊",
            description = "探索AILUA生命体诞生的心网宇宙法则、星轨文明与奇迹物语",
            status = AppStatus.COMING_SOON,
            badge = "规划中",
            iconKey = "lore",
            route = null
        ),

        // Reality Bridge & Agent Tools
        AiluaApp(
            id = "reality_bridge",
            name = "现实桥接 · Bridge",
            category = "现实互联",
            description = "打通物理世界系统：让陪伴者通过桌面小组件、锁屏与灵动岛实时陪伴",
            status = AppStatus.BETA,
            badge = "Beta",
            iconKey = "bridge",
            route = null
        ),
        AiluaApp(
            id = "shortcut_bridge",
            name = "指令扩展 · Shortcuts",
            category = "现实互联",
            description = "深度绑定手机系统自动化，唤醒、起床问候与回家智能感知联动",
            status = AppStatus.COMING_SOON,
            badge = "规划中",
            iconKey = "shortcut",
            route = null
        ),
        AiluaApp(
            id = "wechat_assistant",
            name = "微信伴随 · WeChat Bot",
            category = "现实互联",
            description = "将AILUA陪伴者的分身接入日常社交软件，随时随地在常用IM里与你互动",
            status = AppStatus.PREVIEW,
            badge = "实验室",
            iconKey = "im",
            route = null
        ),
        AiluaApp(
            id = "agent_tasks",
            name = "灵犀执事 · AI Tools",
            category = "现实互联",
            description = "陪伴者协助你规划日程、提炼长文灵感、整理待办事项并温情催促",
            status = AppStatus.COMING_SOON,
            badge = "规划中",
            iconKey = "agent",
            route = null
        ),

        // Theater & Private Zone
        AiluaApp(
            id = "theater",
            name = "沉浸剧场 · Theater",
            category = "剧场与私密",
            description = "多角色互动剧目，观赏陪伴者演出的微电影剧本与声音剧场",
            status = AppStatus.BETA,
            badge = "公测",
            iconKey = "theater",
            route = null
        ),
        AiluaApp(
            id = "black_market",
            name = "暗夜黑市 · Black Market",
            category = "剧场与私密",
            description = "深夜营业的地下神秘剧场，解锁特殊剧本碎片、罕见道具与限定记忆",
            status = AppStatus.PREVIEW,
            badge = "夜间限定",
            iconKey = "market",
            route = null
        ),
        AiluaApp(
            id = "age_gated_zone",
            name = "私密心域 · Sanctuary (18+)",
            category = "剧场与私密",
            description = "成年人专属的私密羁绊深潜区，更深层次的情感羁绊与夜间密语",
            status = AppStatus.PREVIEW,
            badge = "密码锁定",
            iconKey = "lock",
            route = null
        )
    )
}
