package com.kianirani.jarvis.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.kianirani.jarvis.core.event.VisionEvent
import com.kianirani.jarvis.core.event.VisionEventBus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * FV4 — always-listening wake word ("Hey Vision" / «ویژن»). A foreground service that runs
 * Android's on-device [SpeechRecognizer] in a continuous loop, scanning partial results for a
 * wake phrase; on a hit it emits [VisionEvent.WakeWord] on the [VisionEventBus] (the UI/agent
 * react) and restarts listening. Using the platform recognizer keeps the dependency footprint
 * zero (no Porcupine model) and the audio on-device. The restart loop is what fixes the v20
 * "mic keeps turning off" bug — the recognizer is immediately re-armed on end/error.
 *
 * Start/stop via [WakeWord]. Requires RECORD_AUDIO (already declared). Battery: the recognizer
 * is the platform's own, and [PowerPolicy] can gate whether the service runs at all.
 */
@AndroidEntryPoint
class WakeWordService : Service() {

    @Inject lateinit var eventBus: VisionEventBus

    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    private val phrases = listOf("hey vision", "vision", "ویژن", "هی ویژن", "ای ویژن")

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startInForeground()
        startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply { setRecognitionListener(listener) }
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        listening = true
        runCatching { recognizer?.startListening(intent) }
    }

    /** Re-arm the recognizer after each utterance/error so it never silently stops. */
    private fun restart() {
        if (!listening) return
        runCatching { recognizer?.cancel() }
        startListening()
    }

    private fun onHeard(texts: List<String>) {
        val hit = texts.any { t -> phrases.any { t.lowercase().contains(it) } }
        if (hit) eventBus.tryEmit(VisionEvent.WakeWord)
    }

    private val listener = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { onHeard(it) }
            restart()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { onHeard(it) }
        }

        override fun onError(error: Int) = restart()
        override fun onEndOfSpeech() = Unit // results/error will re-arm
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun startInForeground() {
        val channelId = "vision_wakeword"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(channelId, "Vision Wake Word", NotificationManager.IMPORTANCE_MIN),
                )
            }
        }
        val notification: Notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setContentTitle("Vision")
            .setContentText("Listening for \"Hey Vision\"")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MIN)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    override fun onDestroy() {
        listening = false
        runCatching { recognizer?.destroy() }
        recognizer = null
        super.onDestroy()
    }

    private companion object {
        const val NOTIF_ID = 4202
    }
}

/** FV4 — start/stop the wake-word listener. */
object WakeWord {
    fun start(context: Context) {
        val intent = Intent(context, WakeWordService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
        else context.startService(intent)
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, WakeWordService::class.java))
    }
}
