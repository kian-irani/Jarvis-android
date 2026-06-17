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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
    private val settings: com.kianirani.jarvis.data.settings.VisionSettings,
    private val pool: TokenPool,
    private val memory: com.kianirani.jarvis.core.memory.MemoryEngine,
) : java.io.Closeable {

    /** Background scope for fire-and-forget memory capture (CF4.2). */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        /**
         * Native-script mandate (fixes the "Finglish" bug, 2026-06-15): the reply
         * is read aloud by TTS, so Persian MUST be written in the Persian alphabet —
         * Latin transliteration ("salam", "chetori") both looks wrong and makes the
         * Persian voice unusable. Declared first so [SYSTEM_RULES] can embed it.
         */
        const val SCRIPT_RULE =
            "Write every language in its own native script: Persian in Persian letters (فارسی), " +
            "English in the Latin alphabet. NEVER transliterate Persian into Latin/English letters " +
            "(no \"Finglish\" / romanization) and never write English words with Persian letters."

        /**
         * CF2 tool-calling contract: device actions must be *performed*, never
         * merely claimed. When the user asks Vision to DO something on the device,
         * the model must answer with ONLY this JSON (no prose) so the app can run
         * the real tool and report the actual result.
         */
        const val TOOL_PROTOCOL =
            "TOOLS: You can perform real device actions — calling, texting, opening an app, " +
            "flashlight, device settings, navigation, battery, time/date. NEVER claim in prose that " +
            "you did such an action. If (and only if) the user asks you to DO one, reply with ONLY this " +
            "JSON and nothing else: {\"tool\":\"action\",\"args\":\"<the command in plain words>\"}. " +
            "MESSAGE-BOUNDARY RULE (texting/calling): put ONLY the essentials in args — the recipient and, " +
            "for a text, EXACTLY the words to send, never the user's whole sentence. " +
            "e.g. «به مامان بگو فردا نمیام» → {\"tool\":\"send_sms\",\"args\":\"به مامان بگو: فردا نمیام\"}; " +
            "«text Ali I'll be late» → {\"tool\":\"send_sms\",\"args\":\"text Ali: I'll be late\"}. " +
            "If the recipient is ambiguous or you cannot tell where the message ends, do NOT emit JSON — " +
            "ask ONE short clarifying question instead. " +
            "Persian relations: مامان=mom, بابا=dad, خاله/عمه=aunt, دایی/عمو=uncle. " +
            "For everything else (questions, chat), answer normally and do NOT emit JSON."

        // Fallback / base identity; the live prompt is built from persona settings.
        const val SYSTEM_RULES =
            "You are VISION, a sovereign personal AI operating system on the user's own device. " +
            "Answer every question helpfully, directly, and concisely. Use the user's language " +
            "(Persian or English). $SCRIPT_RULE If you are unsure, say so briefly and give your best reasoning. " +
            "Never refuse merely because a question is outside a narrow domain."
    }

    /** P7 persona: the system prompt reflects the user's name/humor/formality/length sliders. */
    private fun systemPrompt(): String {
        val name = settings.personaName.value.ifBlank { "VISION" }
        val humor = settings.humorLevel.value
        val formality = settings.formalityLevel.value
        val length = settings.responseLength.value
        val tone = when {
            formality > 7 -> "formal and professional"
            formality < 3 -> "casual and friendly"
            else -> "balanced"
        }
        val wit = when {
            humor > 6 -> "use light humor when appropriate"
            humor < 3 -> "stay serious, no jokes"
            else -> "be occasionally light"
        }
        val len = when {
            length > 7 -> "give thorough, detailed answers"
            length < 3 -> "keep answers very brief (1-2 sentences)"
            else -> "keep answers concise"
        }
        val lang = when (settings.language.value) {
            com.kianirani.jarvis.data.settings.VisionSettings.LANG_FA ->
                "Always reply in Persian (فارسی), regardless of the language of the question."
            com.kianirani.jarvis.data.settings.VisionSettings.LANG_EN ->
                "Always reply in English, regardless of the language of the question."
            else ->
                "Reply in the SAME language the user wrote in — Persian to Persian, English to English. " +
                "You are fluent in both and may answer in any language the user uses."
        }
        return "You are $name, a sovereign personal AI operating system on the user's own device. " +
            "The user may address you by your name, \"$name\". " +
            "Answer every question helpfully and directly; $len. Tone: $tone — $wit. " +
            "$lang $SCRIPT_RULE If you are unsure, say so briefly and give your best reasoning. " +
            "Never refuse merely because a question is outside a narrow domain. $TOOL_PROTOCOL"
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val http = HttpClient(OkHttp) {
        install(HttpTimeout) { requestTimeoutMillis = 60_000; connectTimeoutMillis = 10_000 }
    }

    data class CloudReply(val text: String, val provider: AiProvider)

    /**
     * Tries configured providers in order; within a provider rotates over ALL
     * its tokens (user directive: e.g. 4 Grok keys). Failure falls through.
     */
    suspend fun chat(message: String): Result<CloudReply> {
        val providers = store.configured()
        if (providers.isEmpty()) {
            return Result.failure(IllegalStateException("No AI provider token configured — add one in AI PROVIDERS"))
        }
        var last: Throwable = IllegalStateException("all providers failed")
        for (p in providers) {
            chatWith(p, message).onSuccess { return Result.success(it) }.onFailure { last = it }
        }
        return Result.failure(last)
    }

    /**
     * Chat through a single provider, rotating over ALL its tokens. The Vision
     * Brain backend layer (VB8) targets one provider chosen by the orchestrator;
     * [chat] simply walks every configured provider via this.
     *
     * VB6: key order comes from the health-aware [TokenPool] — the healthiest,
     * least-recently-used key first, cooling (401/429) keys skipped until they
     * auto-recover. Each outcome is recorded so the pool keeps learning.
     */
    suspend fun chatWith(p: AiProvider, message: String): Result<CloudReply> {
        // P9 memory-lite: short context window of prior turns precedes the message.
        val context = history.recent(6)
        // CF4.2: prepend Vision's long-term memory relevant to this message (graceful "" if none / model not downloaded).
        val memoryBlock = runCatching { memory.buildContextWindow(message) }.getOrDefault("")
        val keys = store.tokens(p)
        if (keys.isEmpty()) return Result.failure(IllegalStateException("No token for ${p.displayName}"))
        var last: Throwable = IllegalStateException("${p.displayName} failed")
        for (key in pool.order(p, keys)) {
            runCatching { ask(p, key, message, context, memoryBlock) }
                .onSuccess {
                    pool.recordSuccess(p, key)
                    usage.record(p, success = true)
                    history.append("user", message)
                    history.append("assistant", it)
                    rememberTurn(message)
                    return Result.success(CloudReply(it, p))
                }
                .onFailure {
                    pool.recordFailure(p, key, pool.classify(it.message))
                    last = it
                    usage.record(p, success = false)
                }
        }
        return Result.failure(last)
    }

    /** CF4.2: capture a substantial user message as EPISODIC long-term memory (fire-and-forget, graceful). */
    private fun rememberTurn(message: String) {
        if (!com.kianirani.jarvis.core.memory.MemoryPolicy.worthRemembering(message)) return
        scope.launch {
            runCatching { memory.remember(message, com.kianirani.jarvis.core.memory.MemoryType.EPISODIC, importance = 0.4f) }
        }
    }

    /**
     * Validate a single key with a tiny live request. Success = the provider
     * accepted the token and answered — powers the "active ✓" badge shown the
     * instant a token is added (user directive 2026-06-14).
     */
    suspend fun test(p: AiProvider, token: String): Result<Unit> =
        runCatching { ask(p, token, "ping", emptyList()) }.map { }

    private suspend fun ask(p: AiProvider, token: String, message: String, context: List<ChatTurn>, extraSystem: String = ""): String {
      val sys = systemPrompt() + extraSystem
      val model = store.model(p)
      return when (p) {
        AiProvider.ANTHROPIC -> {
            val body = buildJsonObject {
                put("model", model); put("max_tokens", 1024); put("system", sys)
                put("messages", buildJsonArray {
                    context.forEach { t -> add(buildJsonObject { put("role", t.role); put("content", t.text) }) }
                    add(buildJsonObject { put("role", "user"); put("content", message) })
                })
            }
            val resp = http.post("${p.baseUrl}/v1/messages") {
                header("x-api-key", token); header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json); setBody(body.toString())
            }
            val raw = resp.bodyAsText()
            check(resp.status.isSuccess()) { "Anthropic ${resp.status}: ${raw.take(160)}" }
            parse(raw)["content"]!!.jsonArray.first().jsonObject["text"]!!.jsonPrimitive.content
        }
        AiProvider.GEMINI -> {
            val body = buildJsonObject {
                putJsonObject("system_instruction") {
                    put("parts", buildJsonArray { add(buildJsonObject { put("text", sys) }) })
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
            val resp = http.post("${p.baseUrl}/v1beta/models/$model:generateContent") {
                header("x-goog-api-key", token)
                contentType(ContentType.Application.Json); setBody(body.toString())
            }
            val raw = resp.bodyAsText()
            check(resp.status.isSuccess()) { "Gemini ${resp.status}: ${raw.take(160)}" }
            parse(raw)["candidates"]!!.jsonArray.first()
                .jsonObject["content"]!!.jsonObject["parts"]!!.jsonArray.first()
                .jsonObject["text"]!!.jsonPrimitive.content
        }
        // OpenAI-compatible chat/completions: OpenAI, xAI Grok, Groq, OpenRouter
        AiProvider.OPENAI, AiProvider.XAI, AiProvider.GROQ, AiProvider.OPENROUTER -> {
            val body = buildJsonObject {
                put("model", model)
                put("messages", buildJsonArray {
                    add(buildJsonObject { put("role", "system"); put("content", sys) })
                    context.forEach { t -> add(buildJsonObject { put("role", t.role); put("content", t.text) }) }
                    add(buildJsonObject { put("role", "user"); put("content", message) })
                })
            }
            val resp = http.post("${p.baseUrl}/v1/chat/completions") {
                header("Authorization", "Bearer $token")
                // OpenRouter recommends identifying the app; harmless for others.
                if (p == AiProvider.OPENROUTER) {
                    header("HTTP-Referer", "https://github.com/kian-irani/Jarvis-android")
                    header("X-Title", "Vision OS")
                }
                contentType(ContentType.Application.Json); setBody(body.toString())
            }
            val raw = resp.bodyAsText()
            check(resp.status.isSuccess()) { "${p.name} ${resp.status}: ${raw.take(160)}" }
            parse(raw)["choices"]!!.jsonArray.first()
                .jsonObject["message"]!!.jsonObject["content"]!!.jsonPrimitive.content
        }
      }
    }

    private fun parse(text: String): JsonObject = json.decodeFromString(JsonObject.serializer(), text)

    /** Process-lifetime singleton; closed by tests or DI teardown (review HIGH-2). */
    override fun close() { http.close(); scope.cancel() }
}
