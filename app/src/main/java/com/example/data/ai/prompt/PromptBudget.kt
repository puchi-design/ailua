package com.example.data.ai.prompt

/**
 * PromptBudget — how large the assembled input may grow, and how history is
 * protected while it shrinks (spec §15).
 *
 * Both values are rough token counts estimated deterministically as
 * `ceil(chars / 4)` — no tokenizer, no I/O, identical on every platform.
 * `historyReserve` is the maximum share the chat history may occupy before it is
 * the first thing trimmed; other optional blocks (lore, life events, memory…)
 * are only touched once history is already down to its reserve.
 */
data class PromptBudget(
    val maxInputBudget: Int = DEFAULT_MAX_INPUT_BUDGET,
    val historyReserve: Int = DEFAULT_HISTORY_RESERVE,
) {
    init {
        require(maxInputBudget >= 0) { "maxInputBudget must be >= 0" }
        require(historyReserve >= 0) { "historyReserve must be >= 0" }
    }

    /**
     * Trims [items] until the estimated cost fits [maxInputBudget].
     *
     * 1. If over budget, drop oldest history blocks (ascending sequence) while
     *    history cost is above [historyReserve].
     * 2. If still over, repeatedly drop the lowest-priority optional block —
     *    never a `required` block, never a HISTORY block (the reserve floor).
     * 3. If required blocks alone exceed the budget, keep everything that is
     *    left and report `overflow = true`; callers surface this in debug
     *    output instead of substring-mangling content.
     *
     * Input must already be deduplicated; output keeps render order for
     * `included` and insertion order for `dropped` (deterministic either way).
     */
    fun fit(items: List<SequencedBlock>): BudgetFitResult {
        val included = items.toMutableList()
        val dropped = mutableListOf<SequencedBlock>()
        var cost = items.sumOf { estimateTokens(it.block.content) }
        if (cost <= maxInputBudget) return BudgetFitResult(included, dropped, overflow = false)

        while (cost > maxInputBudget) {
            val history = included.filter { it.block.category == PromptCategory.HISTORY }
            if (history.isEmpty()) break
            val historyCost = history.sumOf { estimateTokens(it.block.content) }
            if (historyCost <= historyReserve) break
            val oldest = history.minByOrNull { it.seq } ?: break
            included.remove(oldest)
            dropped += oldest
            cost -= estimateTokens(oldest.block.content)
        }

        while (cost > maxInputBudget) {
            val victim = included
                .filter { !it.block.required && it.block.category != PromptCategory.HISTORY }
                .minWithOrNull(compareBy({ it.block.priority }, { it.seq })) ?: break
            included.remove(victim)
            dropped += victim
            cost -= estimateTokens(victim.block.content)
        }

        return BudgetFitResult(included, dropped, overflow = cost > maxInputBudget)
    }

    companion object {
        const val DEFAULT_MAX_INPUT_BUDGET: Int = 8_000
        const val DEFAULT_HISTORY_RESERVE: Int = 2_500
    }
}

/** Deterministic rough token estimate: 1 token ≈ 4 characters, rounded up. */
fun estimateTokens(text: String): Int = if (text.isEmpty()) 0 else (text.length + 3) / 4

/** Result of [PromptBudget.fit]; `overflow` means required blocks still exceed the budget. */
data class BudgetFitResult(
    val included: List<SequencedBlock>,
    val dropped: List<SequencedBlock>,
    val overflow: Boolean,
)
