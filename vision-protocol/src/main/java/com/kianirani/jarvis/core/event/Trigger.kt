package com.kianirani.jarvis.core.event

/**
 * VCF-R2 — a rule that runs [action] when an event of kind [on] fires and [condition]
 * holds (e.g. only for a specific app). Pure matching → unit-tested; the WorkManager /
 * runner wiring that actually executes [action] (a prompt or workflow id) is the
 * on-device part.
 */
data class Trigger(
    val id: String,
    val on: EventKind,
    val action: String,
    val condition: (VisionEvent) -> Boolean = { true },
)

object TriggerMatcher {
    /** The triggers that should fire for [event], in registration order. */
    fun matching(triggers: List<Trigger>, event: VisionEvent): List<Trigger> =
        triggers.filter { it.on == event.kind && it.condition(event) }
}
