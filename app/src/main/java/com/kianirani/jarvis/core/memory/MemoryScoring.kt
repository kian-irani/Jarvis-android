package com.kianirani.jarvis.core.memory

import kotlin.math.pow

/**
 * CF4 — pure ranking math for recall (PRD Part 4). A memory's rank combines how
 * well it matches the query (cosine similarity), how important it is, and how
 * recently it was created (exponential recency decay). No Android deps → unit-tested.
 */
object MemoryScoring {
    /** Memories lose half their recency weight every two weeks by default. */
    const val DEFAULT_HALF_LIFE_MS = 14L * 24 * 60 * 60 * 1000

    /** 1.0 for a brand-new memory, halving every [halfLifeMillis]; never negative-age. */
    fun recencyDecay(ageMillis: Long, halfLifeMillis: Long = DEFAULT_HALF_LIFE_MS): Float {
        if (halfLifeMillis <= 0L) return 1f
        val age = ageMillis.coerceAtLeast(0L)
        return 0.5.pow(age.toDouble() / halfLifeMillis).toFloat()
    }

    /** Maps importance 0..1 onto a 0.5..1.0 multiplier so low-importance memories still surface. */
    fun importanceWeight(importance: Float): Float = 0.5f + 0.5f * importance.coerceIn(0f, 1f)

    /** Combined recall rank — higher is better. */
    fun rankScore(
        similarity: Float,
        importance: Float,
        ageMillis: Long,
        halfLifeMillis: Long = DEFAULT_HALF_LIFE_MS,
    ): Float = similarity.coerceIn(0f, 1f) * importanceWeight(importance) * recencyDecay(ageMillis, halfLifeMillis)
}
