package com.kianirani.jarvis.core.event

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerMatcherTest {

    @Test fun `matches triggers by event kind`() {
        val triggers = listOf(
            Trigger("t1", EventKind.WAKE_WORD, "greet"),
            Trigger("t2", EventKind.APP_OPENED, "context"),
        )
        assertEquals(listOf("t1"), TriggerMatcher.matching(triggers, VisionEvent.WakeWord).map { it.id })
        assertEquals(listOf("t2"), TriggerMatcher.matching(triggers, VisionEvent.AppOpened("com.x")).map { it.id })
    }

    @Test fun `respects a condition`() {
        val triggers = listOf(
            Trigger("maps", EventKind.APP_OPENED, "navHelp") { (it as VisionEvent.AppOpened).packageName == "com.maps" },
        )
        assertEquals(listOf("maps"), TriggerMatcher.matching(triggers, VisionEvent.AppOpened("com.maps")).map { it.id })
        assertTrue(TriggerMatcher.matching(triggers, VisionEvent.AppOpened("com.other")).isEmpty())
    }

    @Test fun `no trigger fires for an unmatched kind`() {
        val triggers = listOf(Trigger("t1", EventKind.SCHEDULED, "x"))
        assertTrue(TriggerMatcher.matching(triggers, VisionEvent.WakeWord).isEmpty())
    }

    @Test fun `every event exposes its kind`() {
        assertEquals(EventKind.WAKE_WORD, VisionEvent.WakeWord.kind)
        assertEquals(EventKind.APP_OPENED, VisionEvent.AppOpened("x").kind)
        assertEquals(EventKind.USER_IDLE, VisionEvent.UserIdle(1L).kind)
        assertEquals(EventKind.SCHEDULED, VisionEvent.Scheduled("s").kind)
        assertEquals(EventKind.CUSTOM, VisionEvent.Custom("c").kind)
    }
}
