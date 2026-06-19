package com.kianirani.jarvis.core.context

import com.kianirani.jarvis.core.protocol.DeviceContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DS-B2 acceptance: ContextEngine formats a DeviceContext into a grounded prompt fragment,
 * skips empty/zero fields, clamps battery, and never crashes. Pure, no device.
 */
class ContextEngineTest {

    @Test fun `null context yields empty fragment`() {
        assertEquals("", ContextEngine.buildContextBlock(null))
    }

    @Test fun `empty context yields empty fragment`() {
        assertEquals("", ContextEngine.buildContextBlock(DeviceContext()))
    }

    @Test fun `populated context renders a CONTEXT block with bullets`() {
        val block = ContextEngine.buildContextBlock(
            DeviceContext(
                foregroundApp = "com.google.maps",
                batteryPercent = 18,
                charging = false,
                network = "cellular",
                timeOfDay = "evening",
            ),
        )
        assertTrue(block.startsWith("\n\n[CONTEXT"))
        assertTrue(block.endsWith("[/CONTEXT]"))
        assertTrue(block.contains("• Foreground app: com.google.maps"))
        assertTrue(block.contains("• Battery: 18%"))
        assertTrue(block.contains("• Network: cellular"))
        assertTrue(block.contains("• Time of day: evening"))
    }

    @Test fun `charging battery is annotated`() {
        val block = ContextEngine.buildContextBlock(DeviceContext(batteryPercent = 90, charging = true))
        assertTrue(block.contains("• Battery: 90%, charging"))
    }

    @Test fun `battery is clamped into 0 to 100`() {
        assertTrue(ContextEngine.buildContextBlock(DeviceContext(batteryPercent = 150)).contains("Battery: 100%"))
        assertTrue(ContextEngine.buildContextBlock(DeviceContext(batteryPercent = -5)).contains("Battery: 0%"))
    }

    @Test fun `zero unread notifications and blank fields are skipped`() {
        val block = ContextEngine.buildContextBlock(
            DeviceContext(foregroundApp = "  ", network = "", unreadNotifications = 0, locale = "en-US"),
        )
        assertFalse(block.contains("Foreground app"))
        assertFalse(block.contains("Network"))
        assertFalse(block.contains("Unread"))
        assertTrue(block.contains("• Locale: en-US"))
    }

    @Test fun `extras are appended as their own bullets`() {
        val block = ContextEngine.buildContextBlock(
            DeviceContext(extras = mapOf("Calendar" to "meeting at 5pm", "blank" to "")),
        )
        assertTrue(block.contains("• Calendar: meeting at 5pm"))
        assertFalse(block.contains("blank"))
    }
}
