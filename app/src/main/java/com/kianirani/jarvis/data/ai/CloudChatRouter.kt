package com.kianirani.jarvis.data.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standalone answer engine (USER DIRECTIVE 2026-06-12: Vision must answer
 * everything from v1 without a second device). Tries each configured provider
 * in [AiProvider] order until one answers. The system rules frame Vision's
 * behaviour across all providers.
 */
@Singleton
class CloudChatRouter @Inject constructor(
    private val store: AiProviderStore,
    private val history: ChatHistoryStore,
    private val usage: AiUsageStore,
) : java.io.Closeable {

    companion object {
        const val SYSTEM_RULES =
            "You are VISION, a sovereign personal AI operating system on the user's own device. " +
            "Answer every question helpfully, directly, and concisely. Use the user's language " +
            "(Persian or English). If you are unsure, say so briefly and give your best reasoning. " +
            "Never refuse merely because a question is outside a narrow domain."
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val http = HttpClient(OkHttp) {
        install(HttpTimeout) { requestTimeoutMillis = 60_000; connectTimeoutMillis = 10_000 }
    }

    data class CloudReply(val text: String, val provider: AiProvider)

    /** Round-robin start offset per provider — a 401/429 key is skipped on later calls. */
    private val rotation = java.util.concurrent.ConcurrentHashMap<AiProvider, Int>()

    /**
     * Tries configured providers in order; within a provider rotates over ALL
     * its tokens (user directive: e.g. 4 Grok keys). Failure falls through.
     */
    suspend fun chat(message: String): Result<CloudReply> {
        // P9 memory-lite: short context window of prior turns precedes the message.
        val context = history.recent(6)
        val providers = store.configured()
        if (providers.isEmpty()) {
            return Result.failure(IllegalStateException("No AI provider token configured — add one in AI PROVIDERS"))
        }
        var last: Throwable = IllegalStateException("all providers failed")
        for (p in providers) {
            val keys = store.tokens(p)
            if (keys.isEmpty()) continue
            val start = rotation.getOrDefault(p, 0) % keys.size
            for (i in keys.indices) {
                val idx = (start + i) % keys.size
                runCatching { ask(p, keys[idx], message, context) }
                    .onSuccess {
                        rotation[p] = idx // stick with the key that worked
                        usage.record(p, success = true)
                        history.append("user", message)
                        history.append("assistant", it)
                        return Result.success(CloudReply(it, p))
                    }
                    .onFailure { last = it; usage.record(p, success = false) }
            }
            rotation[p] = (start + 1) % keys.size
        }
        return Result.failure(last)
    }

    private suspend fun ask(p: AiProvider, token: String, message: String, context: List<ChatTurn>): String = when (p) {
        AiProvider.ANTHROPIC -> {
            val body = buildJsonObject {
                put("model", p.defaultModel); put("max_tokens", 1024); put("system", SYSTEM_RULES)
                put("messages", buildJsonArray {
                    context.forEach { t -> add(buildJsonObject { put("role", t.role); put("content", t.text) }) }
                    add(buildJsonObject { put("role", "user"); put("content", message) })
                })
            }
            val resp = http.post("${p.baseUrl}/v1/messages") {
                header("x-api-key", token); header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json); setBody(body.toString())
            }
            check(resp.status.isSuccess()) { "Anthropic ${resp.status}" }
            parse(resp.bodyAsText())["content"]!!.jsonArray.first().jsonObject["text"]!!.jsonPrimitive.content
        }
        AiProvider.GEMINI -> {
            val body = buildJsonObject {
                putJsonObject("system_instruction") {
                    put("parts", buildJsonArray { add(buildJsonObject { put("text", SYSTEM_RULES) }) })
                }
                put("contents", buildJsonArray {
                    context.forEach { t ->
                        add(buildJsonObject {
                            put("role", if (t.role == "assistant") "model" else "user")
                            put("parts", buildJsonArray { add(buildJsonObject { put("text", t.text) }) })
                        })
                    }
                    add(buildJsonObject { put("role", "user"); put("parts", buildJsonArray { add(buildJsonObject { put("text", message) }) }) })
                })
            }
            val resp = http.post("${p.baseUrl}/v1beta/models/${p.defaultModel}:generateContent") {
                header("x-goog-api-key", token)
                contentType(ContentType.Application.Json); setBody(body.toString())
            }
            check(resp.status.isSuccess()) { "Gemini ${resp.status}" }
            parse(resp.bodyAsText())["candidates"]!!.jsonArray.first()
                .jsonObject["content"]!!.jsonObject["parts"]!!.jsonArray.first()
                .jsonObject["text"]!!.jsonPrimitive.content
        }
        // OpenAI-compatible chat/completions: OpenAI, xAI Grok, Groq, OpenRouter
        AiProvider.OPENAI, AiProvider.XAI, AiProvider.GROQ, AiProvider.OPENROUTER -> {
            val body = buildJsonObject {
                put("model", p.defaultModel)
                put("messages", buildJsonArray {
                    add(buildJsonObject { put("role", "system"); put("content", SYSTEM_RULES) })
                    context.forEach { t -> add(buildJsonObject { put("role", t.role); put("content", t.text) }) }
                    add(buildJsonObject { put("role", "user"); put("content", message) })
                })
            }
            val resp = http.post("${p.baseUrl}/v1/chat/completions") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json); setBody(body.toString())
            }
            check(resp.status.isSuccess()) { "${p.name} ${resp.status}" }
            parse(resp.bodyAsText())["choices"]!!.jsonArray.first()
                .jsonObject["message"]!!.jsonObject["content"]!!.jsonPrimitive.content
        }
    }

    private fun parse(text: String): JsonObject = json.decodeFromString(JsonObject.serializer(), text)

    /** Process-lifetime singleton; closed by tests or DI teardown (review HIGH-2). */
    override fun close() { http.close() }
}
