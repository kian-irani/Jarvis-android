package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.graph.VisionMessage

/** How many queued steering messages to drain at once. */
enum class QueueMode { ALL, ONE }

/**
 * VCF-A5 — steering: messages the user types or says while the agent is mid-run, injected
 * into the conversation between steps (openclaw steering) so the agent can be redirected
 * without restarting. The runtime drains pending messages and reduces them into state.
 */
fun interface SteeringSource {
    suspend fun drain(): List<VisionMessage>
}

/** A thread-safe, queue-backed [SteeringSource] surfaces push into. */
class QueueSteeringSource(private val mode: QueueMode = QueueMode.ALL) : SteeringSource {
    private val queue = ArrayDeque<VisionMessage>()
    private val lock = Any()

    fun push(message: VisionMessage): Unit = synchronized(lock) { queue.addLast(message) }

    override suspend fun drain(): List<VisionMessage> = synchronized(lock) {
        when (mode) {
            QueueMode.ALL -> queue.toList().also { queue.clear() }
            QueueMode.ONE -> queue.removeFirstOrNull()?.let { listOf(it) } ?: emptyList()
        }
    }
}
