package com.kianirani.jarvis.core.brain

import com.kianirani.jarvis.core.gateway.Channel
import com.kianirani.jarvis.core.gateway.VisionGateway
import com.kianirani.jarvis.core.gateway.VisionRequest
import com.kianirani.jarvis.core.graph.GraphEvent
import com.kianirani.jarvis.core.graph.VisionMessage
import com.kianirani.jarvis.core.memory.MemoryEngine
import com.kianirani.jarvis.core.memory.MemoryType
import kotlinx.coroutines.flow.Flow

/**
 * DS-F1 — the single Brain facade (Dual-System foundation, "پایه‌ی همه").
 *
 * Wraps the already-built [VisionGateway] (the front door that routes a request to the
 * per-[Channel] agent) and the [MemoryEngine] (long-term memory) behind one stable
 * surface — `handle` / `resume` / `state` / `remember` / `recall` — so every surface
 * (HUD, floating widget, Android launcher, desktop shell) talks to one entry point
 * regardless of in-process vs network transport. This is **pure delegation with no
 * behaviour change**: it does not touch the live `HudViewModel` chat path; surfaces and
 * the SDK migrate onto it in later, on-device passes (DS-C1/DS-F3).
 */
class VisionBrain(
    private val gateway: VisionGateway,
    private val memory: MemoryEngine,
) {
    /** Drive a turn from any surface; streams the run as graph lifecycle events. */
    fun handle(request: VisionRequest): Flow<GraphEvent> = gateway.submit(request)

    /** Convenience for a plain-text turn (defaults to the MAIN channel + "main" session). */
    fun handle(text: String, sessionId: String = "main", channel: Channel = Channel.MAIN): Flow<GraphEvent> =
        gateway.submit(VisionRequest(text = text, sessionId = sessionId, channel = channel))

    /** Resume a session paused for confirmation (HIL), routed to the agent it started on. */
    fun resume(sessionId: String, answer: VisionMessage): Flow<GraphEvent> = gateway.resume(sessionId, answer)

    /** Active session ids (introspection / cleanup). */
    fun state(): Set<String> = gateway.activeSessions()

    /** Persist a durable fact/preference; null if it couldn't be stored (e.g. model not ready). */
    suspend fun remember(
        content: String,
        type: MemoryType = MemoryType.FACT,
        importance: Float = 0.5f,
        metadata: Map<String, String> = emptyMap(),
    ): String? = memory.remember(content, type, importance, metadata)

    /** Importance/recency-ranked semantic recall (empty on any failure). */
    suspend fun recall(query: String, topK: Int = 5): List<MemoryEngine.Recalled> = memory.recall(query, topK)
}
