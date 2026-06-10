package com.kianirani.jarvis.brain.data

import com.kianirani.jarvis.brain.server.BrainException
import com.kianirani.jarvis.brain.server.brainJson
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.atomic.AtomicInteger

@Serializable data class ChatMessage(val role: String, val content: String)
@Serializable data class ChatReply(val role: String = "assistant", val content: String)

@Serializable private data class GroqRequest(val model: String, val messages: List<ChatMessage>)
@Serializable private data class GroqChoice(val message: ChatMessage)
@Serializable private data class GroqResponse(val choices: List<GroqChoice>)

class ChatRepository(
    private val keys: List<String>,
    private val baseUrl: String = "https://api.groq.com/openai/v1/",
    private val client: OkHttpClient = OkHttpClient(),
    private val defaultModel: String = "llama-3.3-70b-versatile",
) : com.kianirani.jarvis.brain.server.routes.ChatPort {
    private val keyIndex = AtomicInteger(0)
    val keyStatus = Array(keys.size) { "ok" }

    override suspend fun chat(messages: List<ChatMessage>, model: String?): ChatReply = withContext(Dispatchers.IO) {
        val body = brainJson.encodeToString(GroqRequest.serializer(), GroqRequest(model ?: defaultModel, messages))
        repeat(keys.size) {
            val i = keyIndex.get() % keys.size
            val req = Request.Builder()
                .url(baseUrl + "chat/completions")
                .header("Authorization", "Bearer ${keys[i]}")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { res ->
                if (res.code == 429) {
                    keyStatus[i] = "rate_limited"
                    keyIndex.incrementAndGet()
                } else if (res.isSuccessful) {
                    keyStatus[i] = "ok"
                    val parsed = brainJson.decodeFromString(GroqResponse.serializer(), res.body!!.string())
                    return@withContext ChatReply(content = parsed.choices.first().message.content)
                } else {
                    throw BrainException("PROVIDER_ERROR", HttpStatusCode.BadGateway, "Groq HTTP ${res.code}")
                }
            }
        }
        throw BrainException.allKeysLimited()
    }
}
