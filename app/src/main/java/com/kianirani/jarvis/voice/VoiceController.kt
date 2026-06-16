package com.kianirani.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * Voice conversation (v2, 2026-06-16). On-device Android SpeechRecognizer for
 * input and TextToSpeech for replies — free, no extra dependencies. v2 adds
 * **code-switch aware speaking**: replies are split by [VoiceSegmenter] and each
 * Persian / Latin run is spoken sequentially with its own voice, so mixed
 * "یک playlist از Shakira" sounds natural instead of one voice mangling the other.
 * It also picks the highest-quality installed voice per language and lets the
 * user pin a specific voice ([voicesFor] / [VisionSettings] voice prefs).
 *
 * All entry points may be called from any dispatcher; access is marshalled to the
 * main looper (SpeechRecognizer/TTS thread contract).
 */
interface VoiceController {
    val available: Boolean
    fun startListening(onResult: (String) -> Unit, onEnd: () -> Unit)
    fun stopListening()
    fun speak(text: String)
    /** Installed voices for a language ("fa", "en", …) for the settings picker. */
    fun voicesFor(language: String): List<VoiceOption>
    /** Speak a short sample in [language] using the current/best voice (TEST VOICE). */
    fun speakSample(language: String)
    fun release()
}

/** A selectable TTS voice surfaced to the settings picker (FV2). */
data class VoiceOption(
    val id: String,
    val displayName: String,
    val quality: Int,
    val needsNetwork: Boolean,
)

class AndroidVoiceController(
    private val context: Context,
    private val settings: com.kianirani.jarvis.data.settings.VisionSettings? = null,
) : VoiceController {
    private companion object { const val TAG = "VisionVoice" }

    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeech: String? = null

    // Sequential code-switch queue: (locale, text) spoken one after another so
    // each segment's language is applied before that segment plays.
    private val queue = ArrayDeque<Pair<Locale, String>>()

    override val available: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    init {
        tts = TextToSpeech(context) { status ->
            main.post {
                ttsReady = status == TextToSpeech.SUCCESS
                if (ttsReady) {
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {
                            if (utteranceId?.startsWith("seg") == true) main.post { speakNext(flush = false) }
                        }
                        @Deprecated("deprecated in API") override fun onError(utteranceId: String?) {}
                        override fun onError(utteranceId: String?, errorCode: Int) {}
                    })
                    // Default voice; per-segment language is applied at speak time.
                    if ((tts?.setLanguage(Locale.getDefault()) ?: TextToSpeech.LANG_MISSING_DATA) < TextToSpeech.LANG_AVAILABLE) {
                        tts?.language = Locale.US
                    }
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

    // ── Speaking ─────────────────────────────────────────────────────────────

    override fun speak(text: String) {
        if (text.isBlank()) return
        main.post {
            if (!ttsReady) { pendingSpeech = text; return@post }
            settings?.let { tts?.setSpeechRate(it.speechRate.value); tts?.setPitch(it.voicePitch.value) }
            // Split into per-script runs and speak them in order, each with its
            // own voice — the heart of the Persian/English code-switch fix.
            queue.clear()
            VoiceSegmenter.segment(text).forEach { seg -> queue.add(localeFor(seg.script) to seg.text) }
            if (queue.isEmpty()) queue.add(localeFor(VoiceSegmenter.Script.NEUTRAL) to text)
            speakNext(flush = true)
        }
    }

    private var segCounter = 0
    private fun speakNext(flush: Boolean) {
        val (locale, text) = queue.removeFirstOrNull() ?: return
        if (text.isBlank()) { speakNext(flush); return } // skip empties, keep ordering
        applyVoice(locale)
        tts?.speak(text, if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "seg${segCounter++}")
    }

    /** Select the voice for [locale]: a user-pinned voice if set, else the best installed one. */
    private fun applyVoice(locale: Locale) {
        val t = tts ?: return
        val pinned = settings?.let { selectedVoiceName(locale.language) }
        val voice = pinned?.let { name -> t.voices?.firstOrNull { it.name == name } }
            ?: bestVoiceFor(locale)
        if (voice != null) {
            t.voice = voice
            return
        }
        val res = t.setLanguage(locale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "TTS voice unavailable for $locale (res=$res) — prompting install")
            promptInstallVoice(locale)
            t.setLanguage(Locale.getDefault())
        }
    }

    /** Highest-quality installed voice for the locale's language; prefers offline on ties. */
    private fun bestVoiceFor(locale: Locale): Voice? =
        runCatching {
            tts?.voices
                ?.filter { it.locale.language == locale.language }
                ?.filterNot { it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true }
                // Prefer the highest quality; on ties prefer a voice that works offline.
                ?.sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.isNetworkConnectionRequired })
                ?.firstOrNull()
        }.getOrNull()

    override fun voicesFor(language: String): List<VoiceOption> = runCatching {
        tts?.voices
            ?.filter { it.locale.language == language }
            ?.filterNot { it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true }
            ?.sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.name })
            ?.map {
                VoiceOption(
                    id = it.name,
                    displayName = voiceLabel(it),
                    quality = it.quality,
                    needsNetwork = it.isNetworkConnectionRequired,
                )
            }
            ?: emptyList()
    }.getOrDefault(emptyList())

    override fun speakSample(language: String) {
        val sample = when (language) {
            "fa" -> "سلام، من ویژن هستم. در خدمتِ شما هستم."
            else -> "Hi, I'm Vision. I'm ready to help you."
        }
        speak(sample)
    }

    private fun voiceLabel(v: Voice): String {
        val q = when {
            v.quality >= Voice.QUALITY_VERY_HIGH -> "Very High"
            v.quality >= Voice.QUALITY_HIGH -> "High"
            v.quality >= Voice.QUALITY_NORMAL -> "Normal"
            else -> "Low"
        }
        val country = v.locale.country.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
        val net = if (v.isNetworkConnectionRequired) " · online" else ""
        return "$q$country$net"
    }

    private fun selectedVoiceName(language: String): String? = settings?.selectedVoiceName(language)

    /** Map a script run to the locale whose voice should speak it. */
    private fun localeFor(script: VoiceSegmenter.Script): Locale = when (script) {
        VoiceSegmenter.Script.PERSIAN -> Locale("fa")
        VoiceSegmenter.Script.LATIN -> Locale.US
        VoiceSegmenter.Script.NEUTRAL -> when (settings?.language?.value) {
            "fa" -> Locale("fa")
            "en" -> Locale.US
            else -> Locale.getDefault()
        }
    }

    /** Fire the system "install TTS voice" flow once per process + tell the user why. */
    private var promptedInstall = false
    private fun promptInstallVoice(locale: Locale) {
        if (promptedInstall) return
        promptedInstall = true
        runCatching {
            context.startActivity(
                Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        runCatching {
            android.widget.Toast.makeText(
                context,
                "Install the ${locale.displayLanguage} voice in TTS settings to hear spoken replies",
                android.widget.Toast.LENGTH_LONG,
            ).show()
        }
    }

    override fun release() {
        main.post {
            stopListeningOnMain()
            ttsReady = false
            queue.clear()
            tts?.shutdown(); tts = null
        }
    }
}
