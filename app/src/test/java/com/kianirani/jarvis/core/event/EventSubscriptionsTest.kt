package com.kianirani.jarvis.core.event

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DS-B5 acceptance: the subscriber registry dispatches by kind / to-all, in registration
 * order, replaces by id, unsubscribes, and isolates a throwing handler. Pure, no device.
 */
class EventSubscriptionsTest {

    @Test fun `kind-scoped subscriber only fires for its kind`() {
        val subs = EventSubscriptions()
        val seen = mutableListOf<VisionEvent>()
        subs.on(EventKind.COMMAND_RECEIVED, "cmd") { seen += it }
        subs.dispatch(VisionEvent.AppOpened("com.foo"))
        assertTrue(seen.isEmpty())
        subs.dispatch(VisionEvent.CommandReceived("hi"))
        assertEquals(1, seen.size)
        assertEquals(VisionEvent.CommandReceived("hi"), seen.single())
    }

    @Test fun `onAny fires for every kind`() {
        val subs = EventSubscriptions()
        var count = 0
        subs.onAny("logger") { count++ }
        subs.dispatch(VisionEvent.WakeWord)
        subs.dispatch(VisionEvent.ContextChanged("foregroundApp", "com.bar"))
        assertEquals(2, count)
    }

    @Test fun `dispatch returns firing ids in registration order`() {
        val subs = EventSubscriptions()
        subs.onAny("a") {}
        subs.on(EventKind.CONTEXT_CHANGED, "b") {}
        subs.onAny("c") {}
        assertEquals(listOf("a", "b", "c"), subs.dispatch(VisionEvent.ContextChanged("k")))
        assertEquals(listOf("a", "c"), subs.dispatch(VisionEvent.WakeWord))
    }

    @Test fun `re-registering an id replaces the old handler`() {
        val subs = EventSubscriptions()
        val seen = mutableListOf<String>()
        subs.on(EventKind.WAKE_WORD, "x") { seen += "old" }
        subs.on(EventKind.WAKE_WORD, "x") { seen += "new" }
        subs.dispatch(VisionEvent.WakeWord)
        assertEquals(listOf("new"), seen)
    }

    @Test fun `off removes a subscriber`() {
        val subs = EventSubscriptions()
        subs.onAny("x") {}
        assertTrue(subs.off("x"))
        assertFalse(subs.off("x"))
        assertTrue(subs.dispatch(VisionEvent.WakeWord).isEmpty())
    }

    @Test fun `a throwing handler does not stop the others`() {
        val subs = EventSubscriptions()
        var reached = false
        subs.onAny("boom") { error("kaboom") }
        subs.onAny("safe") { reached = true }
        val fired = subs.dispatch(VisionEvent.WakeWord)
        assertEquals(listOf("boom", "safe"), fired)
        assertTrue(reached)
    }

    @Test fun `subscribersFor introspects without firing handlers`() {
        val subs = EventSubscriptions()
        var fired = false
        subs.on(EventKind.COMMAND_RECEIVED, "cmd") { fired = true }
        assertEquals(listOf("cmd"), subs.subscribersFor(VisionEvent.CommandReceived("x")))
        assertFalse(fired)
    }
}
