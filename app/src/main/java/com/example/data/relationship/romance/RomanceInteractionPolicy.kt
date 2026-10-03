package com.example.data.relationship.romance

import com.example.data.character.runtime.CharacterRuntimeProfile
import java.security.MessageDigest
import java.util.Locale

/** Conservative recognition of explicit, completed two-way exchanges. No sentiment model or message-count rewards. */
object RomanceInteractionPolicy {
    private const val MIN_CONVERSATION_MS = 10 * 60_000L
    private const val SESSION_GAP_MS = 30 * 60_000L

    /** A persisted user boundary takes effect immediately, even if the provider later fails or is cancelled. */
    fun observeUserBoundary(record: RomanceRecord, profile: CharacterRuntimeProfile, userTurnId: String, userText: String, occurredAtEpochMs: Long): RomanceRecord {
        if (userTurnId.isBlank() || !explicitBoundary(userText)) return record
        return RomanceReducer.apply(record, RomanceEvent(
            "exchange:$userTurnId:${RomanceEventType.BOUNDARY_RESET.name}", RomanceEventType.BOUNDARY_RESET,
            occurredAtEpochMs, "${RomanceEventType.BOUNDARY_RESET.name}:${fingerprint(userText)}",
        ), profile)
    }

    fun observeExchange(
        record: RomanceRecord,
        profile: CharacterRuntimeProfile,
        userTurnId: String,
        userText: String,
        assistantText: String,
        occurredAtEpochMs: Long,
    ): RomanceRecord {
        if (userTurnId.isBlank() || userTurnId in record.processedExchangeIds || occurredAtEpochMs <= 0 ||
            userText.isBlank() || assistantText.isBlank()) return record
        val user = normalize(userText)
        val assistant = normalize(assistantText)
        var result = record.copy(processedExchangeIds = record.processedExchangeIds + userTurnId)
        // A completed greeting is real chat, but is insufficient to provoke jealousy or deepen a relationship.
        val substantial = user.length >= 12 && assistant.length >= 6 && !isRepeatedGreeting(user)
        if (substantial) {
            result = result.copy(lastUserInteractionAtEpochMs = maxOf(occurredAtEpochMs, result.lastUserInteractionAtEpochMs ?: 0L))
            val previous = result.conversation
            val reset = previous.lastAtEpochMs == 0L || occurredAtEpochMs - previous.lastAtEpochMs > SESSION_GAP_MS
            val session = if (reset) ConversationEvidence(startedAtEpochMs = occurredAtEpochMs) else previous
            val fingerprint = fingerprint(user)
            val evidence = session.copy(
                lastAtEpochMs = maxOf(session.lastAtEpochMs, occurredAtEpochMs),
                uniqueExchangeFingerprints = if (session.rewarded) session.uniqueExchangeFingerprints else session.uniqueExchangeFingerprints + fingerprint,
            )
            result = result.copy(conversation = evidence)
            if (!evidence.rewarded && evidence.uniqueExchangeFingerprints.size >= 3 &&
                occurredAtEpochMs - evidence.startedAtEpochMs >= MIN_CONVERSATION_MS) {
                result = RomanceReducer.apply(result, RomanceEvent(
                    "conversation:${evidence.startedAtEpochMs}", RomanceEventType.SUSTAINED_CONVERSATION,
                    occurredAtEpochMs, "conversation:${fingerprint(evidence.uniqueExchangeFingerprints.sorted().joinToString())}",
                ), profile).copy(conversation = evidence.copy(rewarded = true))
            }
        }
        val semantic = semanticEvent(result, userText, assistantText, occurredAtEpochMs)
        if (semantic != null) result = RomanceReducer.apply(result, RomanceEvent(
            "exchange:$userTurnId:${semantic.name}", semantic, occurredAtEpochMs,
            when (semantic) {
                RomanceEventType.CONFLICT -> "conflict:${fingerprint(user)}:${result.recentConflict?.resolvedAtEpochMs}"
                RomanceEventType.CONFLICT_RESOLVED -> "resolved:${result.recentConflict?.eventId}"
                RomanceEventType.ANNIVERSARY -> "anniversary:${occurredAtEpochMs / RomanceReducer.DAY_MS}"
                else -> "${semantic.name}:${fingerprint(user)}"
            },
        ), profile)
        return result
    }

    internal fun semanticEvent(record: RomanceRecord, userText: String, assistantText: String, now: Long): RomanceEventType? {
        // User boundaries need no character permission. Quoted, hypothetical and negated phrases are excluded.
        if (explicitBoundary(userText)) return RomanceEventType.BOUNDARY_RESET
        if (!literal(userText) || !literal(assistantText)) return null
        if ((direct(userText, conflict) || explicitConflict(userText)) &&
            (direct(assistantText, conflictAcknowledgement) || acknowledgesConflict(assistantText))) return RomanceEventType.CONFLICT
        if (record.recentConflict?.resolvedAtEpochMs == null && record.recentConflict != null &&
            direct(userText, reconciliation) && direct(assistantText, reconciliationAcknowledgement)) return RomanceEventType.CONFLICT_RESOLVED
        if (direct(userText, affection) && direct(assistantText, affectionAcknowledgement)) return RomanceEventType.MUTUAL_AFFECTION
        if (record.romanticConfirmedAtEpochMs != null && direct(userText, commitment) && direct(assistantText, commitmentAcknowledgement)) return RomanceEventType.MUTUAL_COMMITMENT
        if (record.romanticConfirmedAtEpochMs != null && direct(userText, anniversary) && direct(assistantText, anniversaryAcknowledgement)) {
            val elapsedDays = (now - record.romanticConfirmedAtEpochMs) / RomanceReducer.DAY_MS
            if (elapsedDays >= 28 && (elapsedDays % 30 in 0..1 || elapsedDays % 365 in 0..1)) return RomanceEventType.ANNIVERSARY
        }
        if ((direct(userText, goodNews) || explicitGoodNews(userText)) && direct(assistantText, goodNewsAcknowledgement)) return RomanceEventType.GOOD_EVENT
        return null
    }

    /** Exact declarative sentence starts; no "she said", fictional dialogue, uncertainty, or questions. */
    private fun direct(text: String, phrases: List<String>): Boolean {
        if (!literal(text)) return false
        return text.split(Regex("[。！!\n]"))
            .map { normalize(it).trim(',', '，', '.', '。') }
            .any { sentence -> phrases.any { sentence == normalize(it) || sentence.startsWith(normalize(it) + "，") || sentence.startsWith(normalize(it) + ",") } }
    }

    private fun literal(text: String): Boolean {
        val value = text.lowercase(Locale.ROOT)
        if (value.any { it in "\"“”‘’「」『』《》?？" } || Regex("(^|\\s)'[^']+'($|\\s|[.!])").containsMatchIn(value)) return false
        return ambiguous.none { value.contains(it) }
    }

    /** Negative wishes ARE boundaries. Their own negation must not be mistaken for hypothetical rejection. */
    private fun explicitBoundary(text: String): Boolean {
        val value = text.lowercase(Locale.ROOT).trim().replace(Regex("[，,]?(?:好吗|可以吗)[?？]$"), "。")
        if (value.any { it in "\"“”‘’「」『』《》?？" }) return false
        if (Regex("(^|\\s)'[^']+'($|\\s|[.!])").containsMatchIn(value)) return false
        if (listOf("如果", "假如", "假设", "比如", "举例", "他说", "她说", "台词", "小说", "角色扮演", "测试", "if ", "imagine", "pretend", "quote", "example").any { it in value }) return false
        val prefix = "(?:(?:其实|认真说|对不起|抱歉|关于我们|我想说)[，,:：]*)?"
        val statement = "(?:我(?:真的|现在|目前|暂时)?(?:并)?不(?:想|愿意|打算)和你(?:谈恋爱|恋爱|交往|在一起)|我(?:现在|目前)?只想和你做朋友|我们(?:还是|以后|就)?只做朋友(?:吧)?|我们分手吧|我想(?:结束|停止)这段(?:恋爱)?关系|请(?:你)?不要(?:再)?追求我|别再追求我)"
        if (Regex("^$prefix$statement(?:了)?(?:[，,。！!.]|$)").containsMatchIn(value)) return true
        return Regex("^(?:let's|let us) (?:just )?stay friends(?:[,.!]|$)|^i (?:do not|don't) want (?:to date you|a romantic relationship with you)(?:[,.!]|$)|^(?:please )?stop pursuing me(?:[,.!]|$)")
            .containsMatchIn(value)
    }

    private fun explicitConflict(text: String): Boolean = literal(text) &&
        Regex("^(?:我(?:真的|现在)?(?:很生气|有点生气|很难过|被你伤到了)|你刚才(?:说的)?(?:话|那句话)让我(?:难过|生气|受伤)|我需要(?:一点|一些)?时间(?:冷静|整理情绪))(?:[，,。！!]|$)")
            .containsMatchIn(text.trim())

    private fun acknowledgesConflict(text: String): Boolean = literal(text) &&
        Regex("^(?:我(?:知道|明白|理解)(?:你(?:现在)?(?:很生气|很难过|需要时间)|这(?:件事)?(?:伤到你|让你难过)了)|刚才是我(?:说重了|太着急了)|我们(?:先|都)?冷静(?:一下)?)(?:[，,。！!]|$)")
            .containsMatchIn(text.trim())

    private fun explicitGoodNews(text: String): Boolean = literal(text) &&
        Regex("^我(?:今天|刚刚|终于)(?:通过了?[^，,。！!?？]{1,20}考试|拿到了?[^，,。！!?？]{0,16}录取通知(?:书)?|完成了[^，,。！!?？]{1,24}项目|升职了|毕业了)(?:[，,。！!]|$)")
            .containsMatchIn(text.trim())

    private fun normalize(value: String) = value.lowercase(Locale.ROOT).replace(Regex("\\s+"), "").trim()
    private fun isRepeatedGreeting(value: String) = Regex("^(你好|早安|晚安|在吗|嗨|哈喽|hello|hi|hey|goodmorning|goodnight|[!！。.,，])+$").matches(value)
    fun fingerprint(value: String): String = MessageDigest.getInstance("SHA-256").digest(normalize(value).toByteArray()).take(16).joinToString("") { "%02x".format(it) }

    private val ambiguous = listOf("如果", "假如", "假设", "比如", "举例", "他说", "她说", "台词", "小说", "角色扮演", "不要说", "不想", "不愿", "不是", "并不", "没有", "不能", "不可以", "别说", "并未", "未必", "可能", "也许", "测试", "开玩笑", "逗你", "当我没说", "但是", "不过", "if ", "imagine", "pretend", "quote", "example", "not ", "never ", "don't", "can't", "but ", "maybe", "perhaps")
    private val conflict = listOf("我被你刚才的话伤到了", "这件事让我很生气", "我需要一点时间冷静", "我们刚才的争执让我难过", "i need time to cool down")
    private val conflictAcknowledgement = listOf("我知道这件事伤到了你", "我们先冷静一下", "我明白你需要时间", "刚才的话是我说重了", "i understand you need time")
    private val reconciliation = listOf("这件事我们说开了", "误会解开了", "我原谅你了", "我们和好了", "we have cleared this up")
    private val reconciliationAcknowledgement = listOf("我们说开了", "谢谢你愿意说开", "我会记住这次的教训", "我们和好了", "we have cleared this up")
    private val affection = listOf("我想和你正式交往", "我们正式交往吧", "我愿意和你在一起", "i want to date you", "let us date")
    private val affectionAcknowledgement = listOf("我也愿意和你在一起", "那我们正式交往", "我愿意和你在一起", "i want to date you too", "yes let us date")
    private val commitment = listOf("我想和你认真走下去", "我们认真约定共同的未来", "i want to build a future with you")
    private val commitmentAcknowledgement = listOf("我也想和你认真走下去", "我们一起认真走下去", "我愿意和你共同规划未来", "i want to build a future with you too")
    private val anniversary = listOf("今天是我们的交往纪念日", "今天是我们交往一个月", "今天是我们交往一周年", "today is our anniversary")
    private val anniversaryAcknowledgement = listOf("我记得这个日子", "纪念日快乐", "happy anniversary")
    private val goodNews = listOf("我今天通过考试了", "我今天拿到录取通知了", "我今天拿到offer了", "我完成了那个重要项目", "i passed my exam today")
    private val goodNewsAcknowledgement = listOf("恭喜你", "这个结果值得庆祝", "我就知道你的努力会有结果", "congratulations")
}
