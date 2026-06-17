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
}
