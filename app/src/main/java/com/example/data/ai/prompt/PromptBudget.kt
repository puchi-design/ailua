package com.example.data.ai.prompt

/**
 * PromptBudget — how large the assembled input may grow, and how history is
 * protected while it shrinks (spec §15, hardened in P3C-2.1).
 *
 * Both values are rough token counts estimated deterministically as
 * `ceil(chars / 4)` — no tokenizer, no I/O, identical on every platform.
 * `historyReserve` is a SOFT line: history is trimmed to it first, but when
 * the remaining optional context is not enough, history keeps yielding below
 * the reserve before `overflow` may ever be declared — a provider hard limit
 * must be real, not merely preferred.
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
     * Trims [items] until the estimated cost fits [maxInputBudget], in four
     * phases (P3C-2.1):
     *
     * 1. Drop oldest history while history cost is above [historyReserve].
     * 2. Drop the lowest-priority optional block (never `required`, never
     *    history) — lore → life events → memory → … by category priority.
     * 3. The reserve is soft: if still over, keep dropping the OLDEST history
     *    (potentially all of it) rather than overflowing.
     * 4. Only when the `required` blocks alone exceed the budget report
     *    `overflow = true`; callers surface that in debug output instead of
     *    substring-mangling content.
     *
     * Input must already be deduplicated; output keeps render order for
     * `included` and insertion order for `dropped` (deterministic either way).
     */
    fun fit(items: List<SequencedBlock>): BudgetFitResult {
        val included = items.toMutableList()
        val dropped = mutableListOf<SequencedBlock>()
        var cost = items.sumOf { estimateTokens(it.block.content) }
        if (cost <= maxInputBudget) return BudgetFitResult(included, dropped, overflow = false)

        // Phase 1 — trim oldest history down to the (soft) reserve line.
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

        // Phase 2 — lowest-priority optional context first (required never, history not yet).
        while (cost > maxInputBudget) {
            val victim = included
                .filter { !it.block.required && it.block.category != PromptCategory.HISTORY }
                .minWithOrNull(compareBy({ it.block.priority }, { it.seq })) ?: break
            included.remove(victim)
            dropped += victim
            cost -= estimateTokens(victim.block.content)
        }

        // Phase 3 — reserve is soft: history keeps yielding before overflow.
        while (cost > maxInputBudget) {
            val oldest = included
                .filter { it.block.category == PromptCategory.HISTORY }
                .minByOrNull { it.seq } ?: break
            included.remove(oldest)
            dropped += oldest
            cost -= estimateTokens(oldest.block.content)
        }

        // Phase 4 — only required blocks left; overflow means even they don't fit.
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
