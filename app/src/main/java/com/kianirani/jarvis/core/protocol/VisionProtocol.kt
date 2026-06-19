package com.kianirani.jarvis.core.protocol

import com.kianirani.jarvis.core.gateway.Channel
import kotlinx.serialization.Serializable
import com.kianirani.jarvis.core.gateway.VisionRequest as GatewayRequest

/**
 * DS-F2 — **vision-protocol**: the canonical, transport-ready contract shared by every
 * surface (floating widget, Android launcher, desktop shell) and the Brain. Pure Kotlin +
 * kotlinx-serialization, **no Android / no runtime dependencies** → KMP-ready for the
 * eventual `vision-protocol` module (DS-F4). This layer is purely additive: it does not
 * touch the live `HudViewModel` chat path; the in-process / network planes (DS-C1/DS-C2)
 * and the SDK (DS-F3) build requests/responses with these types and map to the existing
 * runtime ([com.kianirani.jarvis.core.gateway.VisionRequest], [VisionEventDto], …).
 *
 * The reused enums [Channel] (gateway) and [com.kianirani.jarvis.core.memory.MemoryType]
 * / [com.kianirani.jarvis.core.event.EventKind] are already pure; they move into this
 * protocol package when the Gradle modules split (DS-F4).
 */

/**
 * Classified user intent — *what* the user wants, decoupled from *how* it's fulfilled.
 * [name] is a stable verb-ish id (`chat`, `open_app`, `set_reminder`, `search`, `call`),
 * [slots] carry extracted arguments, [confidence] is in `0f..1f`.
 */
@Serializable
data class Intent(
    val name: String,
    val confidence: Float = 1f,
    val slots: Map<String, String> = emptyMap(),
) {
    companion object {
        /** The default fallback intent: a plain conversational turn. */
        val CHAT = Intent("chat")
    }
}

/** A binary attachment carried on the wire as base64 (or referenced by [uri]). */
@Serializable
data class Attachment(
    val kind: AttachmentKind,
    val mime: String,
    /** base64-encoded bytes; empty when the payload is referenced by [uri] instead. */
    val base64: String = "",
    /** optional `content://` / `https://` reference when the bytes aren't inlined. */
    val uri: String? = null,
)

/** Coarse type of an [Attachment]. */
@Serializable
enum class AttachmentKind { IMAGE, AUDIO, FILE }

/**
 * The device/environment snapshot a surface may attach to a request so the Brain can
 * ground its answer (CTX / DS-B2 / DS-W5). Every field is optional — a surface fills in
 * only what it can see (opt-in Accessibility, battery, network …).
 */
@Serializable
data class DeviceContext(
    val foregroundApp: String? = null,
    val batteryPercent: Int? = null,
    val charging: Boolean? = null,
    /** `wifi` | `cellular` | `offline`. */
    val network: String? = null,
    /** BCP-47, e.g. `fa-IR`. */
    val locale: String? = null,
    /** `morning` | `afternoon` | `evening` | `night`, or an ISO hour. */
    val timeOfDay: String? = null,
    val unreadNotifications: Int? = null,
    val extras: Map<String, String> = emptyMap(),
)

/**
 * A unit of work submitted to Vision from any surface — the wire envelope. Richer than the
 * minimal in-app [GatewayRequest] (it carries [locale], [intent], [deviceContext] and
 * [attachments]); [toGatewayRequest] narrows it for the current in-process gateway until
 * the gateway consumes the full envelope (DS-C1).
 */
@Serializable
data class VisionRequest(
    val text: String,
    val sessionId: String = "main",
    val channel: Channel = Channel.MAIN,
    val locale: String? = null,
    val intent: Intent? = null,
    val deviceContext: DeviceContext? = null,
    val attachments: List<Attachment> = emptyList(),
) {
    /** Narrow to the minimal in-process gateway request (drops wire-only context for now). */
    fun toGatewayRequest(): GatewayRequest =
        GatewayRequest(text = text, sessionId = sessionId, channel = channel)
}

/**
 * The result of a turn — what a remote/desktop surface receives over the network plane
 * (DS-C2). [finished] = the run completed; [awaitingConfirmation] = the run paused for a
 * human-in-the-loop trust gate (resume with an answer); [error] is non-null on failure.
 */
@Serializable
data class VisionResponse(
    val sessionId: String,
    val text: String = "",
    val intent: Intent? = null,
    val finished: Boolean = true,
    val awaitingConfirmation: Boolean = false,
    val error: String? = null,
)
