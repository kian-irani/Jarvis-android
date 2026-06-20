package com.kianirani.jarvis.core.mesh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** W1 acceptance: loopback by default, all-interfaces+token+warn when remote enabled. Pure. */
class BindPolicyTest {

    @Test fun `default is loopback only, no token, no warning`() {
        val d = BindPolicy.decide(acceptRemoteNodes = false, hasToken = false)
        assertEquals(BindPolicy.LOOPBACK, d.address)
        assertFalse(d.requireToken)
        assertFalse(d.warn)
    }

    @Test fun `remote on binds all interfaces, requires token, warns`() {
        val d = BindPolicy.decide(acceptRemoteNodes = true, hasToken = true)
        assertEquals(BindPolicy.ALL_INTERFACES, d.address)
        assertTrue(d.requireToken)
        assertTrue(d.warn)
    }

    @Test fun `canExpose only when remote on and a token exists`() {
        assertTrue(BindPolicy.canExpose(acceptRemoteNodes = true, hasToken = true))
        assertFalse(BindPolicy.canExpose(acceptRemoteNodes = true, hasToken = false))
        assertFalse(BindPolicy.canExpose(acceptRemoteNodes = false, hasToken = true))
    }
}
