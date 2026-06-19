package com.kianirani.jarvis.core.flags

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DS-X2 acceptance: unknown flags fail closed, channel overrides win over the default, rollout
 * gates by a stable per-install bucket (0% off / 100% on / partial reproducible and roughly
 * proportional). Pure, no device.
 */
class FeatureFlagsTest {

    private fun flags(vararg f: FeatureFlag, channel: ReleaseChannel = ReleaseChannel.PROD, id: String = "device-1") =
        FeatureFlags(f.toList(), channel, id)

    @Test fun `unknown flags are off`() {
        assertFalse(flags().isEnabled("nope"))
    }

    @Test fun `default value applies when no override`() {
        assertTrue(flags(FeatureFlag("a", default = true)).isEnabled("a"))
        assertFalse(flags(FeatureFlag("b", default = false)).isEnabled("b"))
    }

    @Test fun `channel override beats the default`() {
        val flag = FeatureFlag("x", default = false, channelOverrides = mapOf(ReleaseChannel.BETA to true))
        assertTrue(flags(flag, channel = ReleaseChannel.BETA).isEnabled("x"))
        assertFalse(flags(flag, channel = ReleaseChannel.PROD).isEnabled("x"))
    }

    @Test fun `zero percent rollout is off even when enabled`() {
        assertFalse(flags(FeatureFlag("r", default = true, rolloutPercent = 0)).isEnabled("r"))
    }

    @Test fun `full rollout is on`() {
        assertTrue(flags(FeatureFlag("r", default = true, rolloutPercent = 100)).isEnabled("r"))
    }

    @Test fun `rollout is deterministic for a given install`() {
        val flag = FeatureFlag("r", default = true, rolloutPercent = 50)
        val a = FeatureFlags(listOf(flag), ReleaseChannel.PROD, "device-42")
        val b = FeatureFlags(listOf(flag), ReleaseChannel.PROD, "device-42")
        assertEquals(a.isEnabled("r"), b.isEnabled("r"))
        assertEquals(a.isEnabled("r"), a.isEnabled("r")) // stable across calls
    }

    @Test fun `partial rollout is roughly proportional across installs`() {
        val flag = FeatureFlag("r", default = true, rolloutPercent = 30)
        val enabled = (0 until 2000).count {
            FeatureFlags(listOf(flag), ReleaseChannel.PROD, "install-$it").isEnabled("r")
        }
        // 30% of 2000 = 600; allow generous tolerance for the hash distribution
        assertTrue("got $enabled", enabled in 450..750)
    }

    @Test fun `enabledKeys lists only the on flags`() {
        val ff = flags(
            FeatureFlag("on", default = true),
            FeatureFlag("off", default = false),
            FeatureFlag("dark", default = true, rolloutPercent = 0),
        )
        assertEquals(setOf("on"), ff.enabledKeys())
    }
}
