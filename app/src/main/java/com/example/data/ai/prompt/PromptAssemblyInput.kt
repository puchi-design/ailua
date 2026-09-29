package com.example.data.ai.prompt

import com.example.data.ai.model.AiMessage
import com.example.data.model.CharacterCardData
import com.example.data.model.LifeEvent
import com.example.data.model.LoreActivationResult
import com.example.data.projection.CharacterPresence

/**
 * PromptAssemblyInput — everything the Prompt Runtime may read (spec §8).
 *
 * Deliberately narrow: one character card (never an `AiCharacter`), already
 * activated lore (the caller runs `WorldData.getActiveLore()` — the assembler
 * never scans lore itself), an optional presence snapshot, this character's
 * recent life events, placeholder memories, chat history and a caller-supplied
 * date/time. Pure Kotlin + AILUA domain models only: no Context, no Room,
 * no OkHttp, no `java.io`/`java.net` (KMP rule §3).
 *
 * Isolation is enforced here: only `character`'s data may be passed, and events
 * / memories additionally carry their own owner so the assembler can drop
 * anything that leaked in from another character.
 */
data class PromptAssemblyInput(
    val character: CharacterCardData,
    val userPersona: String? = null,
    val activeLore: List<LoreActivationResult> = emptyList(),
    val worldState: CharacterPresence? = null,
    val recentLifeEvents: List<LifeEvent> = emptyList(),
    val memories: List<PromptMemory> = emptyList(),
    val history: List<AiMessage> = emptyList(),
    val currentDate: String = "",
    val currentTime: String = "",
    val userName: String = "",
    val realityContext: String? = null,
)

/**
 * Minimal memory shape for prompt assembly (Memory DB arrives in P3C-5).
 * `characterIds` empty = global memory; otherwise the block is only rendered
 * when it belongs to the assembled character.
 */
data class PromptMemory(
    val id: String = "",
    val title: String = "",
    val content: String,
    val meaning: String? = null,
    val characterIds: List<String> = emptyList(),
)
