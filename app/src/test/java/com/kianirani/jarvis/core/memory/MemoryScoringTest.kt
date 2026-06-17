package com.kianirani.jarvis.core.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryScoringTest {

    private val halfLife = MemoryScoring.DEFAULT_HALF_LIFE_MS

    @Test fun `recency decay is 1 at zero age`() {
        assertEquals(1f, MemoryScoring.recencyDecay(0L), 1e-4f)
    }

    @Test fun `recency decay is half at one half-life`() {
        assertEquals(0.5f, MemoryScoring.recencyDecay(halfLife, halfLife), 1e-3f)
    }

    @Test fun `recency decay treats negative age as fresh`() {
        assertEquals(1f, MemoryScoring.recencyDecay(-5_000L), 1e-4f)
    }

    @Test fun `importance weight maps 0_1 into 0_5 to 1_0 and clamps`() {
        assertEquals(0.5f, MemoryScoring.importanceWeight(0f), 1e-4f)
        assertEquals(1.0f, MemoryScoring.importanceWeight(1f), 1e-4f)
        assertEquals(1.0f, MemoryScoring.importanceWeight(5f), 1e-4f) // clamps above 1
        assertEquals(0.5f, MemoryScoring.importanceWeight(-2f), 1e-4f) // clamps below 0
    }

    @Test fun `rank score rises with similarity`() {
        val hi = MemoryScoring.rankScore(0.9f, importance = 0.5f, ageMillis = 0L)
        val lo = MemoryScoring.rankScore(0.3f, importance = 0.5f, ageMillis = 0L)
        assertTrue(hi > lo)
    }

    @Test fun `rank score rises with importance`() {
        val hi = MemoryScoring.rankScore(0.6f, importance = 0.9f, ageMillis = 0L)
        val lo = MemoryScoring.rankScore(0.6f, importance = 0.1f, ageMillis = 0L)
        assertTrue(hi > lo)
    }

    @Test fun `rank score falls as memory ages`() {
        val fresh = MemoryScoring.rankScore(0.6f, importance = 0.5f, ageMillis = 0L)
        val old = MemoryScoring.rankScore(0.6f, importance = 0.5f, ageMillis = 2 * halfLife)
        assertTrue(old < fresh)
    }
}
