package com.kianirani.jarvis.data.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Pluggable AI provider registry (USER DIRECTIVE 2026-06-12: token slots must
 * exist from v1; any number of tokens per provider, e.g. 4 Grok keys, with
 * rotation). Adding a provider = adding one enum entry; the settings screen
 * and router pick it up automatically.
 *
 * NOTE: GROQ (api.groq.com) and XAI Grok (api.x.ai) are different services —
 * an `xai-...` key on the Groq endpoint returns 401 (user-reported bug).
 */
enum class AiProvider(val displayName: String, val baseUrl: String, val defaultModel: String) {
    ANTHROPIC("Anthropic Claude", "https://api.anthropic.com", "claude-sonnet-4-6"),
    OPENAI("OpenAI", "https://api.openai.com", "gpt-4o-mini"),
    XAI("xAI Grok", "https://api.x.ai", "grok-3-mini"),
    GROQ("Groq", "https://api.groq.com/openai", "llama-3.3-70b-versatile"),
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com", "gemini-2.0-flash"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api", "auto"),
}

interface AiProviderStore {
    /** All tokens saved for the provider, in insertion order. */
    fun tokens(p: AiProvider): List<String>
    fun addToken(p: AiProvider, token: String)
    fun removeToken(p: AiProvider, token: String)
    fun clear(p: AiProvider)
    /** Providers with at least one token, in priority (enum) order. */
    fun configured(): List<AiProvider>
}

/** AES256/Keystore-encrypted token storage — same hardening as brain pairing. */
class PrefsAiProviderStore(context: Context) : AiProviderStore {
    private val json = Json
    private val prefs: SharedPreferences = runCatching {
        val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, "vision_ai_tokens", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrElse { context.getSharedPreferences("vision_ai_tokens_fallback", Context.MODE_PRIVATE) }

    private fun key(p: AiProvider) = "${p.name}_v2"

    override fun tokens(p: AiProvider): List<String> {
        prefs.getString(key(p), null)?.let { raw ->
            val saved = runCatching { json.decodeFromString(ListSerializer(String.serializer()), raw) }
                .getOrDefault(emptyList())
            // Fall back to baked keys if the user cleared all of theirs.
            return saved.ifEmpty { bakedKeys(p) }
        }
        // Migrate legacy single-token slot (pre multi-token) once.
        prefs.getString(p.name, null)?.takeIf { it.isNotBlank() }?.let { legacy ->
            write(p, listOf(legacy))
            prefs.edit().remove(p.name).apply()
            return listOf(legacy)
        }
        // Zero-config chat: ship-time keys (BuildConfig.GROQ_KEYS) so a fresh
        // install can talk before the user adds their own (USER DIRECTIVE 2026-06-12).
        return bakedKeys(p)
    }

    /** Comma-separated keys compiled into the build for [p]; empty if none. */
    private fun bakedKeys(p: AiProvider): List<String> = when (p) {
        AiProvider.GROQ -> com.kianirani.jarvis.BuildConfig.GROQ_KEYS
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }
        else -> emptyList()
    }

    override fun addToken(p: AiProvider, token: String) {
        val t = token.trim()
        if (t.isEmpty()) return
        write(p, (tokens(p) + t).distinct())
    }

    override fun removeToken(p: AiProvider, token: String) = write(p, tokens(p) - token)

    override fun clear(p: AiProvider) {
        prefs.edit().remove(key(p)).remove(p.name).apply()
    }

    override fun configured(): List<AiProvider> = AiProvider.entries.filter { tokens(it).isNotEmpty() }

    private fun write(p: AiProvider, list: List<String>) {
        prefs.edit().apply {
            if (list.isEmpty()) remove(key(p))
            else putString(key(p), json.encodeToString(ListSerializer(String.serializer()), list))
        }.apply()
    }
}
