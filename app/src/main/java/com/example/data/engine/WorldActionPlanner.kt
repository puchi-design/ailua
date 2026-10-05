package com.example.data.engine

import com.example.data.ai.model.AiChatRequest
import com.example.data.ai.model.AiMessage
import com.example.data.ai.model.AiRole
import com.example.data.ai.model.AiStreamEvent
import com.example.data.ai.model.AiProviderError
import com.example.data.ai.runtime.ProviderResolver
import com.example.data.chat.model.VariantStatus
import com.example.data.chat.repository.ChatRepository
import com.example.data.memory.repository.MemoryRepository
import com.example.data.model.LifeEvent
import com.example.data.model.PlannedWorldAction
import com.example.data.model.WorldClock
import com.example.data.model.WorldPlan
import com.example.data.registry.CharacterRegistry
import com.example.data.character.CharacterBehaviorRuntime
import com.example.data.character.CharacterWorldPolicy
import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.projection.projectPresence
import com.example.data.relationship.repository.RelationshipStateRepository
import com.example.data.relationship.romance.RomanceRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/** User phone browsing is a private ledger fact, never an input to character-side AI. */
internal fun LifeEvent.isVisibleToCharacterAi(): Boolean =
    sourceAppId != "check_phone" && metadata["prompt_visibility"] != "hidden"

class WorldActionPlanner(
    private val providerResolver: ProviderResolver,
    private val memoryRepository: MemoryRepository? = null,
    private val chatRepository: ChatRepository? = null,
    private val realityContext: suspend () -> String? = { null },
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<PlannedWorldAction>? {
        return try {
            val body = json.parseToJsonElement(text).jsonObject
            val actions = body["actions"] ?: return null
            json.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(PlannedWorldAction.serializer()), actions)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun generate(clock: WorldClock, events: List<LifeEvent>, existing: List<PlannedWorldAction>): WorldPlan? {
        val resolved = providerResolver.resolve() ?: return null
        val characters = CharacterRegistry.getAllCharacters().take(8)
        val promptEvents = events.filter(LifeEvent::isVisibleToCharacterAi)
        val context = buildString {
            appendLine("世界时间：${clock.dateLabel} ${clock.timeFormatted}；天气：${clock.weather.label}。规划未来 6～12 小时、3～6 个事件。")
            appendLine("角色：")
            characters.forEach { character ->
                val characterContext = buildString {
                    val presence = projectPresence(character, promptEvents)
                    appendLine("${character.id} ${character.name}；${character.bio.take(160)}；当前位置 ${presence.currentLocation}；当前活动 ${presence.currentActivity}")
                    appendLine(CharacterBehaviorRuntime.worldGuidance(CharacterRuntimeResolver.resolve(character.id)).take(480))
                    RomanceRepository.promptInstructions(character.id).takeIf { it.isNotBlank() }?.let { appendLine(it.take(350)) }
                    val memories = memoryRepository?.getMemoriesForPrompt(character.id, 4).orEmpty()
                    memories.forEach { appendLine("记忆：${it.content.take(120)}") }
                    val turns = runCatching {
                        chatRepository?.getOrCreatePrivateSession(character.id)?.let { session ->
                            chatRepository.getResolvedTurns(session.id).takeLast(6)
                        }.orEmpty()
                    }.getOrDefault(emptyList())
                    turns.filter { it.activeVariant?.status == VariantStatus.COMPLETE }.takeLast(4).forEach { turn ->
                        appendLine("聊天 ${turn.role}: ${turn.activeVariant?.content.orEmpty().replace('\n', ' ').take(100)}")
                    }
                    RelationshipStateRepository.states.value.filter { it.fromCharacterId != "user" && it.toCharacterId != "user" && (it.fromCharacterId == character.id || it.toCharacterId == character.id) }
                        .take(2).forEach { appendLine("角色间关系：${it.fromCharacterId}-${it.toCharacterId} ${it.stage} 最近${it.recentInteraction.orEmpty().take(60)}") }
                }
                appendLine(characterContext.take(1000))
            }
            appendLine("近期事实：")
            promptEvents.take(12).forEach { appendLine("${it.worldDateLabel} ${it.time} ${it.characterId} ${it.type}: ${it.title.take(100)} ${it.description.take(100)}") }
            appendLine("近期聊天摘要：")
            promptEvents.filter { it.type.name == "MESSAGE" }.take(4).forEach { appendLine("${it.characterId}: ${it.description.take(120)}") }
            appendLine("已有未来计划：${existing.take(8).joinToString { "${it.triggerWorldDate} ${it.triggerMinutes} ${it.title}" }}")
            appendLine("今天已发生类型：${promptEvents.filter { it.worldDateLabel == clock.dateLabel }.groupingBy { it.type }.eachCount()}")
            appendLine("今天主动联系用户次数：${promptEvents.count { it.worldDateLabel == clock.dateLabel && it.type.name == "MESSAGE" && it.sourceAppId == "heartbeat" }}")
        }.take(10000) + realityContext()?.take(300).orEmpty()
        val request = AiChatRequest(
            model = resolved.model,
            messages = listOf(
                AiMessage(AiRole.SYSTEM, "你是在规划角色自己的生活，不是直接回复用户。输出严格 JSON 对象，唯一顶层字段 actions。每条 action 必须有 id、characterId、triggerWorldDate、triggerMinutes、type、lifeEventType、title、description、location、metadata、relatedCharacterIds。type 取 LIFE_EVENT、MOMENT、LOCATION_CHANGE、INCOMING_CALL；lifeEventType 取 WAKE_UP、MEAL、TRAVEL、THOUGHT、MEMORY、SOCIAL、PHOTO、MOMENT、MESSAGE、DIARY、SLEEP、SURPRISE、LOCATION_CHANGE。时间必须严格晚于当前时间，跨日使用次日日期。角色要有独立生活：吃饭、上课、工作、散步、拍照、听歌、写东西、发动态、写日记、睡觉、联系其他角色。最多 1～2 件直接联系用户的事。不要反复说想你、连续联系用户、无原因情绪大起大落、与既有事实冲突。metadata 可有 search_query、note、draft、hidden_thought 等查手机线索。公开照片和动态只写角色自己的生活，不披露用户私聊、私人记忆或关系状态；私人联系必须使用MESSAGE并在relatedCharacterIds包含user。不要生成 LifeEvent。"),
                AiMessage(AiRole.USER, context),
            ),
            stream = false,
            temperature = 0.45,
            maxTokens = 2000,
            jsonResponse = true,
        )
        var completion = complete(resolved, request)
        if (completion is AiStreamEvent.Failed && responseFormatUnsupported(completion.error)) {
            completion = complete(resolved, request.copy(jsonResponse = false))
        }
        val proposed = (completion as? AiStreamEvent.Completed)?.text?.let(::parse) ?: return null
        val actions = CharacterWorldPolicy.select(proposed, existing, events) { CharacterRegistry.getCard(it)?.data }
        val plan = WorldPlan(clock.dateLabel, clock.minutesOfDay, actions)
        return if (WorldPlanValidator.validate(plan, clock, existing, events, characters.map { it.id }.toSet())) plan else null
    }

    private suspend fun complete(resolved: com.example.data.ai.runtime.ResolvedProvider, request: AiChatRequest): AiStreamEvent? =
        try { resolved.provider.streamChat(request).firstOrNull { it is AiStreamEvent.Completed || it is AiStreamEvent.Failed || it is AiStreamEvent.Cancelled } }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { null }

    private fun responseFormatUnsupported(error: AiProviderError): Boolean {
        val http = error as? AiProviderError.Http ?: return false
        if (http.code !in setOf(400, 422)) return false
        val message = http.message.orEmpty().lowercase()
        return listOf("response_format", "json_object", "unsupported", "not supported", "unknown field", "invalid parameter")
            .any { it in message }
    }
}
