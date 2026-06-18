package com.kianirani.jarvis.brain.data

import com.kianirani.jarvis.brain.data.db.MemoryDao
import com.kianirani.jarvis.brain.data.db.MemoryEntity
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.sqrt

@Serializable
data class SearchHit(val id: String, val content: String, val score: Float)

/** Richer hit for [MemoryRepository.searchDetailed] — carries the fields the CF4 MemoryEngine ranks on. */
data class DetailedHit(
    val id: String,
    val content: String,
    val score: Float,
    val type: String,
    val metadata: String,
    val createdAt: Long,
)

class MemoryRepository(
    private val dao: MemoryDao,
    private val embed: (List<String>) -> List<FloatArray>,
) {
    fun toBlob(v: FloatArray): ByteArray {
        val buf = ByteBuffer.allocate(v.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        v.forEach { buf.putFloat(it) }
        return buf.array()
    }

    private fun fromBlob(b: ByteArray): FloatArray {
        val buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(b.size / 4) { buf.getFloat(it * 4) }
    }

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        var dot = 0f; var na = 0f; var nb = 0f
        for (i in a.indices) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i] }
        return if (na == 0f || nb == 0f) 0f else dot / (sqrt(na) * sqrt(nb))
    }

    suspend fun store(type: String, content: String, metadata: String): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val vec = embed(listOf(content)).first()
        dao.insert(MemoryEntity(id, type, content, metadata, toBlob(vec), now, now))
        return id
    }

    suspend fun list(type: String?, limit: Int, offset: Int) = dao.list(type, limit, offset)

    suspend fun count(type: String? = null): Int = dao.count(type)

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun clear() = dao.clear()

    suspend fun search(query: String, topK: Int): List<SearchHit> {
        val q = embed(listOf(query)).first()
        return dao.allWithEmbedding()
            .map { SearchHit(it.id, it.content, cosine(q, fromBlob(it.embedding!!))) }
            .sortedByDescending { it.score }
            .take(topK)
    }

    /** Like [search] but returns type/metadata/createdAt so callers can re-rank (CF4 MemoryEngine). */
    suspend fun searchDetailed(query: String, pool: Int): List<DetailedHit> {
        val q = embed(listOf(query)).first()
        return dao.allWithEmbedding()
            .map { DetailedHit(it.id, it.content, cosine(q, fromBlob(it.embedding!!)), it.type, it.metadata, it.created_at) }
            .sortedByDescending { it.score }
            .take(pool)
    }
}
