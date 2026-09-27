package com.example.data.ai.prompt

import com.example.data.ai.model.AiMessage
import com.example.data.ai.model.AiRole

/**
 * PromptStack — pure block collector that sorts, deduplicates, applies the
 * budget and renders the final `List<AiMessage>` (spec §7/§16).
 *
 * No database, no registry, no Context, no provider calls: the stack only
 * manipulates [PromptBlock]s handed to it. [PromptAssembler] is the single
 * entry point that builds those blocks; chat UI must never concatenate prompts.
 *
 * Determinism: rendering order is (category order, priority desc, insertion seq),
 * and every dropped block is recorded (blank / duplicate / budget) so the debug
 * result explains exactly what the prompt contains and why.
 */
class PromptStack(private val budget: PromptBudget = PromptBudget()) {

    private val kept = mutableListOf<SequencedBlock>()
    private val dropped = mutableListOf<SequencedBlock>()
    private var nextSeq = 0

    /** Blank content never becomes a message (spec §20 PromptEmptyBlockTest). */
    fun add(block: PromptBlock): PromptStack {
        val trimmed = block.copy(content = block.content.trim())
        val item = SequencedBlock(trimmed, nextSeq++)
        if (trimmed.content.isEmpty()) dropped += item else kept += item
        return this
    }

    fun build(): PromptAssemblyResult {
        val ordered = kept.sortedWith(RENDER_ORDER)

        val seenIds = mutableSetOf<String>()
        val seenContents = mutableSetOf<String>()
        val deduped = mutableListOf<SequencedBlock>()
        val duplicateDropped = mutableListOf<SequencedBlock>()
        for (item in ordered) {
            val idDuplicate = !seenIds.add(item.block.id)
            // Chat history may legitimately repeat identical turns — dedupe
            // applies only to fact blocks (lore / memory / events / …).
            val contentDuplicate = item.block.category != PromptCategory.HISTORY &&
                !seenContents.add(item.block.content)
            if (idDuplicate || contentDuplicate) duplicateDropped += item else deduped += item
        }

        val fit = budget.fit(deduped)
        val included = fit.included.sortedWith(RENDER_ORDER)
        val allDropped = (dropped + duplicateDropped + fit.dropped).sortedBy { it.seq }

        return PromptAssemblyResult(
            messages = render(included.map { it.block }),
            includedBlocks = included.map { it.block },
            droppedBlocks = allDropped.map { it.block },
            estimatedCost = included.sumOf { estimateTokens(it.block.content) },
            overflow = fit.overflow,
        )
    }

    private fun render(blocks: List<PromptBlock>): List<AiMessage> {
        val preHistory = blocks.filter {
            it.category != PromptCategory.HISTORY && it.category != PromptCategory.POST_HISTORY
        }
        val history = blocks.filter { it.category == PromptCategory.HISTORY }
        val post = blocks.filter { it.category == PromptCategory.POST_HISTORY }

        val messages = mutableListOf<AiMessage>()
        appendRoleGroups(preHistory, messages)
        appendRoleGroups(history, messages)
        appendRoleGroups(post, messages)
        return messages
    }

    /** Consecutive same-role blocks merge with a blank line; empty groups vanish. */
    private fun appendRoleGroups(blocks: List<PromptBlock>, out: MutableList<AiMessage>) {
        if (blocks.isEmpty()) return
        var role: AiRole = blocks.first().role
        val buffer = StringBuilder()
        for (block in blocks) {
            if (block.role != role) {
                if (buffer.isNotEmpty()) out += AiMessage(role, buffer.toString())
                buffer.setLength(0)
                role = block.role
            }
            if (buffer.isNotEmpty()) buffer.append("\n\n")
            buffer.append(block.content)
        }
        if (buffer.isNotEmpty()) out += AiMessage(role, buffer.toString())
    }

    private companion object {
        val RENDER_ORDER: Comparator<SequencedBlock> = compareBy(
            { it.block.category.order },
            { -it.block.priority },
            { it.seq },
        )
    }
}
