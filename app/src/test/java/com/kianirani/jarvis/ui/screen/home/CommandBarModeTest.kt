package com.kianirani.jarvis.ui.screen.home

import org.junit.Assert.assertEquals
import org.junit.Test

class CommandBarModeTest {

    @Test fun `speaking shows stop, even with text in the field`() {
        assertEquals(CommandBarMode.STOP, commandBarMode(isSpeaking = true, hasText = false))
        assertEquals(CommandBarMode.STOP, commandBarMode(isSpeaking = true, hasText = true))
    }

    @Test fun `text and not speaking shows send`() {
        assertEquals(CommandBarMode.SEND, commandBarMode(isSpeaking = false, hasText = true))
    }

    @Test fun `idle shows mic`() {
        assertEquals(CommandBarMode.MIC, commandBarMode(isSpeaking = false, hasText = false))
    }
}
