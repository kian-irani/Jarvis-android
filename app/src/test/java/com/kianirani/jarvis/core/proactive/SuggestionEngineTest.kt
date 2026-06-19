package com.kianirani.jarvis.core.proactive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PAS acceptance: the engine surfaces only relevant suggestions, respects per-kind cooldowns
 * and the global rate window (no nagging), shows at most one per kind, and ranks by relevance.
 * Pure, no device.
 */
class SuggestionEngineTest {

    private val now = 10_000_000L

    private fun sug(id: String, kind: String, rel: Float) = Suggestion(id, kind, id, rel, now)

    @Test fun `low-relevance candidates are dropped`() {
        val out = SuggestionEngine.select(
            listOf(sug("a", "reminder", 0.3f), sug("b", "writing", 0.8f)),
            now,
            policy = SuggestionPolicy(maxPerWindow = 5),
        )
        assertEquals(listOf("b"), out.map { it.id })
    }

    @Test fun `highest relevance wins and ties break by id`() {
        val out = SuggestionEngine.select(
            listOf(sug("z", "k1", 0.9f), sug("a", "k2", 0.9f), sug("m", "k3", 0.95f)),
            now,
            policy = SuggestionPolicy(maxPerWindow = 5),
        )
        assertEquals(listOf("m", "a", "z"), out.map { it.id })
    }

    @Test fun `a kind still in cooldown is suppressed`() {
        val out = SuggestionEngine.select(
            listOf(sug("a", "reminder", 0.9f), sug("b", "writing", 0.8f)),
            now,
            lastShownByKind = mapOf("reminder" to now - 5 * 60_000L), // 5m ago, cooldown 30m
            policy = SuggestionPolicy(maxPerWindow = 5, perKindCooldownMillis = 30 * 60_000L),
        )
        assertEquals(listOf("b"), out.map { it.id })
    }

    @Test fun `cooldown expires and the kind is allowed again`() {
        val out = SuggestionEngine.select(
            listOf(sug("a", "reminder", 0.9f)),
            now,
            lastShownByKind = mapOf("reminder" to now - 40 * 60_000L), // 40m ago > 30m
            policy = SuggestionPolicy(maxPerWindow = 5),
        )
        assertEquals(listOf("a"), out.map { it.id })
    }

    @Test fun `the rate window caps how many show at once`() {
        val out = SuggestionEngine.select(
            listOf(sug("a", "k1", 0.9f), sug("b", "k2", 0.8f), sug("c", "k3", 0.7f)),
            now,
            policy = SuggestionPolicy(maxPerWindow = 2),
        )
        assertEquals(listOf("a", "b"), out.map { it.id })
    }

    @Test fun `a full window suppresses everything (no nagging)`() {
        val out = SuggestionEngine.select(
            listOf(sug("a", "k1", 0.99f)),
            now,
            recentlyShownAtMillis = listOf(now - 10 * 60_000L), // already shown 1 in the hour
            policy = SuggestionPolicy(maxPerWindow = 1),
        )
        assertTrue(out.isEmpty())
    }

    @Test fun `at most one suggestion per kind in a single tick`() {
        val out = SuggestionEngine.select(
            listOf(sug("a", "reminder", 0.9f), sug("b", "reminder", 0.8f), sug("c", "writing", 0.85f)),
            now,
            policy = SuggestionPolicy(maxPerWindow = 5),
        )
        assertEquals(listOf("a", "c"), out.map { it.id }) // only the stronger reminder
    }
}
