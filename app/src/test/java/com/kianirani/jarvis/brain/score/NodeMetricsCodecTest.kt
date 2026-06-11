package com.kianirani.jarvis.brain.score

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NodeMetricsCodecTest {

    @Test fun `encode-decode round trip preserves all fields`() {
        val m = DeviceMetrics(
            ramFreeGb = 7.5, cpuCores = 12, isVps = true, batteryPercent = 80,
            networkMbps = 250.0, isOnBattery = true, thermalThrottling = true,
        )
        assertEquals(m, NodeMetricsCodec.decode(NodeMetricsCodec.encode(m)))
    }

    @Test fun `decode applies defaults for missing optional keys`() {
        val m = NodeMetricsCodec.decode("""{"ram_free_gb":2.0,"cpu_cores":4}""")
        assertEquals(DeviceMetrics(ramFreeGb = 2.0, cpuCores = 4), m)
    }

    @Test fun `decode tolerates unknown keys`() {
        val m = NodeMetricsCodec.decode("""{"ram_free_gb":1.0,"cpu_cores":2,"future_field":"x"}""")
        assertEquals(2, m?.cpuCores)
    }

    @Test fun `decode returns null for non-metrics or invalid json`() {
        assertNull(NodeMetricsCodec.decode("{}"))
        assertNull(NodeMetricsCodec.decode("""{"foo":1}"""))
        assertNull(NodeMetricsCodec.decode("not json"))
        assertNull(NodeMetricsCodec.decode(""))
    }
}
