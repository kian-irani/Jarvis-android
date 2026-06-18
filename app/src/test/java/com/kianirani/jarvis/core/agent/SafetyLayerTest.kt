package com.kianirani.jarvis.core.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyLayerTest {

    @Test fun `sensitive actions are critical and need confirmation`() {
        for (a in listOf("send_sms", "call", "send_email", "delete", "purchase", "transfer")) {
            assertEquals(a, ActionRisk.CRITICAL, SafetyLayer.riskOf(a))
            assertTrue(a, SafetyLayer.requiresConfirmation(a))
        }
    }

    @Test fun `safe-list actions are auto and skip confirmation`() {
        for (a in listOf("get_time", "get_battery", "open_app", "flashlight")) {
            assertEquals(a, ActionRisk.AUTO, SafetyLayer.riskOf(a))
            assertFalse(a, SafetyLayer.requiresConfirmation(a))
        }
    }

    @Test fun `unknown action defaults to confirm`() {
        assertEquals(ActionRisk.CONFIRM, SafetyLayer.riskOf("frobnicate"))
        assertTrue(SafetyLayer.requiresConfirmation("frobnicate"))
    }

    @Test fun `declared auto lets an unknown action run automatically`() {
        assertEquals(ActionRisk.AUTO, SafetyLayer.riskOf("frobnicate", ActionRisk.AUTO))
        assertFalse(SafetyLayer.requiresConfirmation("frobnicate", ActionRisk.AUTO))
    }

    @Test fun `always-critical wins over a declared auto`() {
        assertEquals(ActionRisk.CRITICAL, SafetyLayer.riskOf("send_sms", ActionRisk.AUTO))
        assertTrue(SafetyLayer.requiresConfirmation("send_sms", ActionRisk.AUTO))
    }

    @Test fun `declared critical escalates an otherwise-auto action`() {
        assertEquals(ActionRisk.CRITICAL, SafetyLayer.riskOf("get_battery", ActionRisk.CRITICAL))
        assertTrue(SafetyLayer.requiresConfirmation("get_battery", ActionRisk.CRITICAL))
    }

    @Test fun `action name is case and whitespace insensitive`() {
        assertEquals(ActionRisk.CRITICAL, SafetyLayer.riskOf("  Send_SMS "))
        assertEquals(ActionRisk.AUTO, SafetyLayer.riskOf("GET_TIME"))
    }

    @Test fun `anti-hallucination rules are present`() {
        assertTrue(SafetyLayer.ANTI_HALLUCINATION_RULES.isNotBlank())
        assertTrue(SafetyLayer.ANTI_HALLUCINATION_RULES.contains("Never claim"))
    }
}
