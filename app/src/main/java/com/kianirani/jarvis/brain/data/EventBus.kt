package com.kianirani.jarvis.brain.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable

@Serializable
data class BrainEvent(val kind: String, val payload: String, val ts: Long = System.currentTimeMillis())

class EventBus {
    private val _events = MutableSharedFlow<BrainEvent>(replay = 16, extraBufferCapacity = 64)
    val events = _events.asSharedFlow()
    suspend fun publish(e: BrainEvent) = _events.emit(e)
}
