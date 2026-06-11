package com.kianirani.jarvis.brain.score

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrainScoreCalculatorTest {

    private val vps = DeviceMetrics(ramFreeGb = 8.0, cpuCores = 4, isVps = true, batteryPercent = 100, networkMbps = 100.0)
    private val phone = DeviceMetrics(ramFreeGb = 2.0, cpuCores = 8, batteryPercent = 50, networkMbps = 20.0, isOnBattery = true)

    @Test
    fun `score follows roadmap formula`() {
        // 8*20 + 4*10 + 50 + 100*0.3 + 100*0.1 = 160+40+50+30+10 = 290
        assertEquals(290, BrainScoreCalculator.score(vps))
        // 2*20 + 8*10 + 0 + 15 + 2 - 20 = 117
        assertEquals(117, BrainScoreCalculator.score(phone))
    }

    @Test
    fun `thermal throttling and battery penalties apply`() {
        val throttled = phone.copy(thermalThrottling = true)
        assertEquals(BrainScoreCalculator.score(phone) - 30, BrainScoreCalculator.score(throttled))
    }

    @Test
    fun `score never negative`() {
        val dead = DeviceMetrics(ramFreeGb = 0.0, cpuCores = 0, batteryPercent = 0, isOnBattery = true, thermalThrottling = true)
        assertEquals(0, BrainScoreCalculator.score(dead))
    }

    @Test
    fun `elects highest scoring node`() {
        assertEquals("vps", BrainScoreCalculator.elect(mapOf("vps" to vps, "phone" to phone)))
    }

    @Test
    fun `manual override wins when present`() {
        assertEquals("phone", BrainScoreCalculator.elect(mapOf("vps" to vps, "phone" to phone), manualOverride = "phone"))
        assertEquals("vps", BrainScoreCalculator.elect(mapOf("vps" to vps), manualOverride = "ghost"))
    }

    @Test
    fun `tie breaks deterministically by id`() {
        assertEquals("a", BrainScoreCalculator.elect(mapOf("b" to phone, "a" to phone)))
    }

    @Test
    fun `failover removes failed node and re-elects`() {
        assertEquals("phone", BrainScoreCalculator.failover(mapOf("vps" to vps, "phone" to phone), failedId = "vps"))
        assertNull(BrainScoreCalculator.failover(mapOf("vps" to vps), failedId = "vps"))
    }
}
