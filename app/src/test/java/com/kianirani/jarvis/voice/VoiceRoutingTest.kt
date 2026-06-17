package com.kianirani.jarvis.voice

import com.kianirani.jarvis.data.settings.VisionSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceRoutingTest {

    @Test fun `offline never uses neural`() {
        assertFalse(VoiceRouting.useNeural(VisionSettings.LANG_FA, neuralEnabled = true, online = false))
        assertFalse(VoiceRouting.useNeural(VisionSettings.LANG_EN, neuralEnabled = true, online = false))
    }

    @Test fun `persian online auto-enables neural even without the toggle`() {
        assertTrue(VoiceRouting.useNeural(VisionSettings.LANG_FA, neuralEnabled = false, online = true))
    }

    @Test fun `explicit neural toggle uses neural for any language when online`() {
        assertTrue(VoiceRouting.useNeural(VisionSettings.LANG_EN, neuralEnabled = true, online = true))
        assertTrue(VoiceRouting.useNeural(VisionSettings.LANG_AUTO, neuralEnabled = true, online = true))
    }

    @Test fun `non-persian without the toggle stays on-device`() {
        assertFalse(VoiceRouting.useNeural(VisionSettings.LANG_EN, neuralEnabled = false, online = true))
        assertFalse(VoiceRouting.useNeural(VisionSettings.LANG_AUTO, neuralEnabled = false, online = true))
        assertFalse(VoiceRouting.useNeural(null, neuralEnabled = false, online = true))
    }
}
