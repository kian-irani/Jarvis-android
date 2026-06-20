package com.kianirani.jarvis.voice

/**
 * VCF-M3 / FV6 — speech-to-text abstraction (PRD §8.2). Separates **acquisition** (capturing
 * audio → text, always local-first) from **processing** (what the agent does with the text), so
 * the recognizer can be swapped — Android `SpeechRecognizer` today, Vosk/Whisper on-device or a
 * cloud STT later — without touching call sites (the openclaw voice split). The interface is
 * pure; the Android implementation wraps the platform recognizer.
 */
interface SpeechToText {
    val engine: SttEngine

    fun isAvailable(): Boolean

    /**
     * Listen for one utterance in [language] (BCP-47, e.g. "fa-IR"). [onPartial] streams interim
     * text; [onResult] delivers the final transcript; [onError] reports failure.
     */
    fun listen(
        language: String,
        onPartial: (String) -> Unit = {},
        onResult: (String) -> Unit,
        onError: (String) -> Unit = {},
    )

    fun stop()
}

enum class SttEngine(val displayName: String, val onDevice: Boolean) {
    ANDROID("Android (on-device)", true),
    VOSK("Vosk (on-device)", true),
    WHISPER("Whisper (on-device)", true),
    CLOUD("Cloud STT", false),
}

/**
 * VCF-M3 — the perception side: acquire speech locally then hand the transcript to the agent.
 * Holds the chosen [SpeechToText] and exposes [listen]; choosing the engine (prefer on-device,
 * fall back as needed) is [select]. Pure orchestration over the injected engine.
 */
class AudioPerception(private val stt: SpeechToText) {

    /** Capture one spoken turn in [language]; [onTranscript] gets the final text for reasoning. */
    fun listen(language: String, onTranscript: (String) -> Unit, onError: (String) -> Unit = {}) {
        if (!stt.isAvailable()) {
            onError("Speech recognition is not available on this device.")
            return
        }
        stt.listen(language = language, onResult = onTranscript, onError = onError)
    }

    fun stop() = stt.stop()

    companion object {
        /** Pick the best available STT engine, preferring on-device (privacy + offline). */
        fun select(available: List<SttEngine>): SttEngine? =
            available.firstOrNull { it.onDevice } ?: available.firstOrNull()
    }
}
