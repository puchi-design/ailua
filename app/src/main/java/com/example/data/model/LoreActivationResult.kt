package com.example.data.model

/**
 * LoreActivationResult — one WorldBook entry that `WorldData.getActiveLore()`
 * activated for the current context, plus why it fired and its effective
 * priority.
 *
 * Lives in `data.model` (NOT `data.mock`) so production consumers — the AI
 * Prompt Runtime first — can depend on it without dragging demo/mock world
 * fixtures across the domain boundary. This is the P3C-2.1 architecture fix:
 * `PromptAssemblyInput → data.model.LoreActivationResult`, zero `data.mock`.
 */
data class LoreActivationResult(
    val entry: LoreEntry,
    val activationReasons: List<String>,
    val effectivePriority: Int,
)
