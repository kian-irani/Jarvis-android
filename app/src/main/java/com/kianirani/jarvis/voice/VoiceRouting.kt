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
 *
 * Device-bug fix (2026-06-19, user-reported on v67): the language *setting* defaults to
 * [VisionSettings.LANG_AUTO], not `LANG_FA`, so AUTO mode used to pick on-device for a
 * Persian reply — and Samsung/Google on-device engines ship **no `fa-IR` voice**, so the
 * Persian reply came out silent/garbled. AUTO now also looks at the **actual reply text**:
 * if the reply contains Persian script, route to the neural voice (the only reliable
 * Persian voice) regardless of the language setting.
 */
object VoiceRouting {
    /** True when [text] contains any Persian/Arabic-script character (reuses the segmenter). */
    fun hasPersian(text: String): Boolean =
        text.any { VoiceSegmenter.scriptOf(it) == VoiceSegmenter.Script.PERSIAN }

    /**
     * Whether Google Translate's read-aloud endpoint can voice [text]. Google has **no Persian
     * voice** — `translate_tts?tl=fa` returns HTTP 400 for any input — so it must never be used
     * for a reply that contains Persian (doing so wasted a guaranteed-failing round-trip before
     * Edge and was the v129 "Google-primary" regression that left Persian silent/slow). Edge
     * neural is the only reliable free Persian voice; Google stays a Latin-only fallback.
     */
    fun googleCanSpeak(text: String): Boolean = !hasPersian(text)

    fun useNeural(language: String?, mode: NeuralVoiceMode, online: Boolean, replyText: String = ""): Boolean {
        if (!online) return false
        return when (mode) {
            NeuralVoiceMode.ON -> true
            NeuralVoiceMode.OFF -> false
            // AUTO: neural for Persian — whether the user pinned Persian OR the reply itself
            // is Persian (the setting is usually AUTO; on-device Persian TTS is poor/missing).
            NeuralVoiceMode.AUTO -> language == VisionSettings.LANG_FA || hasPersian(replyText)
        }
    }
}
