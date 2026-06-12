package com.kianirani.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Voice conversation v1 (USER DIRECTIVE 2026-06-12: voice on from the first
 * version). On-device Android SpeechRecognizer for input and TextToSpeech for
 * replies — no extra dependencies; providers can replace both layers later.
 *
 * All entry points must be called from the main thread (SpeechRecognizer
 * requirement); the HUD ViewModel guarantees that.
 */
interface VoiceController {
    val available: Boolean
    fun startListening(onResult: (String) -> Unit, onEnd: () -> Unit)
    fun stopListening()
    fun speak(text: String)
    fun release()
}

class AndroidVoiceController(
    private val context: Context,
    private val settings: com.kianirani.jarvis.data.settings.VisionSettings? = null,
) : VoiceController {
    private companion object { const val TAG = "VisionVoice" }

    // All SpeechRecognizer/TTS access is posted to the main looper so callers
    // may invoke from any dispatcher (review HIGH-3: explicit thread contract).
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeech: String? = null

    override val available: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    init {
        tts = TextToSpeech(context) { status ->
            main.post {
                ttsReady = status == TextToSpeech.SUCCESS
                if (ttsReady) {
                    // Prefer the device locale; fall back to US English.
                    if (tts?.setLanguage(Locale.getDefault()) ?: TextToSpeech.LANG_MISSING_DATA < TextToSpeech.LANG_AVAILABLE) {
                        tts?.language = Locale.US
                    }
                    // Replies requested before the engine came up must not be lost.
                    pendingSpeech?.let { speak(it) }; pendingSpeech = null
                }
            }
        }
    }

    override fun startListening(onResult: (String) -> Unit, onEnd: () -> Unit) {
        if (!available) { onEnd(); return }
        main.post { startListeningOnMain(onResult, onEnd) }
    }

    private fun startListeningOnMain(onResult: (String) -> Unit, onEnd: () -> Unit) {
        stopListeningOnMain()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    if (text.isNotBlank()) onResult(text)
                    onEnd()
                }
                override fun onError(error: Int) { Log.w(TAG, "STT error $error"); onEnd() }
                override fun onEndOfSpeech() {}
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    // Pin STT language when the user chose one; AUTO uses the device default.
                    when (settings?.language?.value) {
                        "fa" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                        "en" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    }
                },
            )
        }
    }

    override fun stopListening() {
        main.post { stopListeningOnMain() }
    }

    private fun stopListeningOnMain() {
        recognizer?.run { stopListening(); destroy() }
        recognizer = null
    }

    override fun speak(text: String) {
        if (text.isBlank()) return
        main.post {
            if (ttsReady) {
                // P7 persona: user-tuned delivery style.
                settings?.let { tts?.setSpeechRate(it.speechRate.value); tts?.setPitch(it.voicePitch.value) }
                // Multilingual: speak Persian replies with a Persian voice so
                // "فارسی in → فارسی out" actually sounds right (USER DIRECTIVE 2026-06-12).
                tts?.language = ttsLocaleFor(text)
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vision-reply")
            } else {
                pendingSpeech = text
            }
        }
    }

    /** Persian script anywhere -> Persian voice; otherwise honor the language pref / device. */
    private fun ttsLocaleFor(text: String): Locale {
        if (text.any { it in '؀'..'ۿ' }) return Locale("fa")
        return when (settings?.language?.value) {
            "fa" -> Locale("fa")
            "en" -> Locale.US
            else -> Locale.getDefault()
        }
    }

    override fun release() {
        main.post {
            stopListeningOnMain()
            ttsReady = false
            tts?.shutdown(); tts = null
        }
    }
}
