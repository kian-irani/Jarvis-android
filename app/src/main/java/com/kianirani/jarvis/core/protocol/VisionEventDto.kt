package com.kianirani.jarvis.core.protocol

import com.kianirani.jarvis.core.event.EventKind
import com.kianirani.jarvis.core.event.VisionEvent
import kotlinx.serialization.Serializable

/**
 * DS-F2 — the serializable wire mirror of the runtime [VisionEvent] (VCF-R2). The runtime
 * event is a `sealed interface` (not directly transport-friendly across surfaces); this flat
 * DTO carries the [kind] plus the payload of whichever variant it is, so events can cross the
 * network plane (DS-C2) and survive a JSON round-trip. [VisionEvent.toDto] / [toEvent] are
 * pure and lossless: `event.toDto().toEvent() == event`.
 */
@Serializable
data class VisionEventDto(
    val kind: EventKind,
    /** [VisionEvent.AppOpened.packageName] */
    val packageName: String? = null,
    /** [VisionEvent.UserIdle.millis] */
    val idleMillis: Long? = null,
    /** [VisionEvent.Scheduled.id] */
    val id: String? = null,
    /** [VisionEvent.Custom.name] */
    val name: String? = null,
    /** [VisionEvent.Custom.data] */
    val data: String? = null,
    /** [VisionEvent.CommandReceived.text] */
    val text: String? = null,
    /** [VisionEvent.CommandReceived.source] */
    val source: String? = null,
    /** [VisionEvent.ContextChanged.key] */
    val key: String? = null,
    /** [VisionEvent.ContextChanged.value] */
    val value: String? = null,
)

/** Flatten a runtime [VisionEvent] to its serializable wire form. */
fun VisionEvent.toDto(): VisionEventDto = when (this) {
    is VisionEvent.WakeWord -> VisionEventDto(EventKind.WAKE_WORD)
    is VisionEvent.AppOpened -> VisionEventDto(EventKind.APP_OPENED, packageName = packageName)
    is VisionEvent.UserIdle -> VisionEventDto(EventKind.USER_IDLE, idleMillis = millis)
    is VisionEvent.Scheduled -> VisionEventDto(EventKind.SCHEDULED, id = id)
    is VisionEvent.CommandReceived -> VisionEventDto(EventKind.COMMAND_RECEIVED, text = text, source = source)
    is VisionEvent.ContextChanged -> VisionEventDto(EventKind.CONTEXT_CHANGED, key = key, value = value)
    is VisionEvent.Custom -> VisionEventDto(EventKind.CUSTOM, name = name, data = data)
}

/** Rebuild the runtime [VisionEvent] from its wire form (missing payload → safe defaults). */
fun VisionEventDto.toEvent(): VisionEvent = when (kind) {
    EventKind.WAKE_WORD -> VisionEvent.WakeWord
    EventKind.APP_OPENED -> VisionEvent.AppOpened(packageName ?: "")
    EventKind.USER_IDLE -> VisionEvent.UserIdle(idleMillis ?: 0L)
    EventKind.SCHEDULED -> VisionEvent.Scheduled(id ?: "")
    EventKind.COMMAND_RECEIVED -> VisionEvent.CommandReceived(text ?: "", source ?: "main")
    EventKind.CONTEXT_CHANGED -> VisionEvent.ContextChanged(key ?: "", value ?: "")
    EventKind.CUSTOM -> VisionEvent.Custom(name ?: "", data ?: "")
}
