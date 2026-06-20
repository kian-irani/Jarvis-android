package com.kianirani.jarvis.core.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DS-WIN1 acceptance: leader election picks the highest score (ties by id), skips offline peers,
 * and pairing-token validation gates the join. Pure.
 */
class DesktopPairingTest {

    private fun peer(id: String, score: Double, online: Boolean = true) = Peer(id, "10.0.0.$id", 7799, score, online)

    @Test fun `highest score is elected leader`() {
        val leader = DesktopPairing.electLeader(listOf(peer("1", 0.4), peer("2", 0.9), peer("3", 0.6)))
        assertEquals("2", leader?.id)
    }

    @Test fun `ties break by id deterministically`() {
        val a = DesktopPairing.electLeader(listOf(peer("zeta", 0.5), peer("alpha", 0.5)))
        assertEquals("alpha", a?.id)
    }

    @Test fun `offline peers are skipped`() {
        val leader = DesktopPairing.electLeader(listOf(peer("1", 0.9, online = false), peer("2", 0.3)))
        assertEquals("2", leader?.id)
        assertNull(DesktopPairing.electLeader(listOf(peer("1", 0.9, online = false))))
    }

    @Test fun `leaderAddress formats host port`() {
        assertEquals("10.0.0.2:7799", DesktopPairing.leaderAddress(listOf(peer("1", 0.2), peer("2", 0.9))))
    }

    @Test fun `pairing validates exact non-blank token`() {
        assertTrue(DesktopPairing.isPaired("abc123", "abc123"))
        assertFalse(DesktopPairing.isPaired("abc123", "different"))
        assertFalse(DesktopPairing.isPaired("", "abc123"))
        assertFalse(DesktopPairing.isPaired(null, "abc123"))
    }

    @Test fun `pairing code is a short upper alphanumeric prefix`() {
        assertEquals("ABC123", DesktopPairing.pairingCode("abc123-def456-ghi"))
    }
}
