package com.kianirani.jarvis.voice

import com.kianirani.jarvis.data.settings.NeuralVoiceMode
import com.kianirani.jarvis.data.settings.VisionSettings

/**
 * FV6 — voice provider abstraction (PRD Part 3.1). Generalizes the v34/v41 two-engine voice
 * (Android on-device + Edge neural) into a pluggable [TTSEngine] interface and a [VoiceProvider]
 * catalog, so future engines (Piper, ElevenLabs, OpenAI) drop in without touching call sites.
 * [VoiceProviderSelector] is the pure routing brain (extends [VoiceRouting]); the concrete
 * engines wrap their SDK/HTTP. Pure parts are JVM-testable; the engines are the device half.
 */
interface TTSEngine {
    val provider: VoiceProvider

    /** True if this engine can run right now (installed / online / has a key). */
    fun isAvailable(): Boolean

    /** Speak [text] in [language] (e.g. "fa-IR"); [onDone] fires when playback ends. */
    fun speak(text: String, language: String, onDone: () -> Unit = {})

    fun stop()

    /** Voices this engine offers for [language] (for the picker). */
    fun voices(language: String): List<VoiceOption>
}

/** The known TTS back-ends. [onDevice] engines work offline; the rest need the network. */
enum class VoiceProvider(val displayName: String, val onDevice: Boolean) {
    ANDROID("Android (on-device)", true),
    EDGE_NEURAL("Edge Neural", false),
    PIPER("Piper (on-device)", true),
    ELEVENLABS("ElevenLabs", false),
    OPENAI("OpenAI", false),
}

/**
 * FV6 — picks which [VoiceProvider] should speak a given reply, generalizing [VoiceRouting].
 * Honors the user's [NeuralVoiceMode], offline fallback, and the set of engines actually
 * available, so we are never silent. Pure → JVM-tested.
 */
object VoiceProviderSelector {

    /**
     * Choose a provider from those [available] (insertion-ordered preference). The decision:
     * - offline → first available on-device engine (or null if none).
     * - the reply wants a neural voice ([VoiceRouting.useNeural]) → first available non-on-device
     *   engine, else fall back to an on-device one.
     * - otherwise → first available on-device engine, else any available.
     * Returns null only when nothing is available.
     */
    fun select(
        available: List<VoiceProvider>,
        language: String?,
        mode: NeuralVoiceMode,
        online: Boolean,
        replyText: String = "",
    ): VoiceProvider? {
        if (available.isEmpty()) return null
        val onDevice = available.firstOrNull { it.onDevice }
        if (!online) return onDevice

        val wantsNeural = VoiceRouting.useNeural(language, mode, online = true, replyText = replyText)
        return if (wantsNeural) {
            available.firstOrNull { !it.onDevice } ?: onDevice
        } else {
            onDevice ?: available.first()
        }
    }

    /** True when [language] is Persian (the case that needs a neural voice — stock fa-IR is poor). */
    fun isPersian(language: String?): Boolean = language == VisionSettings.LANG_FA
}
