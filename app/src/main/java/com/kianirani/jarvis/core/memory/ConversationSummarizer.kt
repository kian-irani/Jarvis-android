package com.kianirani.jarvis.core.memory

import com.kianirani.jarvis.core.agent.ModelClient
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage

/**
 * VCF-MEM1 — keeps the conversation window small by compressing older turns into a single
 * SEMANTIC summary once it grows past [threshold] (PRD §11). The trigger
 * ([shouldSummarize]) is pure; [summarize] is model-backed via [ModelClient] (so it is
 * unit-tested with a fake). The caller stores the result as a SEMANTIC memory via
 * MemoryEngine. Graceful: returns "" when not needed or the model call fails.
 */
class ConversationSummarizer(
    private val client: ModelClient,
    private val threshold: Int = DEFAULT_THRESHOLD,
) {
    fun shouldSummarize(messages: List<VisionMessage>): Boolean = messages.size > threshold

    /** A short summary of durable facts/preferences/decisions, or "" when not needed / on failure. */
    suspend fun summarize(messages: List<VisionMessage>): String {
        if (!shouldSummarize(messages)) return ""
        val transcript = messages.joinToString("\n") { "${it.role}: ${it.text()}" }
        return runCatching { client.complete(listOf(prompt(transcript)), null).message.text().trim() }
            .getOrDefault("")
    }

    private fun prompt(transcript: String): VisionMessage = VisionMessage.text(
        Role.USER,
        "Summarize the durable facts, preferences, and decisions from this conversation in 2-3 " +
            "sentences for long-term memory. Omit small talk. Conversation:\n$transcript",
    )

    companion object {
        const val DEFAULT_THRESHOLD = 8
    }
}
