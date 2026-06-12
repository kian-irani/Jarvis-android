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

    override val available: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    init {
        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) tts?.language = Locale.US
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
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vision-reply")
            }
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
