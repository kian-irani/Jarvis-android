package com.kianirani.jarvis.voice

import com.kianirani.jarvis.data.settings.NeuralVoiceMode
import com.kianirani.jarvis.data.settings.VisionSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceRoutingTest {

    @Test fun `offline never uses neural, whatever the mode`() {
        assertFalse(VoiceRouting.useNeural(VisionSettings.LANG_FA, NeuralVoiceMode.ON, online = false))
        assertFalse(VoiceRouting.useNeural(VisionSettings.LANG_FA, NeuralVoiceMode.AUTO, online = false))
        assertFalse(VoiceRouting.useNeural(VisionSettings.LANG_EN, NeuralVoiceMode.ON, online = false))
    }

    @Test fun `AUTO uses neural for Persian online, on-device otherwise`() {
        assertTrue(VoiceRouting.useNeural(VisionSettings.LANG_FA, NeuralVoiceMode.AUTO, online = true))
        assertFalse(VoiceRouting.useNeural(VisionSettings.LANG_EN, NeuralVoiceMode.AUTO, online = true))
        assertFalse(VoiceRouting.useNeural(VisionSettings.LANG_AUTO, NeuralVoiceMode.AUTO, online = true))
        assertFalse(VoiceRouting.useNeural(null, NeuralVoiceMode.AUTO, online = true))
    }

    @Test fun `ON always uses neural when online, any language`() {
        assertTrue(VoiceRouting.useNeural(VisionSettings.LANG_EN, NeuralVoiceMode.ON, online = true))
        assertTrue(VoiceRouting.useNeural(VisionSettings.LANG_FA, NeuralVoiceMode.ON, online = true))
    }

    @Test fun `OFF stays on-device even for Persian online`() {
        assertFalse(VoiceRouting.useNeural(VisionSettings.LANG_FA, NeuralVoiceMode.OFF, online = true))
        assertFalse(VoiceRouting.useNeural(VisionSettings.LANG_EN, NeuralVoiceMode.OFF, online = true))
    }

    // Device-bug fix (v67 report): the language setting defaults to LANG_AUTO, not LANG_FA,
    // so a Persian reply must be detected from the *text* — on-device has no fa-IR voice.
    @Test fun `AUTO uses neural for a Persian reply even when the language setting is auto`() {
        assertTrue(VoiceRouting.useNeural(VisionSettings.LANG_AUTO, NeuralVoiceMode.AUTO, online = true, replyText = "سلام، حالت چطوره؟"))
        assertTrue(VoiceRouting.useNeural(null, NeuralVoiceMode.AUTO, online = true, replyText = "خوبم ممنون"))
        // a mixed code-switch reply still counts as Persian
        assertTrue(VoiceRouting.useNeural(VisionSettings.LANG_AUTO, NeuralVoiceMode.AUTO, online = true, replyText = "یک playlist از Shakira"))
        // a pure-English reply under AUTO stays on-device
        assertFalse(VoiceRouting.useNeural(VisionSettings.LANG_AUTO, NeuralVoiceMode.AUTO, online = true, replyText = "hello there"))
        // offline Persian reply: still on-device (the neural service is unreachable)
        assertFalse(VoiceRouting.useNeural(VisionSettings.LANG_AUTO, NeuralVoiceMode.AUTO, online = false, replyText = "سلام"))
    }

    @Test fun `OFF stays on-device even for a Persian reply text`() {
        assertFalse(VoiceRouting.useNeural(VisionSettings.LANG_AUTO, NeuralVoiceMode.OFF, online = true, replyText = "سلام"))
    }

    @Test fun `hasPersian detects persian script and ignores latin-only`() {
        assertTrue(VoiceRouting.hasPersian("سلام"))
        assertTrue(VoiceRouting.hasPersian("hi سلام"))
        assertFalse(VoiceRouting.hasPersian("hello 123"))
        assertFalse(VoiceRouting.hasPersian(""))
    }
}
