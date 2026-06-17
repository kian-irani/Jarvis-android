package com.kianirani.jarvis.voice

import com.kianirani.jarvis.data.settings.NeuralVoiceMode
import com.kianirani.jarvis.data.settings.VisionSettings

/**
 * BUG-4 / v51 — pure TTS-engine routing. Decides whether a reply is spoken with the
 * online Edge **neural** voice or the on-device engine, independent of Android so
 * it is unit-testable on the JVM.
 *
 * Driven by the user's [NeuralVoiceMode]: ON = always neural (online); OFF = always
 * on-device; AUTO = neural for Persian when online (stock on-device Persian TTS is
 * poor/often missing). Offline always falls back to on-device so we are never silent.
 */
object VoiceRouting {
    fun useNeural(language: String?, mode: NeuralVoiceMode, online: Boolean): Boolean {
        if (!online) return false
        return when (mode) {
            NeuralVoiceMode.ON -> true
            NeuralVoiceMode.OFF -> false
            NeuralVoiceMode.AUTO -> language == VisionSettings.LANG_FA
        }
    }
}
