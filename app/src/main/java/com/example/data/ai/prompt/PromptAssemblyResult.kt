package com.example.data.ai.prompt

import com.example.data.ai.model.AiMessage

/**
 * PromptAssemblyResult — the debug-visible outcome of one assembly (spec §17).
 *
 * All four fields describe this assembly only: the rendered messages that would
 * be sent to the provider, which blocks made it in, which were dropped (and via
 * which stage the caller can infer from `id`), and the rough token estimate.
 * `overflow` flags a budget too small even for the required blocks.
 *
 * Debug object only — it carries full chat content, so it must never be written
 * to Logcat or analytics wholesale, and it never contains keys or credentials.
 */
data class PromptAssemblyResult(
    val messages: List<AiMessage>,
    val includedBlocks: List<PromptBlock>,
    val droppedBlocks: List<PromptBlock>,
    val estimatedCost: Int,
    val overflow: Boolean = false,
)
