package com.kianirani.jarvis.router.health

import org.junit.Assert.assertEquals
import org.junit.Test

/** VB9.1 acceptance: health status maps key/circuit state; key summary reads right. Pure. */
class ProviderHealthTest {

    @Test fun `no keys is unconfigured`() {
        assertEquals(HealthStatus.UNCONFIGURED, ProviderHealth.status(ProviderHealthInput(totalKeys = 0, coolingKeys = 0)))
    }

    @Test fun `all keys healthy is online`() {
        assertEquals(HealthStatus.HEALTHY, ProviderHealth.status(ProviderHealthInput(totalKeys = 4, coolingKeys = 0)))
    }

    @Test fun `some cooling is degraded`() {
        assertEquals(HealthStatus.DEGRADED, ProviderHealth.status(ProviderHealthInput(totalKeys = 4, coolingKeys = 2)))
    }

    @Test fun `all cooling or circuit open is down`() {
        assertEquals(HealthStatus.DOWN, ProviderHealth.status(ProviderHealthInput(totalKeys = 3, coolingKeys = 3)))
        assertEquals(HealthStatus.DOWN, ProviderHealth.status(ProviderHealthInput(totalKeys = 3, coolingKeys = 0, circuitOpen = true)))
    }

    @Test fun `key summary counts ready keys`() {
        assertEquals("2/4 keys ready", ProviderHealth.keySummary(ProviderHealthInput(totalKeys = 4, coolingKeys = 2)))
        assertEquals("no keys", ProviderHealth.keySummary(ProviderHealthInput(totalKeys = 0, coolingKeys = 0)))
    }
}
