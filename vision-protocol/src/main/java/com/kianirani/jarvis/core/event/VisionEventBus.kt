package com.kianirani.jarvis.core.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VCF-R2 — the typed event bus that drives Vision's proactive behaviour. Surfaces publish
 * [VisionEvent]s; the runtime subscribes and runs the agent graph for the matching
 * [Trigger]s. A hot SharedFlow with a buffer so publishers never block. Distinct from the
 * Brain-Lite server's `BrainEvent` bus (a separate, server-side concern).
 */
@Singleton
class VisionEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<VisionEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<VisionEvent> = _events.asSharedFlow()

    /** Live count of active subscribers — the runner can skip work when nobody is listening. */
    val subscriptionCount: StateFlow<Int> get() = _events.subscriptionCount

    /** Publish an event; suspends only if the buffer is somehow full. */
    suspend fun emit(event: VisionEvent) = _events.emit(event)

    /** Non-suspending publish for callers off a coroutine; false if the buffer is full. */
    fun tryEmit(event: VisionEvent): Boolean = _events.tryEmit(event)
}
