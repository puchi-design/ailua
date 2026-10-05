package com.example

import com.example.data.chat.rich.RichMessageParser
import com.example.data.chat.rich.RichMessageStatus
import com.example.data.chat.rich.RichMessageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RichMessageParserTest {
    @Test fun parsesCardsAndPreservesTextOrder() {
        val parsed = RichMessageParser.parse("下班了。[位置:青石街23号] 我带了[礼物:白色郁金香]")
        assertEquals("下班了。 我带了", parsed.content)
        assertEquals(listOf(RichMessageType.TEXT, RichMessageType.LOCATION, RichMessageType.TEXT, RichMessageType.GIFT),
            parsed.payloads.map { it.type })
        assertEquals("青石街23号", parsed.payloads[1].locationName)
        assertEquals(RichMessageStatus.PENDING, parsed.payloads[3].status)
    }

    @Test fun parsesVirtualMoneyAndLimitsFinancialCards() {
        val parsed = RichMessageParser.parse("[红包:52:晚饭钱][转账:18:路费][表情:偷看]")
        assertEquals(listOf(RichMessageType.RED_PACKET, RichMessageType.TEXT, RichMessageType.STICKER),
            parsed.payloads.map { it.type })
        assertEquals(52.0, parsed.payloads[0].amount)
        assertEquals("¥", parsed.payloads[0].currency)
        assertEquals(RichMessageStatus.PENDING, parsed.payloads[0].status)
        assertEquals("[转账:18:路费]", parsed.content)
    }

    @Test fun transferParsesAndThirdRichCardFallsBackToText() {
        val parsed = RichMessageParser.parse("[转账:18.50:今天辛苦了][礼物:咖啡][位置:工作室]")
        assertEquals(listOf(RichMessageType.TRANSFER, RichMessageType.GIFT, RichMessageType.TEXT),
            parsed.payloads.map { it.type })
        assertEquals(18.5, parsed.payloads[0].amount)
        assertEquals("[位置:工作室]", parsed.content)
    }

    @Test fun malformedDirectivesFallBackToPlainTextWithoutCrashing() {
        val raw = "[转账:abc][红包:][位置:][表情:未知][普通方括号]"
        val parsed = RichMessageParser.parse(raw)
        assertEquals(raw, parsed.content)
        assertTrue(parsed.payloads.isEmpty())
        assertEquals("[红包:10000:过大][转账:-1:无效]", RichMessageParser.parse("[红包:10000:过大][转账:-1:无效]").content)
    }
}
