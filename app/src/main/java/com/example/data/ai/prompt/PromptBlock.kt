package com.example.data.ai.prompt

import com.example.data.ai.model.AiRole

/**
 * PromptBlock — the single unit of prompt assembly.
 *
 * Blocks are pure data: no I/O, no repositories, no Android types. The category
 * determines WHERE a block renders (declaration order of [PromptCategory] is the
 * fixed render order), priority determines WHAT survives a budget squeeze, and
 * `required` marks blocks the budget may never drop (global system, character
 * core, post-history instructions — spec §15).
 */
data class PromptBlock(
    val id: String,
    val role: AiRole,
    val content: String,
    val priority: Int,
    val required: Boolean,
    val category: PromptCategory,
    /**
     * True for section headers (`Relevant memories:`, `Recent world events:`).
     * A header must never render headless: [PromptStack] drops it whenever the
     * budget left zero entries alive in its category (P3C-2.1 §orphan header).
     */
    val sectionHeader: Boolean = false,
) {
    companion object {
        fun of(
            category: PromptCategory,
            id: String,
            content: String,
            role: AiRole = AiRole.SYSTEM,
            priority: Int = category.defaultPriority,
            required: Boolean = category.requiredByDefault,
            sectionHeader: Boolean = false,
        ): PromptBlock = PromptBlock(id, role, content, priority, required, category, sectionHeader)
    }
}

/**
 * Fixed prompt order (spec §9). Declaration order IS render order:
 *
 * GLOBAL SYSTEM → CHARACTER CORE → PERSONA → CURRENT WORLD STATE → TIME →
 * ACTIVE LORE → MEMORY → RECENT LIFE EVENTS → CHAT HISTORY → POST-HISTORY.
 *
 * Default priorities implement the budget cut order (spec §15): higher priority
 * survives longer, so ascending-priority dropping yields
 * history → lore → life events → memory → time → world state → persona,
 * while required blocks (system / character core / post-history) never drop.
 */
enum class PromptCategory(
    val order: Int,
    val defaultPriority: Int,
    val requiredByDefault: Boolean,
) {
    SYSTEM(0, 1000, true),
    CHARACTER(1, 800, true),
    PERSONA(2, 700, false),
    WORLD_STATE(3, 600, false),
    TEMPORAL(4, 550, false),
    LORE(5, 300, false),
    MEMORY(6, 500, false),
    LIFE_EVENTS(7, 400, false),
    HISTORY(8, 200, false),
    POST_HISTORY(9, 900, true),
}

/**
 * A block plus its insertion sequence number. Sequence breaks ties: blocks with
 * equal (category, priority) keep insertion order for rendering and are dropped
 * earliest-inserted-first under budget pressure (oldest history first — spec §15).
 */
data class SequencedBlock(val block: PromptBlock, val seq: Int)
