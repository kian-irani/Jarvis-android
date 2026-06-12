package com.kianirani.jarvis.ui.screen.hud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kianirani.jarvis.data.ai.CloudChatRouter
import com.kianirani.jarvis.data.repository.ApiNode
import com.kianirani.jarvis.data.repository.BrainEvent
import com.kianirani.jarvis.data.repository.BrainRepository
import com.kianirani.jarvis.voice.VoiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sin

private val IDLE = listOf("All systems operational.", "3 nodes online.", "Groq ready.", "Awaiting command.")

@HiltViewModel
class HudViewModel @Inject constructor(
    private val brain: BrainRepository,
    private val voice: VoiceController,
    private val cloud: CloudChatRouter,
    private val interpreter: com.kianirani.jarvis.data.agent.CommandInterpreter,
    private val settings: com.kianirani.jarvis.data.settings.VisionSettings,
) : ViewModel() {
    private val _state = MutableStateFlow(HudUiState())
    val uiState: StateFlow<HudUiState> = _state.asStateFlow()
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val logFmt = SimpleDateFormat("HH:mm:ss", Locale.US)
    private var typeJob: Job? = null
    private var idleJob: Job? = null

    init { startClock(); startWaveform(); connectBrain() }

    private fun startClock() = viewModelScope.launch {
        while (isActive) { _state.update { it.copy(currentTime = timeFmt.format(Date())) }; delay(1_000) }
    }

    private fun startWaveform() = viewModelScope.launch {
        var t = 0f
        while (isActive) {
            val listening = _state.value.isListening
            _state.update { it.copy(waveformAmplitudes = List(40) { i ->
                if (listening) ((1f - abs(i - 20f) / 20f * 0.5f) * (0.3f + Math.random().toFloat() * 0.7f)).coerceIn(0.05f, 1f)
                else (0.08f + 0.06f * sin(t + i * 0.3f)).coerceIn(0.02f, 0.2f)
            }) }
            t += 0.15f; delay(60)
        }
    }

    private fun connectBrain() {
        brain.connect(viewModelScope)
        brain.startPolling(viewModelScope, 5_000L)
        viewModelScope.launch {
            brain.events.collect { e -> when (e) {
                is BrainEvent.Connected    -> { _state.update { it.copy(brainOnline = true, groqOnline = true) }; addLog("Brain connected", "ok"); typeText("Brain online."); stopIdle() }
                is BrainEvent.Disconnected -> { _state.update { it.copy(brainOnline = false) }; addLog("Reconnecting...", "warn"); startIdle() }
                is BrainEvent.NodeUpdated  -> updateNode(e.node)
                is BrainEvent.ChatReply    -> appendText(e.text)
                is BrainEvent.LogEntry     -> addLog(e.message, e.level)
                is BrainEvent.Error        -> addLog("Error: ${e.cause.message}", "err")
            } }
        }
        startIdle()
    }

    private fun updateNode(n: ApiNode) {
        _state.update { state ->
            val m = n.metrics; val ex = state.nodes.firstOrNull { it.id == n.node_id }
            val updated = NodeInfo(n.node_id, n.name, ex?.ip ?: "", if (n.name.contains("tr")) "BRAIN" else "NODE", n.status == "online",
                m?.cpu_percent?.toInt() ?: ex?.cpu ?: 0,
                if (m != null && m.ram_total_gb > 0) (m.ram_used_gb / m.ram_total_gb * 100).toInt() else ex?.ram ?: 0, ex?.ping ?: 0)
            val nodes = if (state.nodes.any { it.id == n.node_id }) state.nodes.map { if (it.id == n.node_id) updated else it } else state.nodes + updated
            val ib = updated.role == "BRAIN"
            state.copy(nodes = nodes, nodesOnline = nodes.count { it.online },
                brainCpu = if (ib) m?.cpu_percent ?: state.brainCpu else state.brainCpu,
                brainRam = if (ib && m != null && m.ram_total_gb > 0) m.ram_used_gb / m.ram_total_gb * 100 else state.brainRam,
                brainNet = if (ib) (m?.net_sent_mb ?: 0f).coerceIn(0f, 100f) else state.brainNet)
        }
    }

    fun onInputChange(text: String) = _state.update { it.copy(inputText = text) }

    fun sendChat() {
        val raw = _state.value.inputText.trim(); if (raw.isEmpty()) return
        // Respond to its name: "Vision, what's the time" / "ویژن ساعت چنده" — strip the
        // leading wake-name (user-configurable) so the rest is handled normally.
        val msg = stripWakeName(raw)
        _state.update { it.copy(inputText = "", jarvisOutput = "") }; stopIdle(); addLog("You: $raw", "info")
        viewModelScope.launch {
            // P5 agentic v1: device commands ("open camera", "battery", …) run
            // locally with zero latency and zero data leaving the phone.
            interpreter.tryHandle(msg)?.let { reply ->
                typeText(reply); speak(reply); addLog("Local: command", "ok"); return@launch
            }
            typeText("Processing...")
            // Only round-trip the brain when it is actually reachable; otherwise a
            // dead localhost:7799 socket stalls every message 8-15s before timing
            // out. When offline we go straight to the cloud path below.
            if (_state.value.brainOnline) {
                val handled = brain.chat(msg)
                    .onSuccess { resp -> typeText(resp.response); speak(resp.response); addLog("Brain: ${resp.duration_ms}ms", "ok") }
                    .isSuccess
                if (handled) return@launch
            }
            // P4.5 trust gate: SOVEREIGN (0) means nothing leaves the device.
            if (settings.trustLevel.value == 0) {
                typeText("Brain unreachable — cloud disabled by SOVEREIGN trust level.")
                addLog("Cloud blocked (trust)", "warn"); return@launch
            }
            // Standalone path: no brain paired/reachable -> cloud providers
            cloud.chat(msg)
                .onSuccess { r -> typeText(r.text); speak(r.text); addLog("Cloud: ${r.provider.displayName}", "ok") }
                .onFailure { e -> typeText("Error: ${e.message}"); addLog("Failed", "err") }
        }
    }

    /** Drops a leading "<name>[,/ ]" so addressing Vision by its (configurable) name works. */
    private fun stripWakeName(text: String): String {
        val name = settings.personaName.value.trim()
        if (name.isEmpty()) return text
        val candidates = listOf(name, "ویژن", "vision")
        var out = text
        for (n in candidates) {
            if (out.startsWith(n, ignoreCase = true)) {
                out = out.substring(n.length).trimStart(' ', ',', '،', '.', '!', '?', ':')
                break
            }
        }
        return out.ifBlank { text }
    }

    fun toggleListening() {
        if (!settings.voiceEnabled.value && !_state.value.isListening) {
            addLog("Voice input disabled in SYSTEM CONFIG", "warn"); return
        }
        val now = !_state.value.isListening
        _state.update { it.copy(isListening = now) }
        if (now) {
            addLog("Voice on", "ok"); typeText("Listening...")
            // Recognizer callbacks may arrive on any thread — hop to Main.immediate
            // before touching state (review finding HIGH-1).
            voice.startListening(
                onResult = { heard ->
                    viewModelScope.launch(Dispatchers.Main.immediate) {
                        _state.update { it.copy(inputText = heard) }
                        addLog("Heard: $heard", "info")
                        sendChat()
                    }
                },
                onEnd = {
                    viewModelScope.launch(Dispatchers.Main.immediate) {
                        _state.update { it.copy(isListening = false) }
                    }
                },
            )
        } else {
            voice.stopListening(); addLog("Voice off", "info")
        }
    }

    /** TTS honors the settings toggle. */
    private fun speak(text: String) { if (settings.ttsEnabled.value) voice.speak(text) }

    private fun typeText(text: String) { typeJob?.cancel(); typeJob = viewModelScope.launch { _state.update { it.copy(jarvisOutput = "") }; var cur = ""; for (ch in text) { cur += ch; _state.update { it.copy(jarvisOutput = cur) }; delay(28L + (0..18).random()) } } }
    private fun appendText(d: String) = _state.update { it.copy(jarvisOutput = it.jarvisOutput + d) }
    private fun startIdle() { if (idleJob?.isActive == true) return; idleJob = viewModelScope.launch { var i = 0; while (isActive) { typeText(IDLE[i++ % IDLE.size]); delay(4_500) } } }
    private fun stopIdle() { idleJob?.cancel(); idleJob = null }
    private fun addLog(msg: String, level: String) { val lv = when (level) { "ok" -> EventLevel.OK; "warn" -> EventLevel.WARN; "err" -> EventLevel.ERR; else -> EventLevel.INFO }; _state.update { it.copy(eventLog = (it.eventLog + LogEvent(logFmt.format(Date()), msg, lv)).takeLast(50)) } }
    override fun onCleared() { super.onCleared(); voice.release(); brain.close() }
}
