package com.kianirani.jarvis.core.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** C2 acceptance: op parsing (EN/FA), risk rating, confirmation gate. Pure. */
class ServerControlTest {

    @Test fun `parses ops from natural language`() {
        assertEquals(ServerOp.RESTART, ServerControl.opFor("restart the frankfurt server"))
        assertEquals(ServerOp.RESTART, ServerControl.opFor("سرور رو ری‌استارت کن"))
        assertEquals(ServerOp.STATUS, ServerControl.opFor("what's the status"))
        assertEquals(ServerOp.REMOVE, ServerControl.opFor("delete the container"))
        assertEquals(ServerOp.UNKNOWN, ServerControl.opFor("sing a song"))
    }

    @Test fun `risk rating`() {
        assertEquals(ServerRisk.SAFE, ServerControl.riskOf(ServerOp.STATUS))
        assertEquals(ServerRisk.CONFIRM, ServerControl.riskOf(ServerOp.RESTART))
        assertEquals(ServerRisk.CRITICAL, ServerControl.riskOf(ServerOp.REMOVE))
    }

    @Test fun `parse builds a risk-rated command`() {
        val cmd = ServerControl.parse("reboot it", "frankfurt")
        assertEquals(ServerOp.RESTART, cmd.op)
        assertEquals("frankfurt", cmd.target)
        assertEquals(ServerRisk.CONFIRM, cmd.risk)
    }

    @Test fun `destructive and mutating need confirmation, status does not`() {
        assertTrue(ServerControl.requiresConfirmation(ServerControl.parse("remove", "x")))
        assertTrue(ServerControl.requiresConfirmation(ServerControl.parse("restart", "x")))
        assertFalse(ServerControl.requiresConfirmation(ServerControl.parse("status", "x")))
    }
}
