package com.example

import com.example.data.ai.provider.AiProvider
import com.example.data.ai.runtime.ChatGenerationRuntime
import com.example.data.ai.runtime.ChatPromptContext
import com.example.data.ai.runtime.ProviderResolver
import com.example.data.ai.runtime.ResolvedProvider
import com.example.data.model.CharacterCardData
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.LoreActivationResult
import com.example.data.model.LoreEntry
import com.example.data.projection.CharacterPresence

/** Fixed character card for runtime tests — real-shaped data, never MockData. */
internal fun miraRuntimeCard() = CharacterCardData(
    id = "mira",
    name = "Mira",
    description = "雨天茶馆的主人，温柔细腻。",
    personality = "温和，喜欢听雨。",
    scenario = "一家只在雨夜营业的茶馆。",
    firstMessage = "",
    exampleMessages = "",
    systemPrompt = "",
    postHistoryInstructions = "",
)

internal fun yunaRuntimeCard() = CharacterCardData(
    id = "yuna",
    name = "悠奈",
    description = "街角甜品店的店员，元气满满。",
    personality = "活泼，热爱探店。",
    scenario = "一家飘着焦糖香气的甜品店。",
    firstMessage = "",
    exampleMessages = "",
    systemPrompt = "",
    postHistoryInstructions = "",
)

internal fun runtimeLifeEvent(characterId: String, title: String) = LifeEvent(
    id = "ev_${characterId}_$title",
    characterId = characterId,
    time = "21:14",
    type = LifeEventType.THOUGHT,
    title = title,
    description = "虚拟世界事件。",
    location = "琉璃茶馆",
)

internal fun runtimeLore(title: String, content: String) = LoreActivationResult(
    entry = LoreEntry(id = "lore_$title", title = title, content = content),
    activationReasons = listOf("test"),
    effectivePriority = 20,
)

/** Deterministic prompt context: fixed cards/presence, settable lore/events. */
internal class FakeChatPromptContext : ChatPromptContext {

    var lore: List<LoreActivationResult> = emptyList()
    var lifeEvents: List<LifeEvent> = emptyList()

    private val cards = mapOf(
        "mira" to miraRuntimeCard(),
        "yuna" to yunaRuntimeCard(),
    )

    override fun characterCard(characterId: String): CharacterCardData? = cards[characterId]

    override fun presence(characterId: String): CharacterPresence? = CharacterPresence(
        characterId = characterId,
        currentActivity = "在窗边整理茶具",
        currentLocation = "琉璃茶馆",
        currentEventId = null,
    )

    override fun lifeEvents(characterId: String): List<LifeEvent> =
        lifeEvents.filter { it.characterId == characterId }

    override fun activeLore(
        characterId: String,
        locationId: String?,
        recentUserText: String,
        lifeEventTitle: String?,
    ): List<LoreActivationResult> = lore

    override fun temporal(): Pair<String, String> = "9月25日" to "21:30"

    override fun userName(): String = "旅人"
}

/** Scriptable resolver — `resolved = null` models "AI 连接未配置". */
internal class FakeProviderResolver(var resolved: ResolvedProvider?) : ProviderResolver {
    override fun resolve(): ResolvedProvider? = resolved
}

/** One wired runtime: JDBC harness + fake resolver + fake prompt context. */
internal class ChatRuntimeFixture {

    val chat = ChatTestHarness.inMemory()
    val promptContext = FakeChatPromptContext()
    val resolver = FakeProviderResolver(null)
    val runtime = ChatGenerationRuntime(chat.repository, resolver, promptContext)

    val repository get() = chat.repository

    fun use(provider: AiProvider, profileId: String = "prof-1", model: String = "test-model") {
        resolver.resolved = ResolvedProvider(profileId = profileId, model = model, provider = provider)
    }

    fun removeProvider() {
        resolver.resolved = null
    }

    fun sessionId(characterId: String): String =
        repository.getOrCreatePrivateSession(characterId).id

    fun turns(characterId: String) =
        repository.getResolvedTurns(sessionId(characterId))
}
