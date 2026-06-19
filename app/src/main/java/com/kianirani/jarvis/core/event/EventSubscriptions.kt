package com.kianirani.jarvis.core.event

/**
 * DS-B5 — a pure subscriber registry over [VisionEvent]s. Where a [Trigger] is a *rule*
 * (event-kind + condition → an action string the runner later executes), a **subscriber**
 * is a live *handler* a component registers to react in-process (e.g. the ContextEngine
 * re-grounding on [VisionEvent.ContextChanged], or a logger on every event).
 *
 * Deliberately transport- and coroutine-free: registration + [dispatch] are synchronous and
 * deterministic so they're unit-testable; the [VisionEventBus] (a hot SharedFlow) remains the
 * async fan-out, and a thin collector can pump bus events into [dispatch]. A subscriber can
 * scope to a single [EventKind] or to all kinds ([Scope.ALL]); [dispatch] returns the ids that
 * fired (registration order) and never lets one handler's throw break the others.
 */
class EventSubscriptions {

    private sealed interface Scope {
        data object All : Scope
        data class Of(val kind: EventKind) : Scope
    }

    private data class Sub(val id: String, val scope: Scope, val handler: (VisionEvent) -> Unit)

    private val subs = mutableListOf<Sub>()

    /** Subscribe to a single [kind]. Re-registering the same [id] replaces the old handler. */
    fun on(kind: EventKind, id: String, handler: (VisionEvent) -> Unit) = register(Sub(id, Scope.Of(kind), handler))

    /** Subscribe to every event regardless of kind. */
    fun onAny(id: String, handler: (VisionEvent) -> Unit) = register(Sub(id, Scope.All, handler))

    /** Remove a subscriber; true if one was removed. */
    fun off(id: String): Boolean = subs.removeAll { it.id == id }

    /** Ids that would fire for [event], in registration order (introspection / testing). */
    fun subscribersFor(event: VisionEvent): List<String> = matching(event).map { it.id }

    /**
     * Deliver [event] to every matching handler in registration order; returns the ids that
     * ran. A handler that throws is isolated — the rest still receive the event.
     */
    fun dispatch(event: VisionEvent): List<String> {
        val fired = mutableListOf<String>()
        for (sub in matching(event)) {
            fired += sub.id
            runCatching { sub.handler(event) }
        }
        return fired
    }

    private fun register(sub: Sub) {
        subs.removeAll { it.id == sub.id }
        subs += sub
    }

    private fun matching(event: VisionEvent): List<Sub> =
        subs.filter { it.scope is Scope.All || (it.scope as Scope.Of).kind == event.kind }
}
