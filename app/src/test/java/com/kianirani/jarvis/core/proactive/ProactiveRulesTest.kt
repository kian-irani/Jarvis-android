package com.kianirani.jarvis.core.proactive

import com.kianirani.jarvis.core.event.VisionEvent
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** C0.3 acceptance: events map to suggestion candidates (or null). Pure. */
class ProactiveRulesTest {

    @Test fun `important notification yields a handle suggestion`() {
        val s = ProactiveRules.suggestionFor(VisionEvent.Custom("notification_important", "com.wa"), now = 1)
        assertTrue(s != null && s.kind == "notification")
        assertTrue(s!!.text.contains("com.wa"))
    }

    @Test fun `opening a browser yields a summarize suggestion`() {
        val s = ProactiveRules.suggestionFor(VisionEvent.AppOpened("com.android.chrome"), now = 1)
        assertTrue(s != null && s.text.contains("summarize"))
    }

    @Test fun `non-browser app and unrelated events yield nothing`() {
        assertNull(ProactiveRules.suggestionFor(VisionEvent.AppOpened("com.maps"), now = 1))
        assertNull(ProactiveRules.suggestionFor(VisionEvent.WakeWord, now = 1))
        assertNull(ProactiveRules.suggestionFor(VisionEvent.Custom("other", "x"), now = 1))
    }
}
