package com.kianirani.jarvis.core.gateway

import com.kianirani.jarvis.core.agent.VisionAgent
import com.kianirani.jarvis.core.graph.GraphEvent
import com.kianirani.jarvis.core.graph.VisionMessage
import kotlinx.coroutines.flow.Flow

/** Where a request came from — drives which agent (and tool allowlist / trust) handles it. */
enum class Channel { MAIN, GROUP, WIDGET, REMOTE }

/** A unit of work submitted to Vision from any surface. */
data class VisionRequest(
    val text: String,
    val sessionId: String = "main",
    val channel: Channel = Channel.MAIN,
)

/**
 * VCF-R1 — the single front door (PRD §10). A surface (HUD, widget, group chat, remote)
 * submits a [VisionRequest]; the gateway picks the [VisionAgent] for that [Channel]
 * (different channels get different tool allowlists / trust) and streams back the run.
 * Sessions are isolated by [VisionRequest.sessionId] (the agent's checkpointer threads);
 * the gateway remembers each session's channel so [resume] re-enters the same agent.
 * Surfaces stay thin — they only build requests and render events.
 */
class VisionGateway(private val agentFor: (Channel) -> VisionAgent) {
    private val sessionChannel = mutableMapOf<String, Channel>()

    /** Submit a request and stream the run as graph lifecycle events. */
    fun submit(request: VisionRequest): Flow<GraphEvent> {
        sessionChannel[request.sessionId] = request.channel
        return agentFor(request.channel).run(request.text, threadId = request.sessionId)
    }

    /** Resume a session paused for confirmation, routing to the agent it started on. */
    fun resume(sessionId: String, answer: VisionMessage): Flow<GraphEvent> =
        agentFor(sessionChannel[sessionId] ?: Channel.MAIN).resume(sessionId, answer)

    /** Sessions seen so far (introspection / cleanup). */
    fun activeSessions(): Set<String> = sessionChannel.keys.toSet()
}
