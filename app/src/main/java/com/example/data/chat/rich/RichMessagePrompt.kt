package com.example.data.chat.rich

import com.example.data.ai.model.AiMessage
import com.example.data.ai.model.AiRole
import com.example.data.chat.model.ChatTurnRole
import com.example.data.chat.model.ResolvedChatTurn
import com.example.data.chat.model.VariantStatus

object RichMessagePrompt {
    const val INSTRUCTIONS = "聊天首先以自然文字为主。富消息是低频行为，只在当前语境和角色生活习惯确实需要时使用；" +
        "不要连续发红包、转账、礼物或位置，不要用它们回避角色设定。虚拟金额须符合角色经济状况与场景，绝非真实支付。" +
        "可选格式：[红包:52:晚饭钱] [转账:18:今天辛苦了] [位置:青石街23号] [礼物:一束白色郁金香] [表情:偷看]。" +
        "单轮最多两张富消息卡，其中红包/转账合计最多一张；不要输出 JSON，也不要自行编造引用消息 ID。"

    fun history(turn: ResolvedChatTurn): AiMessage? {
        val variant = turn.activeVariant?.takeIf { it.status == VariantStatus.COMPLETE } ?: return null
        val role = if (turn.role == ChatTurnRole.USER) AiRole.USER else AiRole.ASSISTANT
        val content = if (variant.richPayloads.isEmpty()) variant.content else variant.richPayloads.joinToString(" ") { payload ->
            when (payload.type) {
                RichMessageType.TEXT -> payload.label.orEmpty().trim()
                RichMessageType.STICKER -> "[发送表情：${payload.label}]"
                RichMessageType.LOCATION -> "[分享虚拟位置：${payload.locationName}]"
                RichMessageType.GIFT -> "[送出礼物：${payload.label}${payload.userActionForPrompt()}]"
                RichMessageType.RED_PACKET -> "[发送虚拟红包 ${payload.currency}${payload.amount}：${payload.label.orEmpty()}${payload.userActionForPrompt()}]"
                RichMessageType.TRANSFER -> "[发起虚拟转账 ${payload.currency}${payload.amount}：${payload.label.orEmpty()}${payload.userActionForPrompt()}]"
                RichMessageType.QUOTE -> payload.label.orEmpty()
            }
        }.trim()
        val withQuote = if (turn.role == ChatTurnRole.USER && variant.quoteMessageId != null && !variant.quotePreview.isNullOrBlank()) {
            "用户回复了你之前的消息：\"${variant.quotePreview}\"\n用户说：\"$content\""
        } else content
        return AiMessage(role, withQuote)
    }

    private fun RichMessagePayload.userActionForPrompt(): String = when {
        type == RichMessageType.RED_PACKET && status == RichMessageStatus.OPENED -> "；用户已打开"
        type == RichMessageType.GIFT && status == RichMessageStatus.RECEIVED -> "；用户已收下"
        type == RichMessageType.TRANSFER && status == RichMessageStatus.ACCEPTED -> "；用户已收下"
        type == RichMessageType.TRANSFER && status == RichMessageStatus.DECLINED -> "；用户已退回"
        status == RichMessageStatus.CANCELED -> "；已取消"
        else -> ""
    }
}
