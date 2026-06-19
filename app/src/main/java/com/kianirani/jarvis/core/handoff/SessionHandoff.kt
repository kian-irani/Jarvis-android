package com.kianirani.jarvis.core.handoff

import com.kianirani.jarvis.core.graph.VisionMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * MX session handoff (PRD §14, "session handoff") — pick up a conversation on another device.
 * A [SessionSnapshot] is the serializable state needed to resume a session elsewhere (the
 * messages, the rolling summary, where it came from); [encode]/[decode] move it over the mesh.
 * Pure kotlinx-serialization → JVM-tested and convergent with the rest of the wire contract
 * (DS-F2). Capturing the live session and re-hydrating it into a running gateway are the
 * on-device half.
 */
@Serializable
data class SessionSnapshot(
    val sessionId: String,
    val messages: List<VisionMessage>,
    val createdAtMillis: Long,
    val summary: String? = null,
    val originNode: String = "",
)

object SessionHandoff {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Capture the current session into a transferable snapshot. */
    fun capture(
        sessionId: String,
        messages: List<VisionMessage>,
        now: Long,
        originNode: String = "",
        summary: String? = null,
    ): SessionSnapshot = SessionSnapshot(sessionId, messages, now, summary, originNode)

    /** Serialize a snapshot for the wire. */
    fun encode(snapshot: SessionSnapshot): String = json.encodeToString(snapshot)

    /** Parse a snapshot received from a peer; null on malformed input (never throws). */
    fun decode(payload: String): SessionSnapshot? =
        runCatching { json.decodeFromString<SessionSnapshot>(payload) }.getOrNull()

    /** True if the snapshot is older than [maxAgeMillis] (stale handoffs should be ignored). */
    fun isStale(snapshot: SessionSnapshot, now: Long, maxAgeMillis: Long): Boolean =
        now - snapshot.createdAtMillis > maxAgeMillis
}
