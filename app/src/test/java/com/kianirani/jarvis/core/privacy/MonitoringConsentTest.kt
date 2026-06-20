package com.kianirani.jarvis.core.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** MON3 acceptance: off by default, master kill-switch, per-source opt-in, transparent active list. Pure. */
class MonitoringConsentTest {

    @Test fun `nothing is monitored by default`() {
        val c = MonitoringConsent()
        MonitorSource.entries.forEach { assertFalse(c.canMonitor(it)) }
    }

    @Test fun `a source needs both master on and per-source grant`() {
        val granted = MonitoringConsent().grant(MonitorSource.USAGE)
        assertFalse(granted.canMonitor(MonitorSource.USAGE)) // master still off
        val on = granted.copy(masterEnabled = true)
        assertTrue(on.canMonitor(MonitorSource.USAGE))
        assertFalse(on.canMonitor(MonitorSource.SMS)) // not granted
    }

    @Test fun `master kill-switch disables everything`() {
        val c = MonitoringConsent(masterEnabled = true, allowed = setOf(MonitorSource.NOTIFICATIONS, MonitorSource.USAGE))
        assertTrue(c.canMonitor(MonitorSource.USAGE))
        val off = c.copy(masterEnabled = false)
        assertFalse(off.canMonitor(MonitorSource.USAGE))
        assertTrue(off.activeSources().isEmpty())
    }

    @Test fun `revoke removes a source`() {
        val c = MonitoringConsent(masterEnabled = true).grant(MonitorSource.SCREEN).revoke(MonitorSource.SCREEN)
        assertFalse(c.canMonitor(MonitorSource.SCREEN))
    }

    @Test fun `activeSources reflects granted while master on`() {
        val c = MonitoringConsent(masterEnabled = true, allowed = setOf(MonitorSource.CALL_LOG))
        assertEquals(setOf(MonitorSource.CALL_LOG), c.activeSources())
    }
}
