package com.kianirani.jarvis.core.memory

import java.util.concurrent.ConcurrentHashMap

/**
 * CF4 — learns what the user keeps rejecting (PRD Part 4). After [threshold]
 * rejections of the same key (e.g. a UI style), it is blacklisted so Vision stops
 * suggesting it. Pure in-memory counting → unit-tested; persistence (as a
 * PREFERENCE memory) is layered on top by [MemoryEngine].
 */
class PreferenceLearner(private val threshold: Int = 3) {
    private val counts = ConcurrentHashMap<String, Int>()

    /** Record a rejection of [key]; returns true once it reaches the blacklist threshold. */
    fun reject(key: String): Boolean = counts.merge(key, 1, Int::plus)!! >= threshold

    fun isBlacklisted(key: String): Boolean = rejections(key) >= threshold

    fun rejections(key: String): Int = counts[key] ?: 0

    fun reset(key: String) { counts.remove(key) }
}
