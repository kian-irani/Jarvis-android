package com.kianirani.jarvis.data.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** v20 fix — CallTool/SmsTool command parsing (EN + FA). Pure, no Android needed. */
class ActionToolParsingTest {

    // --- CallTool.parseTarget ---

    @Test
    fun `parses english call commands`() {
        assertEquals("mother", CallTool.parseTarget("call mother"))
        assertEquals("mother", CallTool.parseTarget("Call Mother now"))
        assertEquals("ali reza", CallTool.parseTarget("phone ali reza"))
        assertEquals("dad", CallTool.parseTarget("dial dad please"))
    }

    @Test
    fun `parses persian call commands across word orders`() {
        assertEquals("مامان", CallTool.parseTarget("با مامان تماس بگیر"))
        assertEquals("علی", CallTool.parseTarget("به علی زنگ بزن"))
        assertEquals("مامان", CallTool.parseTarget("تماس بگیر با مامان"))
        assertEquals("علی", CallTool.parseTarget("زنگ بزن به علی"))
    }

    @Test
    fun `non-call messages are not matched as calls`() {
        assertNull(CallTool.parseTarget("what time is it"))
        assertNull(CallTool.parseTarget("recall the last thing")) // must not match "call"
        assertNull(CallTool.parseTarget("flashlight on"))
        assertNull(CallTool.parseTarget(""))
    }

    // --- SmsTool.parse ---

    @Test
    fun `parses english message commands into target and body`() {
        assertEquals("mother" to "i am late", SmsTool.parse("text mother saying i am late"))
        assertEquals("ali" to "call me", SmsTool.parse("message ali: call me"))
        assertEquals("dad" to "on my way", SmsTool.parse("send dad a message saying on my way"))
    }

    @Test
    fun `parses persian message commands into target and body`() {
        assertEquals("مامان" to "دیر میام", SmsTool.parse("به مامان پیام بده که دیر میام"))
        assertEquals("علی" to "زنگ بزن", SmsTool.parse("به علی بنویس زنگ بزن"))
    }

    @Test
    fun `non-message commands are not matched`() {
        assertNull(SmsTool.parse("call mother"))
        assertNull(SmsTool.parse("what's the weather"))
        assertNull(SmsTool.parse(""))
    }

    // --- v79 device-bug fix: broaden parsing to the phrasings users (and the model) use ---

    @Test
    fun `parses more natural persian call phrasings`() {
        assertEquals("مامان", CallTool.parseTarget("به مامان یه زنگ بزن"))
        assertEquals("مامان", CallTool.parseTarget("زنگ بزن مامان"))
        assertEquals("علی", CallTool.parseTarget("تماس با علی بگیر"))
        assertEquals("علی", CallTool.parseTarget("شماره علی رو بگیر"))
        // existing qualified forms must still win, not be swallowed by the looser ones
        assertEquals("علی", CallTool.parseTarget("زنگ بزن به علی"))
    }

    @Test
    fun `parses give-a-call english phrasing`() {
        assertEquals("mom", CallTool.parseTarget("give mom a call"))
    }

    @Test
    fun `parses the bego form the prompt tells the model to emit`() {
        // TOOL_PROTOCOL example: «به مامان بگو: فردا نمیام» — previously unmatched -> "didn't understand".
        assertEquals("مامان" to "فردا نمیام", SmsTool.parse("به مامان بگو: فردا نمیام"))
        assertEquals("مامان" to "فردا نمیام", SmsTool.parse("به مامان بگو فردا نمیام"))
        assertEquals("علی" to "سلام", SmsTool.parse("به علی بفرست سلام"))
        assertEquals("مامان" to "دوستت دارم", SmsTool.parse("برای مامان بنویس دوستت دارم"))
    }

    @Test
    fun `bego to self or assistant is a chat request not an sms`() {
        // "tell me the time" must flow to the AI, not become an SMS to a contact "من".
        assertNull(SmsTool.parse("به من بگو ساعت چنده"))
        assertNull(SmsTool.parse("به ویژن بگو خاموش شو"))
        // a real addressee with بگو still parses
        assertEquals("علی" to "سلام", SmsTool.parse("به علی بگو سلام"))
    }

    // --- case handling: matching is case-insensitive, but the SMS body keeps its capitalization
    // (the registry used to lowercase the whole message, so "See you at 5PM" was sent as
    // "see you at 5pm"). The target is still lowercased for contact lookup.
    @Test
    fun `message body preserves original capitalization while matching is case-insensitive`() {
        assertEquals("mom" to "I'll be there at 5PM", SmsTool.parse("Text Mom: I'll be there at 5PM"))
        assertEquals("ali" to "OK See you", SmsTool.parse("MESSAGE Ali saying OK See you"))
        assertEquals("dad" to "On My Way", SmsTool.parse("Send Dad a message saying On My Way"))
    }
}
