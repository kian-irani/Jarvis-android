package com.kianirani.jarvis.core.mesh

import org.junit.Assert.assertEquals
import org.junit.Test

/** Presence acceptance: merge keeps freshest per device, staleness flips to OFFLINE. Pure. */
class DevicePresenceTest {

    private val now = 1_000_000L

    @Test fun `merge keeps the newest announcement per device`() {
        val current = listOf(Presence("phone", PresenceStatus.ONLINE, now - 5000))
        val incoming = listOf(
            Presence("phone", PresenceStatus.BUSY, now), // newer → wins
            Presence("desktop", PresenceStatus.ONLINE, now),
        )
        val merged = DevicePresence.merge(current, incoming)
        assertEquals(PresenceStatus.BUSY, merged.first { it.deviceId == "phone" }.status)
        assertEquals(2, merged.size)
    }

    @Test fun `merge is order-independent`() {
        val a = listOf(Presence("p", PresenceStatus.ONLINE, 1))
        val b = listOf(Presence("p", PresenceStatus.AWAY, 2))
        assertEquals(DevicePresence.merge(a, b), DevicePresence.merge(b, a))
    }

    @Test fun `a stale device is effectively offline`() {
        val fresh = Presence("p", PresenceStatus.ONLINE, now - 10_000)
        val stale = Presence("q", PresenceStatus.ONLINE, now - 200_000)
        assertEquals(PresenceStatus.ONLINE, DevicePresence.effectiveStatus(fresh, now))
        assertEquals(PresenceStatus.OFFLINE, DevicePresence.effectiveStatus(stale, now))
    }

    @Test fun `onlineDevices excludes stale`() {
        val presences = listOf(
            Presence("a", PresenceStatus.ONLINE, now - 1000),
            Presence("b", PresenceStatus.ONLINE, now - 300_000),
        )
        assertEquals(listOf("a"), DevicePresence.onlineDevices(presences, now))
    }
}
