package com.kianirani.jarvis.core.graph

import kotlinx.serialization.Serializable

/**
 * VCF-G1 — the spine of the Vision Cognitive Framework (PRD §4.1).
 *
 * A message is a **list of typed content parts**, not a string. This is the single
 * change that unlocks multimodal input (image + audio) and structured tool calls,
 * while plain text stays a one-element `[Text]` list. Pure kotlinx-serialization,
 * no Android deps → JVM-testable and checkpoint-friendly (VCF-G3).
 */
@Serializable
enum class Role { SYSTEM, USER, ASSISTANT, TOOL }

/**
 * One piece of a message. Polymorphic so a single [VisionMessage] can mix text,
 * media, and tool traffic. Raw media is kept as bytes here; the per-provider
 * base64 / data-URI encoding is built at send time (PRD §8.3).
 */
@Serializable
sealed interface ContentPart {
    @Serializable
    data class Text(val text: String) : ContentPart

    /** Raw image bytes; [mime] drives per-provider encoding at send time. */
    @Serializable
    data class Image(val bytes: ByteArray, val mime: String = "image/png") : ContentPart {
        // ByteArray uses reference equality by default; compare by content so state
        // equality and serialization round-trips behave (VCF-G3 checkpointing).
        override fun equals(other: Any?): Boolean =
            this === other || (other is Image && mime == other.mime && bytes.contentEquals(other.bytes))

        override fun hashCode(): Int = 31 * bytes.contentHashCode() + mime.hashCode()
    }

    /** Raw audio bytes (e.g. captured speech) for local STT or a multimodal model. */
    @Serializable
    data class Audio(val bytes: ByteArray, val mime: String = "audio/ogg") : ContentPart {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Audio && mime == other.mime && bytes.contentEquals(other.bytes))

        override fun hashCode(): Int = 31 * bytes.contentHashCode() + mime.hashCode()
    }

    /** A function call the model asked for. */
    @Serializable
    data class ToolCall(val id: String, val name: String, val argsJson: String) : ContentPart

    /** The result we send back for a [ToolCall]. */
    @Serializable
    data class ToolResult(
        val callId: String,
        val name: String,
        val content: List<ContentPart>,
        val isError: Boolean = false,
    ) : ContentPart
}

/**
 * A single turn in the conversation. [name] is an optional speaker label (e.g. a
 * sub-agent name in a multi-agent crew, VCF-X).
 */
@Serializable
data class VisionMessage(
    val role: Role,
    val content: List<ContentPart>,
    val name: String? = null,
) {
    /** All [ContentPart.Text] joined — the plain-text view of this message. */
    fun text(): String = content.filterIsInstance<ContentPart.Text>().joinToString(" ") { it.text }

    /** Every tool call the model requested in this message. */
    fun toolCalls(): List<ContentPart.ToolCall> = content.filterIsInstance<ContentPart.ToolCall>()

    /** True if any part carries an image (drives multimodal provider encoding). */
    fun hasImage(): Boolean = content.any { it is ContentPart.Image }

    companion object {
        /** Convenience for the common text-only message. */
        fun text(role: Role, text: String): VisionMessage =
            VisionMessage(role, listOf(ContentPart.Text(text)))
    }
}
