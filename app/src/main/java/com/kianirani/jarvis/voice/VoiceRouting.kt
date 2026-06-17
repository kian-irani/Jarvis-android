package com.kianirani.jarvis.voice

import com.kianirani.jarvis.data.settings.VisionSettings

/**
 * BUG-4 — pure TTS-engine routing. Decides whether a reply should be spoken with
 * the online Edge **neural** voice or the on-device engine, independent of Android
 * so it is unit-testable on the JVM.
 *
 * Rule: use neural when it is reachable (online) AND the user wants it — either by
 * explicitly enabling neural voice, OR by setting their reply language to Persian
 * (`fa`). Persian users who picked Persian expect the fluent neural voice, since
 * the stock on-device Persian TTS is poor/often missing. Anything else (or offline)
 * stays on-device so we are never silent.
 */
object VoiceRouting {
    fun useNeural(language: String?, neuralEnabled: Boolean, online: Boolean): Boolean {
        if (!online) return false
        return neuralEnabled || language == VisionSettings.LANG_FA
    }
}
