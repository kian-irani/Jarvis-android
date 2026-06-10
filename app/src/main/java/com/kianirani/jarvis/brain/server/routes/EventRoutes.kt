package com.kianirani.jarvis.brain.server.routes

import com.kianirani.jarvis.brain.data.BrainEvent
import com.kianirani.jarvis.brain.data.EventBus
import com.kianirani.jarvis.brain.server.brainJson
import com.kianirani.jarvis.brain.server.success
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent

fun Route.eventRoutes(bus: EventBus) {
    post("/events") {
        val e = call.receive<BrainEvent>()
        bus.publish(e)
        call.respond(success(mapOf("published" to e.kind)))
    }
    sse("/events/stream") {
        bus.events.collect { e ->
            send(ServerSentEvent(data = brainJson.encodeToString(BrainEvent.serializer(), e), event = e.kind))
        }
    }
}
