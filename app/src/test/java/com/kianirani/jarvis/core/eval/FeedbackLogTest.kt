package com.kianirani.jarvis.core.eval

import com.kianirani.jarvis.core.eval.FeedbackLog.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** CF6 — per-turn feedback log: record, rate, stats, ring-buffer bound. */
class FeedbackLogTest {

    private fun log() = FeedbackLog()

    private fun FeedbackLog.add(success: Boolean = true) =
        record("hi", "CHAT", "qwen", "LOCAL", latencyMs = 12, success = success, now = 0L)

    @Test
    fun `records appear newest-first and report stats`() {
        val l = log()
        l.add(); l.add(success = false); l.add()
        val s = l.stats()
        assertEquals(3, s.total)
        assertEquals(2, s.successes)
        assertEquals(2.0 / 3.0, s.successRate, 1e-9)
        assertEquals(3L, l.recent.value.first().id) // newest first
    }

    @Test
    fun `rating a known turn updates thumbs counts and unknown id is a no-op`() {
        val l = log()
        val id = l.add()
        assertTrue(l.rate(id, Rating.UP))
        assertEquals(1, l.stats().thumbsUp)
        assertTrue(l.rate(id, Rating.DOWN)) // change of mind
        assertEquals(0, l.stats().thumbsUp)
        assertEquals(1, l.stats().thumbsDown)
        assertFalse(l.rate(9999, Rating.UP))
    }

    @Test
    fun `buffer is bounded to capacity, dropping the oldest`() {
        val l = log()
        repeat(250) { l.add() }
        assertEquals(200, l.stats().total)
        // ids 51..250 survive; newest is 250, oldest kept is 51.
        assertEquals(250L, l.recent.value.first().id)
        assertEquals(51L, l.recent.value.last().id)
    }

    @Test
    fun `empty log has a zero success rate and clear empties it`() {
        val l = log()
        assertEquals(0.0, l.stats().successRate, 0.0)
        l.add()
        l.clear()
        assertEquals(0, l.stats().total)
        assertTrue(l.recent.value.isEmpty())
    }
}
