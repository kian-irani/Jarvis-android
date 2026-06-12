package com.kianirani.jarvis.data.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P9 (Memory System) v1 — persistent conversation memory. Stores the last
 * [MAX_TURNS] chat turns and feeds a short context window into every cloud
 * request so Vision remembers the conversation across app restarts.
 */
@Serializable
data class ChatTurn(val role: String, val text: String, val at: Long = System.currentTimeMillis())

@Singleton
class ChatHistoryStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("vision_chat_history", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(ChatTurn.serializer())

    fun all(): List<ChatTurn> =
        prefs.getString(KEY, null)?.let {
            runCatching { json.decodeFromString(serializer, it) }.getOrDefault(emptyList())
        } ?: emptyList()

    fun append(role: String, text: String) {
        val next = (all() + ChatTurn(role, text)).takeLast(MAX_TURNS)
        prefs.edit().putString(KEY, json.encodeToString(serializer, next)).apply()
    }

    /** Most recent turns, oldest first — context window for the router. */
    fun recent(n: Int = 6): List<ChatTurn> = all().takeLast(n)

    fun clear() = prefs.edit().remove(KEY).apply()

    companion object {
        private const val KEY = "turns"
        private const val MAX_TURNS = 60
    }
}
