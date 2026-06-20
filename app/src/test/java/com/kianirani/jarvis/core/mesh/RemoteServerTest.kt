package com.kianirani.jarvis.core.mesh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** W4/W6 acceptance: remote-server validation + token-rotation age check. Pure. */
class RemoteServerTest {

    @Test fun `valid server passes`() {
        assertTrue(RemoteServerValidator.isValid(RemoteServer("10.0.0.5", 7799, "tok")))
        assertTrue(RemoteServerValidator.isValid(RemoteServer("brain.example.com", 443, "tok")))
    }

    @Test fun `invalid host port token are flagged`() {
        assertTrue(RemoteServerValidator.validate(RemoteServer("has space", 7799, "t")).contains("invalid host"))
        assertTrue(RemoteServerValidator.validate(RemoteServer("h", 70000, "t")).contains("port out of range"))
        assertTrue(RemoteServerValidator.validate(RemoteServer("h", 7799, "")).contains("token is required"))
    }

    @Test fun `address formats host port`() {
        assertEquals("10.0.0.5:7799", RemoteServerValidator.address(RemoteServer("10.0.0.5", 7799, "t")))
    }

    @Test fun `token rotates after max age`() {
        val issued = 1_000_000L
        val day = 24L * 60 * 60 * 1000
        assertFalse(TokenRotation.shouldRotate(issued, issued + 3 * day, maxAgeMillis = 7 * day))
        assertTrue(TokenRotation.shouldRotate(issued, issued + 8 * day, maxAgeMillis = 7 * day))
        assertEquals(4 * day, TokenRotation.remainingMillis(issued, issued + 3 * day, maxAgeMillis = 7 * day))
    }
}
