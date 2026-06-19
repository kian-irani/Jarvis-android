package com.kianirani.jarvis.core.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LM3 acceptance: tiers classify by RAM+cores, the low-memory warning trips below the
 * threshold, and model selection picks the biggest model that fits (or null). Pure.
 */
class DeviceTierSelectorTest {

    private data class Model(val id: String, val minRamGb: Float)
    private val models = listOf(Model("nano", 1f), Model("lite", 3f), Model("full", 6f))

    @Test fun `high-end device is FULL`() {
        assertEquals(DeviceTier.FULL, DeviceTierSelector.tierFor(ramGb = 8f, cpuCores = 8))
    }

    @Test fun `mid device is LITE`() {
        assertEquals(DeviceTier.LITE, DeviceTierSelector.tierFor(ramGb = 4f, cpuCores = 6))
    }

    @Test fun `low-ram or few-core device is NANO`() {
        assertEquals(DeviceTier.NANO, DeviceTierSelector.tierFor(ramGb = 2f, cpuCores = 8))
        assertEquals(DeviceTier.NANO, DeviceTierSelector.tierFor(ramGb = 8f, cpuCores = 2))
    }

    @Test fun `low-memory warning trips below the threshold`() {
        assertTrue(DeviceTierSelector.warnLowMemory(2.5f))
        assertFalse(DeviceTierSelector.warnLowMemory(4f))
    }

    @Test fun `selectModel picks the largest model that fits`() {
        assertEquals("full", DeviceTierSelector.selectModel(8f, models) { it.minRamGb }?.id)
        assertEquals("lite", DeviceTierSelector.selectModel(4f, models) { it.minRamGb }?.id)
        assertEquals("nano", DeviceTierSelector.selectModel(2f, models) { it.minRamGb }?.id)
    }

    @Test fun `selectModel returns null when nothing fits`() {
        assertNull(DeviceTierSelector.selectModel(0.5f, models) { it.minRamGb })
    }
}
