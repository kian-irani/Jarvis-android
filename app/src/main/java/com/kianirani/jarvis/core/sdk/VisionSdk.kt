package com.kianirani.jarvis.core.sdk

import com.kianirani.jarvis.core.brain.VisionBrain
import com.kianirani.jarvis.core.gateway.Channel
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import com.kianirani.jarvis.core.memory.MemoryType
import com.kianirani.jarvis.core.protocol.MemoryDto
import com.kianirani.jarvis.core.protocol.VisionRequest
import com.kianirani.jarvis.core.protocol.VisionResponse
import com.kianirani.jarvis.core.sdk.ResponseProjector.asResponses
import kotlinx.coroutines.flow.Flow

/**
 * DS-F3 — **vision-sdk**: the transport-agnostic client every surface (floating widget,
 * Android launcher, desktop shell) codes against. A surface builds DS-F2 protocol
 * [VisionRequest]s and observes protocol [VisionResponse]s without knowing whether the
 * Brain is reached **in-process** ([InProcessVisionSdk] → [VisionBrain], = DS-C1) or over
 * the **network plane** (DS-C2). Its surface speaks only protocol types — no runtime
 * `GraphEvent` leaks out — so the same surface code works for either transport.
 */
interface VisionSdk {
    /** Drive a turn from a full protocol request; streams protocol responses. */
    fun send(request: VisionRequest): Flow<VisionResponse>

    /** Convenience for a plain-text turn. */
    fun send(text: String, sessionId: String = "main", channel: Channel = Channel.MAIN): Flow<VisionResponse>

    /** Resume a session paused for confirmation (HIL) with a plain answer string. */
    fun resume(sessionId: String, answer: String): Flow<VisionResponse>

    /** Persist a durable fact/preference; null if it couldn't be stored. */
    suspend fun remember(
        content: String,
        type: MemoryType = MemoryType.FACT,
        importance: Float = 0.5f,
        metadata: Map<String, String> = emptyMap(),
    ): String?

    /** Importance/recency-ranked semantic recall as protocol DTOs (empty on failure). */
    suspend fun recall(query: String, topK: Int = 5): List<MemoryDto>

    /** Active session ids (introspection / cleanup). */
    fun sessions(): Set<String>
}

/**
 * In-process adapter (DS-C1): binds the SDK to the same-process [VisionBrain] facade —
 * projecting its runtime [com.kianirani.jarvis.core.graph.GraphEvent] stream to protocol
 * responses, wrapping a plain confirmation string into the [VisionMessage] the runtime
 * `resume` expects, and mapping recalled memories to protocol [MemoryDto]s (relevance kept
 * in `metadata["score"]`). Pure delegation — no behaviour change to the Brain.
 */
class InProcessVisionSdk(private val brain: VisionBrain) : VisionSdk {

    override fun send(request: VisionRequest): Flow<VisionResponse> =
        brain.handle(request.toGatewayRequest()).asResponses(request.sessionId)

    override fun send(text: String, sessionId: String, channel: Channel): Flow<VisionResponse> =
        brain.handle(text, sessionId, channel).asResponses(sessionId)

    override fun resume(sessionId: String, answer: String): Flow<VisionResponse> =
        brain.resume(sessionId, VisionMessage.text(Role.USER, answer)).asResponses(sessionId)

    override suspend fun remember(
        content: String,
        type: MemoryType,
        importance: Float,
        metadata: Map<String, String>,
    ): String? = brain.remember(content, type, importance, metadata)

    override suspend fun recall(query: String, topK: Int): List<MemoryDto> =
        brain.recall(query, topK).map { r ->
            MemoryDto(content = r.content, type = r.type, id = r.id, metadata = mapOf("score" to r.score.toString()))
        }

    override fun sessions(): Set<String> = brain.state()
}
