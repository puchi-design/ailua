package com.example

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.data.chat.local.ChatDatabase
import com.example.data.chat.rich.RichMessagePayload
import com.example.data.chat.rich.RichMessageCodec
import com.example.data.chat.rich.RichMessageStatus
import com.example.data.chat.rich.RichMessageType
import com.example.data.chat.model.VariantStatus
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RichMessagePersistenceTest {
    @Test fun cardsAndQuoteSurviveFreshRepositoryOverSameFile() {
        val file = File.createTempFile("ailua-rich-restart", ".db")
        try {
            val first = ChatTestHarness.file(file)
            val session = first.repository.getOrCreatePrivateSession("hewenchuan")
            val answer = first.repository.appendAssistantTurn(session.id, "", VariantStatus.STREAMING)
            first.repository.updateVariant(answer.activeVariantId!!, "", VariantStatus.COMPLETE,
                richPayloads = listOf(RichMessagePayload(RichMessageType.RED_PACKET, "晚饭钱", 52.0, "¥", RichMessageStatus.PENDING)))
            first.repository.updateRichStatus(answer.activeVariantId, 0, RichMessageStatus.OPENED)
            val reply = first.repository.appendUserTurn(session.id, "你自己还没吃吧", answer.id, "晚饭钱")
            first.driver.close()

            val second = ChatTestHarness.file(file, createSchema = false)
            val turns = second.repository.getResolvedTurns(session.id)
            assertEquals(RichMessageType.RED_PACKET, turns[0].activeVariant!!.richPayloads.single().type)
            assertEquals(RichMessageStatus.OPENED, turns[0].activeVariant!!.richPayloads.single().status)
            assertEquals(answer.id, turns[1].activeVariant!!.quoteMessageId)
            assertEquals("晚饭钱", turns[1].activeVariant!!.quotePreview)
            assertEquals(reply.id, turns[1].id)
            second.driver.close()
        } finally { file.delete() }
    }

    @Test fun migratingV8KeepsExistingRowsAndAddsNullableRichColumns() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            driver.execute(null, "CREATE TABLE chat_session (id TEXT PRIMARY KEY NOT NULL, character_id TEXT NOT NULL, created_at_epoch_ms INTEGER NOT NULL, updated_at_epoch_ms INTEGER NOT NULL)", 0)
            driver.execute(null, "CREATE TABLE chat_turn (id TEXT PRIMARY KEY NOT NULL, session_id TEXT NOT NULL, role TEXT NOT NULL, position INTEGER NOT NULL, active_variant_id TEXT, created_at_epoch_ms INTEGER NOT NULL)", 0)
            driver.execute(null, "CREATE TABLE chat_variant (id TEXT PRIMARY KEY NOT NULL, turn_id TEXT NOT NULL, variant_index INTEGER NOT NULL, content TEXT NOT NULL, status TEXT NOT NULL, provider_profile_id TEXT, model TEXT, error_type TEXT, error_message TEXT, created_at_epoch_ms INTEGER NOT NULL, updated_at_epoch_ms INTEGER NOT NULL)", 0)
            driver.execute(null, "INSERT INTO chat_session VALUES ('s','mira',1,1)", 0)
            driver.execute(null, "INSERT INTO chat_turn VALUES ('t','s','USER',0,'v',1)", 0)
            driver.execute(null, "INSERT INTO chat_variant VALUES ('v','t',0,'旧聊天','COMPLETE',NULL,NULL,NULL,NULL,1,1)", 0)
            ChatDatabase.Schema.migrate(driver, 8, 9)
            val row = ChatDatabase(driver).chatVariantQueries.selectVariantById("v").executeAsOneOrNull()
            assertNotNull(row)
            assertEquals("旧聊天", row!!.content)
            assertEquals(null, row.rich_payloads_json)
            assertEquals(null, row.quote_message_id)
        } finally { driver.close() }
    }

    @Test fun quoteCannotLinkAnotherCharactersSession() {
        val f = ChatTestHarness.inMemory()
        try {
            val one = f.repository.getOrCreatePrivateSession("mira")
            val two = f.repository.getOrCreatePrivateSession("yuna")
            val privateMessage = f.repository.appendAssistantTurn(one.id, "私密内容")
            f.repository.appendUserTurn(two.id, "引用测试", privateMessage.id, "伪造预览")
            val stored = f.repository.getResolvedTurns(two.id).single().activeVariant!!
            assertEquals(null, stored.quoteMessageId)
            assertEquals(null, stored.quotePreview)
        } finally { f.driver.close() }
    }

    @Test fun futureCardCannotEraseKnownCardsOrBeLostWhenKnownStatusChanges() {
        val f = ChatTestHarness.inMemory()
        try {
            val session = f.repository.getOrCreatePrivateSession("hewenchuan")
            val turn = f.repository.appendAssistantTurn(session.id, "", VariantStatus.COMPLETE)
            val variantId = checkNotNull(turn.activeVariantId)
            val original = """[{"type":"PHOTO","label":"随手拍","mediaUrl":"local://photo"},""" +
                """{"type":"RED_PACKET","label":"晚饭钱","amount":30.0,"status":"PENDING","futureFlag":"kept"}]"""
            f.database.chatVariantQueries.updateRichPayloads(original, f.clock.nowEpochMs(), variantId)

            val visible = f.repository.getResolvedTurns(session.id).single().activeVariant!!.richPayloads
            assertEquals(listOf(RichMessageType.TEXT, RichMessageType.RED_PACKET), visible.map { it.type })
            assertTrue(visible.first().label.orEmpty().contains("随手拍"))
            assertTrue(f.repository.updateRichStatus(variantId, 1, RichMessageStatus.OPENED))

            val saved = checkNotNull(f.database.chatVariantQueries.selectVariantById(variantId)
                .executeAsOneOrNull()?.rich_payloads_json)
            assertTrue(saved.contains("\"type\":\"PHOTO\""))
            assertTrue(saved.contains("\"mediaUrl\":\"local://photo\""))
            assertTrue(saved.contains("\"futureFlag\":\"kept\""))
            assertEquals(RichMessageStatus.OPENED,
                f.repository.getResolvedTurns(session.id).single().activeVariant!!.richPayloads[1].status)
            assertEquals("消息卡片暂不可用", RichMessageCodec.decode("{broken").single().label)
        } finally { f.driver.close() }
    }
}
