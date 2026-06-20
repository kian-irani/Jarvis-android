package com.kianirani.jarvis.core.chat

import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage

/**
 * Conversation export — turn a chat history ([VisionMessage]s) into shareable Markdown or plain
 * text, so the user can save/share a session. Pure formatting (system turns optionally omitted,
 * speaker-labelled) → JVM-tested; writing the file / share-sheet is the device half.
 */
object ConversationExport {

    /** Markdown: `**You:** …` / `**Vision:** …`, system turns dropped unless [includeSystem]. */
    fun toMarkdown(messages: List<VisionMessage>, assistantName: String = "Vision", includeSystem: Boolean = false): String =
        messages.filter { includeSystem || it.role != Role.SYSTEM }
            .joinToString("\n\n") { "**${speaker(it.role, assistantName)}:** ${it.text().trim()}" }

    /** Plain text, "You: …" lines. */
    fun toPlainText(messages: List<VisionMessage>, assistantName: String = "Vision", includeSystem: Boolean = false): String =
        messages.filter { includeSystem || it.role != Role.SYSTEM }
            .joinToString("\n") { "${speaker(it.role, assistantName)}: ${it.text().trim()}" }

    private fun speaker(role: Role, assistantName: String): String = when (role) {
        Role.USER -> "You"
        Role.ASSISTANT -> assistantName
        Role.SYSTEM -> "System"
        Role.TOOL -> "Tool"
    }
}
