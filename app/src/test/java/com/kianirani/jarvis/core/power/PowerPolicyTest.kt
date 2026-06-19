package com.kianirani.jarvis.core.power

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DS-W6 acceptance: power mode tracks battery/charging/memory, idle and conserve collapse the
 * orb, the wake word drops only in MINIMAL, and animations degrade under pressure. Pure.
 */
class PowerPolicyTest {

    @Test fun `charging keeps full mode even on low battery`() {
        assertEquals(PowerMode.FULL, PowerPolicy.mode(PowerState(batteryPercent = 5, charging = true)))
    }

    @Test fun `low battery unplugged conserves, critical goes minimal`() {
        assertEquals(PowerMode.CONSERVE, PowerPolicy.mode(PowerState(25, charging = false)))
        assertEquals(PowerMode.MINIMAL, PowerPolicy.mode(PowerState(10, charging = false)))
        assertEquals(PowerMode.FULL, PowerPolicy.mode(PowerState(80, charging = false)))
    }

    @Test fun `memory pressure forces minimal even while charging`() {
        assertEquals(PowerMode.MINIMAL, PowerPolicy.mode(PowerState(90, charging = true, lowMemory = true)))
    }

    @Test fun `collapses to dot when idle past the threshold`() {
        val active = PowerState(90, charging = true, idleMillis = 1_000)
        val idle = PowerState(90, charging = true, idleMillis = 9_000)
        assertFalse(PowerPolicy.shouldCollapseToDot(active))
        assertTrue(PowerPolicy.shouldCollapseToDot(idle))
    }

    @Test fun `collapses whenever not in full mode`() {
        assertTrue(PowerPolicy.shouldCollapseToDot(PowerState(10, charging = false, idleMillis = 0)))
    }

    @Test fun `wake word is on except in minimal`() {
        assertTrue(PowerPolicy.wakeWordEnabled(PowerState(80, charging = false)))
        assertTrue(PowerPolicy.wakeWordEnabled(PowerState(25, charging = false))) // conserve still listens
        assertFalse(PowerPolicy.wakeWordEnabled(PowerState(10, charging = false))) // minimal
    }

    @Test fun `animations degrade under memory pressure or minimal`() {
        assertTrue(PowerPolicy.degradeAnimations(PowerState(90, charging = true, lowMemory = true)))
        assertTrue(PowerPolicy.degradeAnimations(PowerState(10, charging = false)))
        assertFalse(PowerPolicy.degradeAnimations(PowerState(80, charging = false)))
    }
}
