package com.kianirani.jarvis.core.memory

import com.kianirani.jarvis.brain.data.MemoryRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CF4 MemoryEngine (PRD Part 4) — Vision's typed long-term memory. A thin, graceful
 * layer over the existing on-device vector store ([MemoryRepository]: MiniLM
 * embeddings + cosine search in Room). It adds [MemoryType] typing, importance, and
 * recency-decayed ranking ([MemoryScoring]), and produces a [buildContextWindow]
 * block to inject into the chat system prompt.
 *
 * Importance is carried inside the row's `metadata` JSON, so no Room migration is
 * needed. Every call is best-effort: if the embedding model isn't downloaded yet
 * the store/search throws and we degrade to a no-op / empty result — never crash.
 */
@Singleton
class MemoryEngine @Inject constructor(
    private val repo: MemoryRepository,
    private val preferences: PreferenceLearner,
) {
    private val json = Json { ignoreUnknownKeys = true }

    data class Recalled(val id: String, val content: String, val type: MemoryType, val score: Float)

    /** A stored memory as shown in the Memory screen (CF4.3 browse). */
    data class Stored(
        val id: String,
        val content: String,
        val type: MemoryType,
        val importance: Float,
        val createdAt: Long,
    )

    /** Store a memory; returns its id, or null if it couldn't be embedded (e.g. model not ready). */
    suspend fun remember(
        content: String,
        type: MemoryType,
        importance: Float = 0.5f,
        metadata: Map<String, String> = emptyMap(),
    ): String? {
        if (content.isBlank()) return null
        val meta = buildJsonObject {
            put("importance", importance.coerceIn(0f, 1f))
            metadata.forEach { (k, v) -> put(k, v) }
        }.toString()
        return runCatching { repo.store(type.name, content, meta) }.getOrNull()
    }

    /** Importance/recency-ranked semantic recall. Empty on any failure (graceful). */
    suspend fun recall(
        query: String,
        topK: Int = 5,
        pool: Int = 24,
        now: Long = System.currentTimeMillis(),
    ): List<Recalled> {
        if (query.isBlank()) return emptyList()
        val hits = runCatching { repo.searchDetailed(query, pool) }.getOrDefault(emptyList())
        return hits
            .map { h ->
                val rank = MemoryScoring.rankScore(
                    similarity = h.score,
                    importance = importanceOf(h.metadata),
                    ageMillis = now - h.createdAt,
                )
                Recalled(h.id, h.content, MemoryType.fromName(h.type), rank)
            }
            .sortedByDescending { it.score }
            .take(topK)
    }

    /** A compact memory block to prepend to the model's system prompt; "" when nothing relevant. */
    suspend fun buildContextWindow(query: String, topK: Int = 3, maxChars: Int = 600): String {
        val mems = recall(query, topK)
        if (mems.isEmpty()) return ""
        val body = mems.joinToString("\n") { "• ${it.content}" }.take(maxChars)
        return "\n\n[MEMORY — what you remember about this user]\n$body\n[/MEMORY]"
    }

    /** Most-recent stored memories for the Memory screen; empty on any failure (graceful). */
    suspend fun browse(type: MemoryType? = null, limit: Int = 50): List<Stored> =
        runCatching { repo.list(type?.name, limit, 0) }.getOrDefault(emptyList())
            .map { Stored(it.id, it.content, MemoryType.fromName(it.type), importanceOf(it.metadata), it.created_at) }

    /** How many memories are stored (optionally of one [type]); 0 on failure. */
    suspend fun count(type: MemoryType? = null): Int =
        runCatching { repo.count(type?.name) }.getOrDefault(0)

    /** Delete one memory by id; true if it was removed. Best-effort. */
    suspend fun forget(id: String): Boolean =
        runCatching { repo.delete(id); true }.getOrDefault(false)

    /** Wipe all stored memories; true on success. Best-effort. */
    suspend fun clearAll(): Boolean =
        runCatching { repo.clear(); true }.getOrDefault(false)

    /**
     * Record a disliked thing (rejection feedback) — routes through [PreferenceLearner]
     * so repeated dislikes blacklist the value, and persists it as a high-importance
     * PREFERENCE memory. Returns the memory id, or null if it couldn't be stored.
     */
    suspend fun learnDislike(category: String, value: String): String? {
        preferences.reject(preferenceKey(category, value))
        return remember(
            content = "User dislikes $category: $value",
            type = MemoryType.PREFERENCE,
            importance = 0.9f,
            metadata = mapOf("category" to category, "value" to value, "sentiment" to "negative"),
        )
    }

    /** True once the user has rejected this category/value enough times to blacklist it. */
    fun isDisliked(category: String, value: String): Boolean =
        preferences.isBlacklisted(preferenceKey(category, value))

    private fun importanceOf(metadata: String): Float =
        runCatching {
            json.parseToJsonElement(metadata).jsonObject["importance"]?.jsonPrimitive?.floatOrNull
        }.getOrNull() ?: 0.5f

    companion object {
        /** Stable blacklist key for a disliked category/value (case/space-insensitive). */
        fun preferenceKey(category: String, value: String): String =
            "${category.trim().lowercase()}:${value.trim().lowercase()}"
    }
}
