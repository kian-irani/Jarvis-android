package com.kianirani.jarvis.core.gesture

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * DS-W3 acceptance: defaults map the documented gestures, custom bindings override defaults,
 * an explicit NONE disables a gesture, unmapped gestures return NONE, and bind is immutable.
 * Pure.
 */
class GestureMapTest {

    @Test fun `defaults match the spec`() {
        val m = GestureMap()
        assertEquals(GestureAction.QUICK_PROMPT, m.actionFor(Gesture.TAP))
        assertEquals(GestureAction.VOICE, m.actionFor(Gesture.LONG_PRESS))
        assertEquals(GestureAction.EXPAND_PANEL, m.actionFor(Gesture.SWIPE_UP))
        assertEquals(GestureAction.REPEAT_LAST, m.actionFor(Gesture.DOUBLE_TAP))
    }

    @Test fun `an unmapped gesture is NONE`() {
        assertEquals(GestureAction.NONE, GestureMap().actionFor(Gesture.PINCH))
    }

    @Test fun `custom binding overrides the default`() {
        val m = GestureMap(mapOf(Gesture.TAP to GestureAction.OPEN_VISION))
        assertEquals(GestureAction.OPEN_VISION, m.actionFor(Gesture.TAP))
    }

    @Test fun `an explicit NONE disables a default gesture`() {
        val m = GestureMap(mapOf(Gesture.LONG_PRESS to GestureAction.NONE))
        assertEquals(GestureAction.NONE, m.actionFor(Gesture.LONG_PRESS))
    }

    @Test fun `bind returns a new map and leaves the original unchanged`() {
        val original = GestureMap()
        val rebound = original.bind(Gesture.SWIPE_DOWN, GestureAction.NOTIFICATIONS)
        assertEquals(GestureAction.NOTIFICATIONS, rebound.actionFor(Gesture.SWIPE_DOWN))
        assertEquals(GestureAction.NONE, original.actionFor(Gesture.SWIPE_DOWN)) // unchanged
    }

    @Test fun `bindings includes defaults merged with overrides`() {
        val m = GestureMap(mapOf(Gesture.PINCH to GestureAction.LOCK))
        assertEquals(GestureAction.LOCK, m.bindings()[Gesture.PINCH])
        assertEquals(GestureAction.QUICK_PROMPT, m.bindings()[Gesture.TAP])
    }
}
