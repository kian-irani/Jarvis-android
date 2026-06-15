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
}
