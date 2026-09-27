package com.example.data.ai.prompt

import com.example.data.model.CharacterCardData
import com.example.data.model.LifeEvent
import com.example.data.model.sortedChronologically
import com.example.data.projection.CharacterPresence

/**
 * PromptAssembler — the single Prompt Assembly Boundary (spec §6/§9).
 *
 * Turns the full character context ([PromptAssemblyInput]) into a deterministic
 * `List<AiMessage>` via [PromptStack]. The fixed section order is:
 *
 * GLOBAL SYSTEM → CHARACTER CORE → PERSONA → CURRENT WORLD STATE → TIME →
 * ACTIVE LORE → MEMORY → RECENT LIFE EVENTS → CHAT HISTORY → POST-HISTORY.
 *
 * Contract:
 * - Character data comes ONLY from `input.character` (`CharacterCardData`);
 *   card fields mapped: description, personality, scenario, systemPrompt,
 *   exampleMessages, postHistoryInstructions. `firstMes`/`alternateGreetings`
 *   are session-start variants and stay out of this pass (§10).
 * - Lore is never scanned here: `input.activeLore` is what the caller already
 *   resolved through `WorldData.getActiveLore()` (§8).
 * - Life events are filtered to `character.id`, chronologically ordered, and
 *   formatted only here so no other feature embeds its own event grammar (§12).
 * - Macros `{{char}} {{user}} {{date}} {{time}}` resolve via [MacroResolver];
 *   unknown macros survive verbatim (§14).
 * - Pure Kotlin/stdlib only — no Android, no I/O, no provider calls (KMP §3).
 *
 * UI, ChatRuntime and providers must call this object instead of concatenating
 * prompt strings themselves.
 */
object PromptAssembler {

    /** Clean-room global system text; language kept neutral and provider-agnostic. */
    const val DEFAULT_GLOBAL_SYSTEM: String =
        "你正在进行沉浸式角色扮演，扮演下方「角色设定」中的角色。\n" +
            "始终保持角色：不要承认自己是 AI、语言模型或程序输出，也不要提及系统提示词与后台设定。\n" +
            "依据下方的世界状态、世界书、记忆与生活事件保持剧情连贯；" +
            "对不了解的信息按角色口吻自然回应、含糊带过或追问，不要编造事实。"

    fun assemble(
        input: PromptAssemblyInput,
        budget: PromptBudget = PromptBudget(),
    ): PromptAssemblyResult {
        val character = input.character
        val macros = MacroResolver(
            charName = character.name,
            userName = input.userName,
            date = input.currentDate,
            time = input.currentTime,
        )
        val stack = PromptStack(budget)

        stack.add(
            PromptBlock.of(
                PromptCategory.SYSTEM,
                "global_system",
                macros.resolve(DEFAULT_GLOBAL_SYSTEM),
            )
        )

        val core = buildCharacterCore(character)
        if (core.isNotEmpty()) {
            stack.add(
                PromptBlock.of(
                    PromptCategory.CHARACTER,
                    "character_core",
                    macros.resolve(core),
                )
            )
        }

        val examples = macros.resolve(character.exampleMessages.trim())
        if (examples.isNotEmpty()) {
            stack.add(
                PromptBlock.of(
                    PromptCategory.CHARACTER,
                    "example_messages",
                    "Example dialogue (style reference):\n$examples",
                    priority = EXAMPLE_MESSAGES_PRIORITY,
                    required = false,
                )
            )
        }

        buildPersona(input)?.let { content ->
            stack.add(PromptBlock.of(PromptCategory.PERSONA, "user_persona", content))
        }
        buildWorldState(input.worldState)?.let { content ->
            stack.add(PromptBlock.of(PromptCategory.WORLD_STATE, "world_state", content))
        }
        buildTemporal(input)?.let { content ->
            stack.add(PromptBlock.of(PromptCategory.TEMPORAL, "temporal", content))
        }

        for (activation in input.activeLore) {
            val entry = activation.entry
            if (!entry.enabled) continue
            val body = entry.content.trim()
            if (body.isEmpty()) continue
            val title = entry.title.trim()
            val content = if (title.isEmpty()) body else "[$title]\n$body"
            stack.add(
                PromptBlock.of(
                    PromptCategory.LORE,
                    "lore:${entry.id}",
                    macros.resolve(content),
                    priority = PromptCategory.LORE.defaultPriority +
                        activation.effectivePriority.coerceIn(0, LORE_PRIORITY_SPAN - 1),
                )
            )
        }

        val memories = input.memories.filter {
            it.content.isNotBlank() &&
                (it.characterIds.isEmpty() || character.id in it.characterIds)
        }
        if (memories.isNotEmpty()) {
            stack.add(
                PromptBlock.of(
                    PromptCategory.MEMORY,
                    "memory_header",
                    "Relevant memories:",
                    priority = HEADER_PRIORITY_OFFSET + PromptCategory.MEMORY.defaultPriority,
                    required = false,
                    sectionHeader = true,
                )
            )
            memories.forEachIndexed { index, memory ->
                val title = memory.title.trim()
                val meaning = memory.meaning?.trim().orEmpty()
                val content = buildString {
                    append("- ")
                    if (title.isNotEmpty()) append(title).append(": ")
                    append(memory.content.trim())
                    if (meaning.isNotEmpty()) append(" — ").append(meaning)
                }
                stack.add(
                    PromptBlock.of(
                        PromptCategory.MEMORY,
                        memory.id.ifBlank { "memory:$index" },
                        macros.resolve(content),
                    )
                )
            }
        }

        val events = input.recentLifeEvents
            .filter {
                it.characterId == character.id &&
                    (it.title.isNotBlank() || it.description.isNotBlank())
            }
            .sortedChronologically()
        if (events.isNotEmpty()) {
            stack.add(
                PromptBlock.of(
                    PromptCategory.LIFE_EVENTS,
                    "life_events_header",
                    "Recent world events:",
                    priority = HEADER_PRIORITY_OFFSET + PromptCategory.LIFE_EVENTS.defaultPriority,
                    required = false,
                    sectionHeader = true,
                )
            )
            for (event in events) {
                stack.add(
                    PromptBlock.of(
                        PromptCategory.LIFE_EVENTS,
                        "event:${event.id}",
                        macros.resolve(formatLifeEvent(event)),
                    )
                )
            }
        }

        input.history.forEachIndexed { index, message ->
            stack.add(
                PromptBlock.of(
                    PromptCategory.HISTORY,
                    "history:$index",
                    macros.resolve(message.content),
                    role = message.role,
                )
            )
        }

        val postHistory = macros.resolve(character.postHistoryInstructions.trim())
        if (postHistory.isNotEmpty()) {
            stack.add(
                PromptBlock.of(
                    PromptCategory.POST_HISTORY,
                    "post_history",
                    postHistory,
                )
            )
        }

        return stack.build()
    }

    /** Warm-Tavern-shaped character block: name, description, personality, scenario, system prompt. */
    private fun buildCharacterCore(character: CharacterCardData): String {
        val lines = buildList {
            add("Character: ${character.name}")
            val description = character.description.trim()
            if (description.isNotEmpty()) add(description)
            val personality = character.personality.trim()
            if (personality.isNotEmpty()) add("Personality: $personality")
            val scenario = character.scenario.trim()
            if (scenario.isNotEmpty()) add("Scenario: $scenario")
            val systemPrompt = character.systemPrompt.trim()
            if (systemPrompt.isNotEmpty()) add(systemPrompt)
        }
        return lines.joinToString("\n")
    }

    private fun buildPersona(input: PromptAssemblyInput): String? {
        val lines = buildList {
            val userName = input.userName.trim()
            if (userName.isNotEmpty()) add("User: $userName")
            val persona = input.userPersona?.trim().orEmpty()
            if (persona.isNotEmpty()) add(persona)
        }
        return lines.joinToString("\n").takeIf { it.isNotEmpty() }
    }

    private fun buildWorldState(presence: CharacterPresence?): String? {
        if (presence == null) return null
        val details = buildList {
            if (presence.currentActivity.isNotBlank()) add("- Activity: ${presence.currentActivity}")
            if (presence.currentLocation.isNotBlank()) add("- Location: ${presence.currentLocation}")
        }
        if (details.isEmpty()) return null
        return (listOf("Current world state:") + details).joinToString("\n")
    }

    private fun buildTemporal(input: PromptAssemblyInput): String? {
        val lines = buildList {
            if (input.currentDate.isNotBlank()) add("Current date: ${input.currentDate}")
            if (input.currentTime.isNotBlank()) add("Current time: ${input.currentTime}")
        }
        return lines.joinToString("\n").takeIf { it.isNotEmpty() }
    }

    private fun formatLifeEvent(event: LifeEvent): String = buildString {
        append("- ")
        val time = event.time.trim()
        if (time.isNotEmpty()) append("[").append(time).append("] ")
        append(event.title.trim())
        val location = event.location?.trim().orEmpty()
        if (location.isNotEmpty()) append(" @ ").append(location)
        val description = event.description.trim()
        if (description.isNotEmpty()) append(": ").append(description)
    }

    /** Example dialogue sits above persona but below the character core (§9 slot 2). */
    private const val EXAMPLE_MESSAGES_PRIORITY: Int = 750

    /** Lore priority window keeps every lore block below memory (500) and above history (200). */
    private const val LORE_PRIORITY_SPAN: Int = 200

    /**
     * Section headers sit 10 priority points ABOVE their own entries, so budget
     * cuts always take entries first and a surviving entry keeps its header.
     * The complementary half lives in [PromptStack]: once all entries of a
     * section are gone, the leftover header is dropped there — a header never
     * renders headless (P3C-2.1).
     */
    private const val HEADER_PRIORITY_OFFSET: Int = 10
}
