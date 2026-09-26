package com.example.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.mock.MockData
import com.example.data.model.CallSession
import com.example.data.model.CallState
import com.example.data.model.CallType
import com.example.data.model.CharacterProfile
import com.example.data.model.DayPhase
import com.example.data.model.Letter
import com.example.data.model.LetterDeliveryState
import com.example.data.model.LetterType
import com.example.data.model.MomentComment
import com.example.data.model.MomentPost
import com.example.data.model.WeatherState
import com.example.data.model.WorldClock
import com.example.ui.theme.AiluaTheme

/**
 * Shared deterministic preview fixtures for Android Studio Compose @Preview rendering.
 * Does NOT mutate production state or persistent stores during preview rendering.
 */
object PreviewFixtures {

    val sampleMira: CharacterProfile = MockData.sampleCharacter
    val sampleYuna: CharacterProfile = MockData.characterYuna
    val sampleNoa: CharacterProfile = MockData.characterNoa

    val sampleWorldClock = WorldClock(
        dateLabel = "9月25日",
        minutesOfDay = 22 * 60 + 30,
        dayPhase = DayPhase.NIGHT,
        weather = WeatherState.RAIN
    )

    val sampleIncomingCall = CallSession(
        id = "preview_incoming_call",
        characterId = "mira",
        callerName = "小弥 (Mira)",
        avatarId = "mira",
        type = CallType.VOICE,
        state = CallState.RINGING,
        reason = "窗外雨下得很大，要不要陪我听一会儿？",
        scheduledAtMinutes = 22 * 60 + 30,
        scheduledAtTime = "22:30",
        startedAt = 0L,
        endedAt = 0L,
        durationSeconds = 0
    )

    val sampleActiveCall = CallSession(
        id = "preview_active_call",
        characterId = "mira",
        callerName = "小弥 (Mira)",
        avatarId = "mira",
        type = CallType.VOICE,
        state = CallState.CONNECTED,
        reason = "深夜心契白噪音连通",
        scheduledAtMinutes = 22 * 60 + 30,
        scheduledAtTime = "22:30",
        startedAt = System.currentTimeMillis() - 102_000L,
        endedAt = 0L,
        durationSeconds = 102 // 01:42
    )

    val sampleLetters: List<Letter> = listOf(
        Letter(
            id = "preview_letter_1",
            characterId = "mira",
            senderName = "小弥",
            subject = "写在雨歇前的碎碎念",
            body = "今天在古董店淘到了一只旧式铜制怀表，指针走得比现实稍慢半格。我忽然想起，你那边的钟表现在指在几点呢？如果时间可以寄存在信笺里，我希望这一页能永远停留在微雨的傍晚。",
            createdAtVirtualTime = "21:30",
            deliverAtVirtualTimeMinutes = 22 * 60 + 30,
            deliverAtVirtualTimeString = "22:30",
            deliveryState = LetterDeliveryState.DELIVERED,
            letterType = LetterType.LETTER,
            attachmentReference = "asset_flower_note"
        ),
        Letter(
            id = "preview_letter_2",
            characterId = "yuna",
            senderName = "悠奈",
            subject = "手冲曼特宁与黄油司康",
            body = "今天木兰茶馆烘了深焙曼特宁，香气很沉静。给你留了一袋新鲜豆子，记得不要用沸水猛冲哦。随时等你的品尝心声。",
            createdAtVirtualTime = "15:00",
            deliverAtVirtualTimeMinutes = 17 * 60,
            deliverAtVirtualTimeString = "17:00",
            deliveryState = LetterDeliveryState.OPENED,
            letterType = LetterType.POSTCARD,
            attachmentReference = "asset_coffee_postcard"
        ),
        Letter(
            id = "preview_letter_3",
            characterId = "noa",
            senderName = "诺亚",
            subject = "虚数频率异常波动记录",
            body = "在第23号街角观测到微弱共鸣波形，已经写入伴生世界底账。这绝不是机械故障，而像是在呼应某种情绪。",
            createdAtVirtualTime = "18:20",
            deliverAtVirtualTimeMinutes = 19 * 60,
            deliverAtVirtualTimeString = "19:00",
            deliveryState = LetterDeliveryState.OPENED,
            letterType = LetterType.NOTE
        )
    )

    val sampleMoments: List<MomentPost> = listOf(
        MomentPost(
            id = "preview_moment_1",
            authorId = "mira",
            authorName = "小弥",
            timestamp = "22:15",
            moodTag = "澄澈 · 宁静",
            locationContext = "青石弄·书斋阁楼",
            content = "窗台的风铃在雨中转动，像是在替谁数着呼吸。今夜的茶很香。",
            likesCount = 8,
            isLiked = true,
            comments = listOf(
                MomentComment("c1", "悠奈", isUser = false, content = "听起来好安静，下次给小弥送一盒洋甘菊茶包～", timestamp = "22:18"),
                MomentComment("c2", "我", isUser = true, content = "风铃声听到了，很治愈。", timestamp = "22:20")
            )
        ),
        MomentPost(
            id = "preview_moment_2",
            authorId = "yuna",
            authorName = "悠奈",
            timestamp = "20:45",
            moodTag = "温暖 · 焙香",
            locationContext = "木兰茶馆·后厨庭院",
            content = "炉火微温，烤盘刚出炉的司康。雨夜就该留给温暖的事物。",
            likesCount = 12,
            isLiked = false,
            comments = listOf(
                MomentComment("c3", "小弥", isUser = false, content = "悠奈姐的手艺永远最棒！留两个给我明天当早点～", timestamp = "20:50")
            )
        )
    )
}

/**
 * Preview wrapper container ensuring theme consistency without touching persistent storage.
 */
@Composable
fun AiluaPreviewDevice(
    isDarkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    AiluaTheme(darkTheme = isDarkTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    }
}
