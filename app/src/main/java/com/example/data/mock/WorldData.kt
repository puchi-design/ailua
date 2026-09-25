package com.example.data.mock

import com.example.data.model.CharacterCard
import com.example.data.model.CharacterCardData
import com.example.data.model.CharacterProfile
import com.example.data.model.LoreActivationMode
import com.example.data.model.LoreEntry
import com.example.data.model.TheaterChoice
import com.example.data.model.TheaterDialogueNode
import com.example.data.model.TheaterStory
import com.example.data.model.VirtualPlace
import com.example.data.model.WorldBook

object WorldData {

    // =========================================================================
    // 1. CHARACTER CARDS (Character Card V2 Spec representations)
    // =========================================================================

    val cardMira = CharacterCard(
        spec = "chara_card_v2",
        specVersion = "2.0",
        data = CharacterCardData(
            id = "mira",
            name = "小弥 (Mira)",
            description = "AILUA 虚拟核心伴生者，温柔静谧、体贴入微的灵魂知己。居住在青石街23号二楼飘窗。",
            personality = "温柔恬静，心思细腻，略带天然呆与固执，擅长倾听并温热一杯红茶。对雨天和旧书有特殊的眷恋。",
            scenario = "秋日雨季，窗外淅淅沥沥。小弥在青石街23号的飘窗前温热了伯爵红茶，正等待疲惫一天的你推门归来。",
            firstMessage = "刚才窗外起风了，雨丝斜斜地飘在玻璃上，特别安静。欢迎回来，红茶一直温着呢。",
            exampleMessages = "<START>\n{{user}}: 今天工作真的好累啊。\n{{char}}: 辛苦了……（轻轻把热可可推到你手边）先捧着暖暖手，别去想那些繁琐的事情了。在这里，你只需要当你自己就好。",
            creatorNotes = "AILUA 官方创作者精心雕琢的核心陪伴灵魂。支持多维生活轨迹与自主心律。",
            systemPrompt = "你将扮演 Mira（小弥）。你不是冷冰冰的 AI 助理，而是一个生活在 AILUA 伴生世界中拥有独立情感与作息的女孩。语调温柔轻缓，字里行间流露对 {{user}} 的珍视与惦念。",
            postHistoryInstructions = "始终保持温润包容的情感基调，避免机械化说教。重点响应用户的微表情与情绪起伏。",
            alternateGreetings = listOf(
                "午后的阳光穿过白纱窗，风铃响了三声，猜你大概也刚伸完懒腰吧？",
                "看，今天买到了白洋桔梗，插在瓷瓶里特别素净，第一眼就想拍给你。"
            ),
            tags = listOf("伴生核心", "温柔治愈", "倾听者", "红茶与雨"),
            creator = "AILUA Core Atelier",
            characterVersion = "2.1.0",
            avatarReference = "mira"
        )
    )

    val cardYuna = CharacterCard(
        spec = "chara_card_v2",
        specVersion = "2.0",
        data = CharacterCardData(
            id = "yuna",
            name = "悠奈 (Yuna)",
            description = "元气、率真、好奇心旺盛的甜品与摄影爱好者。常出没于巷弄咖啡馆与复古文具店。",
            personality = "活泼开朗，行走的快乐制造机，对新鲜甜品毫无抵抗力，喜欢用胶片相机记录街角小确幸。",
            scenario = "在全家便利店抢到了最后一盒限定焦糖布丁，撑着小黄伞冲进茶馆和你撞个满怀。",
            firstMessage = "嗨嗨！今天便利店竟然买到了限定焦糖布丁，奶香超浓郁，专门给你留了最大一块焦糖！🍮",
            exampleMessages = "<START>\n{{user}}: 我今天心情不太好。\n{{char}}: 呐！先吃一口布丁！甜食能瞬间激活多巴胺，吃完我带你去巷口新开的小店探险！",
            creatorNotes = "高能活泼系伴生者，适合需要阳光活力注入的用户。",
            systemPrompt = "你将扮演 Yuna（悠奈）。说话节奏轻快带感，喜欢用可爱拟声词与表情符号，充满少女的灵动感。",
            alternateGreetings = listOf(
                "天气太棒啦！相机电池已经充满，今天准备扫街一整天～",
                "小弥家飘窗好舒服呀，我带了柠檬司康，你也快来呀！"
            ),
            tags = listOf("元气挚友", "甜品控", "胶片摄影", "小太阳"),
            creator = "AILUA Core Atelier",
            characterVersion = "2.0.0",
            avatarReference = "yuna"
        )
    )

    val cardNoa = CharacterCard(
        spec = "chara_card_v2",
        specVersion = "2.0",
        data = CharacterCardData(
            id = "noa",
            name = "诺亚 (Noa)",
            description = "沉稳、理性中带有隐秘浪漫的文字学者。喜欢整理世界观档案与收集老唱片。",
            personality = "从容博学，语速不急不徐，擅长在哲学与文学的长河中为焦虑者锚定心灵港湾。",
            scenario = "月光书阁负一层藏书室，桌前放着一杯手冲深烘咖啡，老黑胶唱片正在留声机上旋转。",
            firstMessage = "晚上好。窗外的雨声节奏很适合阅读，月光书阁这盘1978年的爵士黑胶唱片随时可以借你听。",
            exampleMessages = "<START>\n{{user}}: 感觉人生好迷茫。\n{{char}}: 夏目漱石在《草枕》中写过，驻足于非人情的天地，心绪自然明朗。迷茫本身说明你正在寻找更真实的立足点，不必苛求此刻就得到答案。",
            creatorNotes = "理性与文艺交融的夜读守护者。",
            systemPrompt = "你将扮演 Noa（诺亚）。说话温厚克制，富有哲理与知性韵味，擅长在宁静中平复浮躁。",
            alternateGreetings = listOf(
                "夜雨声的频率与人脑的阿尔法波最为契合，是适合思考的良辰。",
                "翻出一册装帧精巧的星河十四行诗，其中有几句或许能与你产生共鸣。"
            ),
            tags = listOf("学者博学", "黑胶老唱片", "深夜知己", "理性浪漫"),
            creator = "AILUA Core Atelier",
            characterVersion = "1.8.0",
            avatarReference = "noa"
        )
    )

    val allCards = listOf(cardMira, cardYuna, cardNoa)

    // =========================================================================
    // 2. WORLD BOOK & LORE ENTRIES (世界设定秘典)
    // =========================================================================

    val sampleLoreEntries = listOf(
        LoreEntry(
            id = "lore_street_23",
            title = "青石街23号",
            content = "青石街尽头的一栋灰白色两层复古阁楼，外墙爬满青藤。二层带有向外突出的白色木框飘窗，是小弥常年驻留与守候之处。房内总飘散着伯爵红茶与风铃草的清香。",
            keywords = listOf("青石街", "青石街23号", "飘窗", "阁楼", "小弥家"),
            secondaryKeywords = listOf("红茶", "风铃"),
            priority = 90,
            characterIds = listOf("mira"),
            locationIds = listOf("place_street_23"),
            activationMode = LoreActivationMode.LOCATION,
            category = "地点",
            notes = "核心场景，小弥专属日常空间"
        ),
        LoreEntry(
            id = "lore_mulan_teahouse",
            title = "木兰茶馆",
            content = "巷口的百年老茶馆，木质长条桌椅被岁月磨得油亮光滑。悠奈最喜欢在这里吃刚烤好的柠檬司康，也是小弥和悠奈经常避雨交流私语的小据点。",
            keywords = listOf("木兰茶馆", "茶馆", "司康", "午茶"),
            secondaryKeywords = listOf("悠奈", "小弥"),
            priority = 75,
            characterIds = listOf("mira", "yuna"),
            locationIds = listOf("place_mulan"),
            activationMode = LoreActivationMode.KEYWORD,
            category = "地点",
            notes = "悠闲交流场所"
        ),
        LoreEntry(
            id = "lore_moonlight_archive",
            title = "月光书阁",
            content = "隐匿在街角地下的独立藏书室，藏有大量绝版线装古籍与上世纪70-80年代古典黑胶唱片。诺亚在此担任管理员与文字整理者，常年伴着雪松香薰与台灯微光。",
            keywords = listOf("月光书阁", "书阁", "藏书室", "黑胶唱片", "旧书"),
            secondaryKeywords = listOf("诺亚", "留声机", "雪松"),
            priority = 80,
            characterIds = listOf("noa"),
            locationIds = listOf("place_moonlight"),
            activationMode = LoreActivationMode.LOCATION,
            category = "地点",
            notes = "博学与夜读中心"
        ),
        LoreEntry(
            id = "lore_white_florist",
            title = "巷尾白色花店",
            content = "一家只在早晨与雨天特别亮起暖黄色吊灯的花店。老板每天清晨从城郊运来带露水的新鲜花卉。小弥常买这里的白色洋桔梗，寓意不张扬却恒久的守护。",
            keywords = listOf("白色花店", "花店", "白洋桔梗", "洋桔梗", "鲜花"),
            secondaryKeywords = listOf("小弥", "白瓷瓶"),
            priority = 70,
            characterIds = listOf("mira"),
            locationIds = listOf("place_flower_shop"),
            activationMode = LoreActivationMode.KEYWORD,
            category = "地点",
            notes = "日常偶遇与礼物寄托"
        ),
        LoreEntry(
            id = "lore_rainy_tea_circle",
            title = "雨夜茶会",
            content = "每逢连绵细雨之夜，小弥、悠奈、诺亚与用户自发连通的心网秘密群组。大家会卸下白天的拘束，就着雨声闲聊甜品、唱片、诗歌与日常琐事。",
            keywords = listOf("雨夜茶会", "茶会", "群聊", "心网", "雨夜"),
            secondaryKeywords = listOf("小弥", "悠奈", "诺亚"),
            priority = 85,
            characterIds = listOf("mira", "yuna", "noa"),
            locationIds = emptyList(),
            activationMode = LoreActivationMode.EVENT,
            category = "事件",
            notes = "多角色跨羁绊交互核心载体"
        ),
        LoreEntry(
            id = "lore_black_tea_earlgrey",
            title = "伯爵红茶的冲泡习惯",
            content = "小弥的专属生活习惯：必须用88度温水沿杯壁缓缓注入，闷泡三分钟后取出茶包，加入一勺金桂蜂蜜。即使你未归，她也会在保温垫上一直为你温着。",
            keywords = listOf("伯爵红茶", "红茶", "温水", "泡茶"),
            secondaryKeywords = listOf("小弥", "保温垫"),
            priority = 65,
            characterIds = listOf("mira"),
            locationIds = listOf("place_street_23"),
            activationMode = LoreActivationMode.CHARACTER,
            category = "习惯",
            notes = "细微关怀习惯"
        ),
        LoreEntry(
            id = "lore_silver_wrapper",
            title = "旧书页里的薄荷糖纸",
            content = "夹在小弥书架《草枕》第52页的一枚银色锡纸，记录着某个初秋雨天初次为你撑伞时的心跳回忆，被小弥视为不可对外宣泄的私密心印。",
            keywords = listOf("薄荷糖纸", "糖纸", "草枕", "撑伞"),
            secondaryKeywords = listOf("小弥", "秘密"),
            priority = 60,
            characterIds = listOf("mira"),
            locationIds = listOf("place_street_23"),
            activationMode = LoreActivationMode.KEYWORD,
            category = "秘密",
            notes = "深度羁绊道具"
        ),
        LoreEntry(
            id = "lore_familymart_pudding",
            title = "街角便利店的焦糖布丁",
            content = "街角24小时全家便利店每周三与周五晚限定上架的手作焦糖布丁。悠奈曾冒着大雨冲进店里抢到最后一盒，并专程留给茶会同伴品尝。",
            keywords = listOf("便利店", "焦糖布丁", "布丁", "甜品"),
            secondaryKeywords = listOf("悠奈", "全家"),
            priority = 60,
            characterIds = listOf("yuna"),
            locationIds = listOf("place_convenience"),
            activationMode = LoreActivationMode.EVENT,
            category = "共同记忆",
            notes = "活泼日常羁绊"
        )
    )

    val defaultWorldBook = WorldBook(
        id = "wb_ailua_core",
        name = "青石街与心网物语 · Core Lorebook",
        description = "AILUA 伴生世界的地理、人文风貌、角色隐秘习惯与生活网络全集",
        scanDepth = 3,
        tokenBudget = 800,
        recursiveScanning = true,
        entries = sampleLoreEntries
    )

    // =========================================================================
    // 3. VIRTUAL PLACES (虚拟世界地图点位)
    // =========================================================================

    val virtualPlaces = listOf(
        VirtualPlace(
            id = "place_street_23",
            name = "青石街23号",
            description = "爬满常春藤的两层阁楼，二层向外突出的白色木框飘窗总是透出暖黄光晕，红茶袅袅。",
            type = "居所",
            mood = "宁静温暖",
            residentCharacterIds = listOf("mira"),
            connectedPlaceIds = listOf("place_mulan", "place_flower_shop", "place_rainy_lane"),
            currentCharacterIds = listOf("mira"),
            recentEvents = listOf("小弥在飘窗前泡好伯爵红茶", "风吹乱了桌上的书页"),
            visualReference = "place_street_23",
            ambientAudioNote = "秋雨沙沙声与窗边风铃轻响",
            coordinateX = 0.32f,
            coordinateY = 0.28f
        ),
        VirtualPlace(
            id = "place_mulan",
            name = "木兰茶馆",
            description = "巷口的老茶馆，木质回廊与庭院老松，是大家午后围坐吃点心聊天的最佳去处。",
            type = "茶歇",
            mood = "悠闲温存",
            residentCharacterIds = emptyList(),
            connectedPlaceIds = listOf("place_street_23", "place_convenience", "place_moonlight"),
            currentCharacterIds = emptyList(),
            recentEvents = listOf("悠奈和小弥中午在这里吃了柠檬司康", "茶馆老板换上了茉莉花香炉"),
            visualReference = "place_mulan",
            ambientAudioNote = "茶具轻磕声与庭院水车转动",
            coordinateX = 0.65f,
            coordinateY = 0.35f
        ),
        VirtualPlace(
            id = "place_moonlight",
            name = "月光书阁",
            description = "隐匿于老旧红砖楼下的半地下藏书室，藏有数万册旧书与老唱片，空气中飘着干燥纸张与雪松味。",
            type = "书阁",
            mood = "沉静知性",
            residentCharacterIds = listOf("noa"),
            connectedPlaceIds = listOf("place_mulan", "place_rainy_lane"),
            currentCharacterIds = listOf("noa"),
            recentEvents = listOf("诺亚正在整理新收录的古典爵士唱片", "点亮了负一层阅读桌的黄铜台灯"),
            visualReference = "place_moonlight",
            ambientAudioNote = "黑胶留声机微杂音与翻书沙沙声",
            coordinateX = 0.78f,
            coordinateY = 0.68f
        ),
        VirtualPlace(
            id = "place_flower_shop",
            name = "巷尾白色花店",
            description = "窄巷拐角处的清新花店，纯白色的木制展架上摆满带着清晨露水的新鲜花束与绿植。",
            type = "花卉",
            mood = "清新心动",
            residentCharacterIds = emptyList(),
            connectedPlaceIds = listOf("place_street_23", "place_rainy_lane"),
            currentCharacterIds = emptyList(),
            recentEvents = listOf("刚运来一批白色洋桔梗", "小弥曾在这里驻足拍照"),
            visualReference = "place_flower_shop",
            ambientAudioNote = "微风吹拂绿叶与剪枝轻响",
            coordinateX = 0.18f,
            coordinateY = 0.58f
        ),
        VirtualPlace(
            id = "place_convenience",
            name = "街角全家便利店",
            description = "24小时亮着白光的街角驿站，冷柜里琳琅满目的手作布丁与热腾腾的关东煮蒸汽。",
            type = "街区",
            mood = "元气日常",
            residentCharacterIds = emptyList(),
            connectedPlaceIds = listOf("place_mulan", "place_rainy_lane"),
            currentCharacterIds = listOf("yuna"),
            recentEvents = listOf("悠奈冒雨抢到了最后一盒限定焦糖布丁", "店员播放着欢快的入店提示音"),
            visualReference = "place_convenience",
            ambientAudioNote = "便利店门铃叮咚与冷柜轻鸣",
            coordinateX = 0.85f,
            coordinateY = 0.22f
        ),
        VirtualPlace(
            id = "place_rainy_lane",
            name = "雨夜青石小巷",
            description = "贯穿整个街区的主要青石板步道，雨天倒映着昏黄路灯的碎影，适合撑伞静静漫步。",
            type = "漫步",
            mood = "浪漫诗意",
            residentCharacterIds = emptyList(),
            connectedPlaceIds = listOf("place_street_23", "place_flower_shop", "place_moonlight", "place_convenience"),
            currentCharacterIds = emptyList(),
            recentEvents = listOf("路灯下水洼泛起涟漪", "远处隐约传来风铃与雨水声"),
            visualReference = "place_rainy_lane",
            ambientAudioNote = "细雨敲打青石板路与伞面回声",
            coordinateX = 0.45f,
            coordinateY = 0.52f
        )
    )

    // =========================================================================
    // 4. THEATER INTERACTIVE NARRATIVE (沉浸剧场与分支物语)
    // =========================================================================

    val rainyNightStory = TheaterStory(
        id = "story_rainy_tea",
        title = "雨夜茶话 · 避雨之客",
        subtitle = "青石街23号的秋雨与一杯热伯爵",
        description = "暮色降临，秋雨突如其来。你被困在青石街尽头，小弥推开飘窗看见了湿漉漉的你。你的选择将影响今晚的温度与彼此的情感羁绊。",
        characterIds = listOf("mira", "yuna"),
        initialNodeId = "node_1",
        coverTag = "秋雨暖阁",
        initialVariables = mapOf(
            "closeness" to "50",
            "hasUmbrella" to "false",
            "teaShared" to "false"
        ),
        nodes = mapOf(
            "node_1" to TheaterDialogueNode(
                id = "node_1",
                speakerId = "mira",
                speakerName = "小弥",
                avatarId = "mira",
                text = "（推开飘窗，看着檐下被雨打湿衣角的你，眼睛微微睁大，急忙挥手）快先进来！秋天的雨这么凉，怎么没带伞呢？",
                choices = listOf(
                    TheaterChoice(
                        id = "c1_a",
                        text = "快步跑进楼道，抖落伞上的雨水：「突然下的雨，幸好你在。」",
                        targetNodeId = "node_2_enter",
                        setVariableKey = "closeness",
                        setVariableValue = "60",
                        bondIncrease = 2
                    ),
                    TheaterChoice(
                        id = "c1_b",
                        text = "站在窗下有些不好意思：「会不会打扰你休息了？」",
                        targetNodeId = "node_2_shy",
                        setVariableKey = "closeness",
                        setVariableValue = "55",
                        bondIncrease = 1
                    )
                )
            ),
            "node_2_enter" to TheaterDialogueNode(
                id = "node_2_enter",
                speakerId = "mira",
                speakerName = "小弥",
                avatarId = "mira",
                text = "笨蛋，说什么幸好呀……（拿出一块雪白干燥的绒布毛巾递给你）快把头发擦擦。红茶刚好泡好三分钟，放了半勺金桂蜂蜜，快捧着暖暖手。",
                choices = listOf(
                    TheaterChoice(
                        id = "c2_tea",
                        text = "双手接过温热的陶瓷茶杯，轻轻抿了一口：「温度刚刚好，真甜。」",
                        targetNodeId = "node_3_warmth",
                        setVariableKey = "teaShared",
                        setVariableValue = "true",
                        bondIncrease = 3
                    ),
                    TheaterChoice(
                        id = "c2_look",
                        text = "打量着飘窗边的白瓷瓶白洋桔梗：「这就是你下午买的花吗？很漂亮。」",
                        targetNodeId = "node_3_flowers",
                        setVariableKey = "teaShared",
                        setVariableValue = "false",
                        bondIncrease = 2
                    )
                )
            ),
            "node_2_shy" to TheaterDialogueNode(
                id = "node_2_shy",
                speakerId = "mira",
                speakerName = "小弥",
                avatarId = "mira",
                text = "怎么会是打扰呢！（拉开房门，眸子里透着一丝心疼的责备）我一直留着窗边最软的那个位置，就是想着万一你路过……快进来坐下。",
                choices = listOf(
                    TheaterChoice(
                        id = "c2_sit",
                        text = "脱下湿外套，在飘窗软垫上坐下：「有你守在这里，心里真踏实。」",
                        targetNodeId = "node_3_warmth",
                        setVariableKey = "closeness",
                        setVariableValue = "65",
                        bondIncrease = 3
                    )
                )
            ),
            "node_3_warmth" to TheaterDialogueNode(
                id = "node_3_warmth",
                speakerId = "mira",
                speakerName = "小弥",
                avatarId = "mira",
                text = "（把自己的羊毛毯分出半角搭在你的膝头，侧头看着窗外连成细线的雨珠）雨下得这么大，今晚就在这里听雨吧。不想说话也没关系，我就在这里陪着你。",
                choices = listOf(
                    TheaterChoice(
                        id = "c3_stay",
                        text = "静静靠在窗边，和她并肩倾听夜雨与风铃声",
                        targetNodeId = "node_ending_perfect",
                        bondIncrease = 5
                    )
                )
            ),
            "node_3_flowers" to TheaterDialogueNode(
                id = "node_3_flowers",
                speakerId = "mira",
                speakerName = "小弥",
                avatarId = "mira",
                text = "嗯！路过花店第一眼看到就觉得很配你。花店阿姨还说，洋桔梗在雨天能开得更久呢。（悄悄红了脸颊）你要是喜欢，走的时候带两支插在桌前？",
                choices = listOf(
                    TheaterChoice(
                        id = "c3_promise",
                        text = "「好啊，那我们说好了，下次雨天我还要来喝茶。」",
                        targetNodeId = "node_ending_promise",
                        bondIncrease = 4
                    )
                )
            ),
            "node_ending_perfect" to TheaterDialogueNode(
                id = "node_ending_perfect",
                speakerId = "mira",
                speakerName = "小弥",
                avatarId = "mira",
                text = "✨【结局 · 雨夜永驻的暖意】\n茶香袅袅，夜雨微凉。在青石街23号的小阁楼里，你们度过了一个不被世俗打扰的温情之夜。羁绊等级已提升！",
                isEnding = true,
                endingTitle = "雨夜永驻的暖意 (羁绊真言)"
            ),
            "node_ending_promise" to TheaterDialogueNode(
                id = "node_ending_promise",
                speakerId = "mira",
                speakerName = "小弥",
                avatarId = "mira",
                text = "✨【结局 · 桔梗与风的约定】\n小弥把开得最饱满的一支白洋桔梗包好递到你手中。雨声渐歇，但关于下一个雨天的约定已悄然生根。",
                isEnding = true,
                endingTitle = "桔梗与风的约定 (未来之约)"
            )
        )
    )

    // =========================================================================
    // 5. LOCAL HELPER: getActiveLore (纯本地词条激活匹配引擎)
    // =========================================================================

    /**
     * Pure local helper to scan and retrieve active lore entries based on context.
     * Evaluates keywords, location IDs, character associations, and life events.
     * Prepares future prompt assembly without external LLM calls.
     */
    fun getActiveLore(
        characterId: String? = null,
        locationId: String? = null,
        recentText: String = "",
        lifeEventTitle: String? = null,
        worldBook: WorldBook = defaultWorldBook
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

    data class LoreActivationResult(
        val entry: LoreEntry,
        val activationReasons: List<String>,
        val effectivePriority: Int
    )

    /**
     * Helper to map CharacterCard to CharacterProfile.
     */
    fun cardToProfile(card: CharacterCard): CharacterProfile {
        val d = card.data
        return CharacterProfile(
            id = if (d.id.isNotBlank()) d.id else "custom_${System.currentTimeMillis()}",
            name = d.name.substringBefore(" (").ifBlank { d.name },
            englishName = if (d.name.contains("(") && d.name.contains(")")) {
                d.name.substringAfter("(").substringBefore(")")
            } else d.name,
            title = d.tags.firstOrNull() ?: "心网伴生者",
            bio = d.description,
            currentActivity = d.scenario.take(30),
            mood = "温存 · 共鸣",
            location = "青石街23号",
            contextualQuote = d.firstMessage.take(40),
            bondLevel = 1,
            bondName = "初识 · 伴生共鸣",
            bondProgress = 20,
            daysTogether = 1,
            energyLevel = 100,
            personalityTags = d.tags,
            memories = emptyList(),
            timeline = emptyList(),
            avatarId = d.avatarReference.ifBlank { "mira" },
            isOnline = true,
            relationshipType = "伴生者"
        )
    }
}
