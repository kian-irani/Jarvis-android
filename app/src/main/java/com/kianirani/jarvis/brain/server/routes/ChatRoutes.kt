package com.kianirani.jarvis.brain.server.routes

import com.kianirani.jarvis.brain.data.ChatMessage
import com.kianirani.jarvis.brain.data.ChatReply
import com.kianirani.jarvis.brain.server.BrainException
import com.kianirani.jarvis.brain.server.success
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

interface ChatPort { suspend fun chat(messages: List<ChatMessage>, model: String?): ChatReply }

@Serializable
data class ChatRequest(val messages: List<ChatMessage>, val model: String? = null)

fun Route.chatRoutes(chat: ChatPort) {
    post("/chat") {
        val req = call.receive<ChatRequest>()
        if (req.messages.isEmpty()) throw BrainException.validation("messages must not be empty")
        call.respond(success(chat.chat(req.messages, req.model)))
    }
}
