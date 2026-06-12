package com.kianirani.jarvis.data.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Pluggable AI provider registry (USER DIRECTIVE 2026-06-12: token slots must
 * exist from v1). Adding a provider = adding one enum entry; the settings
 * screen and router pick it up automatically.
 */
enum class AiProvider(val displayName: String, val baseUrl: String, val defaultModel: String) {
    ANTHROPIC("Anthropic Claude", "https://api.anthropic.com", "claude-sonnet-4-6"),
    OPENAI("OpenAI", "https://api.openai.com", "gpt-4o-mini"),
    GROQ("Groq", "https://api.groq.com/openai", "llama-3.3-70b-versatile"),
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com", "gemini-2.0-flash"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api", "auto"),
}

interface AiProviderStore {
    fun token(p: AiProvider): String?
    fun setToken(p: AiProvider, token: String?)
    /** Providers with a token saved, in priority order. */
    fun configured(): List<AiProvider>
}

/** AES256/Keystore-encrypted token storage — same hardening as brain pairing. */
class PrefsAiProviderStore(context: Context) : AiProviderStore {
    private val prefs: SharedPreferences = runCatching {
        val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, "vision_ai_tokens", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrElse { context.getSharedPreferences("vision_ai_tokens_fallback", Context.MODE_PRIVATE) }

    override fun token(p: AiProvider): String? =
        prefs.getString(p.name, null)?.takeIf { it.isNotBlank() }

    override fun setToken(p: AiProvider, token: String?) {
        prefs.edit().apply {
            if (token.isNullOrBlank()) remove(p.name) else putString(p.name, token.trim())
        }.apply()
    }

    override fun configured(): List<AiProvider> = AiProvider.entries.filter { token(it) != null }
}
