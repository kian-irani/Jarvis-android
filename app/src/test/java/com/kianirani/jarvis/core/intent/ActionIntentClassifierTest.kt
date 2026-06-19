package com.kianirani.jarvis.core.intent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DS-B1 acceptance: the action classifier maps EN + FA commands to the right protocol
 * intent and slot, and falls back to a low-confidence `chat`. Pure, no device.
 */
class ActionIntentClassifierTest {

    @Test fun `open app extracts the app name`() {
        val i = ActionIntentClassifier.classify("open Spotify")
        assertEquals("open_app", i.name)
        assertEquals("Spotify", i.slots["app"])
    }

    @Test fun `call extracts the target and strips filler`() {
        val i = ActionIntentClassifier.classify("call mom")
        assertEquals("call", i.name)
        assertEquals("mom", i.slots["target"])
    }

    @Test fun `set reminder extracts the task`() {
        val i = ActionIntentClassifier.classify("remind me to buy milk")
        assertEquals("set_reminder", i.name)
        assertEquals("buy milk", i.slots["task"])
    }

    @Test fun `search extracts the query and trims punctuation`() {
        val i = ActionIntentClassifier.classify("search for pizza near me.")
        assertEquals("search", i.name)
        assertEquals("pizza near me", i.slots["query"])
    }

    @Test fun `weather and time are recognised without a slot`() {
        assertEquals("weather", ActionIntentClassifier.classify("what's the weather like?").name)
        val time = ActionIntentClassifier.classify("what time is it")
        assertEquals("time", time.name)
        assertTrue(time.slots.isEmpty())
    }

    @Test fun `persian call command classifies and extracts target`() {
        val i = ActionIntentClassifier.classify("زنگ بزن به علی")
        assertEquals("call", i.name)
        assertEquals("علی", i.slots["target"])
    }

    @Test fun `persian open command classifies as open_app`() {
        assertEquals("open_app", ActionIntentClassifier.classify("تلگرام را باز کن").name)
    }

    @Test fun `plain conversation falls back to low-confidence chat`() {
        val i = ActionIntentClassifier.classify("hey, how are you doing today?")
        assertEquals("chat", i.name)
        assertEquals(0.3f, i.confidence)
    }

    @Test fun `blank text is chat`() {
        assertEquals("chat", ActionIntentClassifier.classify("   ").name)
    }
}
