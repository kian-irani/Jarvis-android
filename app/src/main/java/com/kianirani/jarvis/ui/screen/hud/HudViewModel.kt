package com.kianirani.jarvis.ui.screen.hud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kianirani.jarvis.data.repository.ApiNode
import com.kianirani.jarvis.data.repository.BrainEvent
import com.kianirani.jarvis.data.repository.BrainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

private val IDLE_TEXTS = listOf(
    "All systems operational. Brain server nominal.",
    "Node mesh: 3 nodes online.",
    "Groq LLaMA-3.3-70B ready.",
    "Awaiting your command."
)

@HiltViewModel
class HudViewModel @Inject constructor(
    private val brain: BrainRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HudUiState())
    val uiState: StateFlow<HudUiState> = _state.asStateFlow()

    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val logFmt  = SimpleDateFormat("HH:mm:ss", Locale.US)
    private var typeJob: Job? = null
    private var idleJob: Job? = null

    init {
        startClock()
        startWaveform()
        connectBrain()
    }

    private fun startClock() = viewModelScope.launch {
        while (isActive) {
            _state.update { it.copy(currentTime = timeFmt.format(Date())) }
            delay(1_000)
        }
    }

    private fun startWaveform() = viewModelScope.launch {
        var t = 0f
        while (isActive) {
            val listening = _state.value.isListening
            val bars = List(40) { i ->
                if (listening) {
                    val dist = abs(i - 20f) / 20f
                    ((1f - dist * 0.5f) * (0.3f + Math.random().toFloat() * 0.7f)).coerceIn(0.05f, 1f)
                } else {
                    (0.08f + 0.06f * sin(t + i * 0.3f)).coerceIn(0.02f, 0.2f)
                }
            }
            _state.update { it.copy(waveformAmplitudes = bars) }
            t += 0.15f
            delay(60)
        }
    }

    private fun connectBrain() {
        brain.connectWebSocket(viewModelScope)
        brain.startPolling(viewModelScope, 5_000L)
        viewModelScope.launch {
            brain.events.collect { event ->
                when (event) {
                    is BrainEvent.Connected -> {
                        _state.update { it.copy(brainOnline = true, groqOnline = true) }
                        addLog("Brain connected", "ok")
                        typeText("Brain online. All systems nominal.")
                        stopIdle()
                    }
                    is BrainEvent.Disconnected -> {
                        _state.update { it.copy(brainOnline = false) }
                        addLog("Disconnected. Reconnecting...", "warn")
                        startIdle()
                    }
                    is BrainEvent.NodeUpdated -> updateNode(event.node)
                    is BrainEvent.ChatReply   -> appendText(event.text)
                    is BrainEvent.LogEntry    -> addLog(event.message, event.level)
                    is BrainEvent.Error       -> addLog("Error: ${event.cause.message}", "err")
                }
            }
        }
        startIdle()
    }

    private fun updateNode(n: ApiNode) {
        _state.update { state ->
            val m = n.metrics
            val existing = state.nodes.firstOrNull { it.id == n.node_id }
            val updated = NodeInfo(
                id     = n.node_id,
                name   = n.name,
                ip     = existing?.ip ?: "",
                role   = if (n.name.contains("tr")) "BRAIN" else "NODE",
                online = n.status == "online",
                cpu    = m?.cpu_percent?.toInt() ?: existing?.cpu ?: 0,
                ram    = if (m != null && m.ram_total_gb > 0)
                             (m.ram_used_gb / m.ram_total_gb * 100).toInt()
                         else existing?.ram ?: 0,
                ping   = existing?.ping ?: 0
            )
            val nodes = if (state.nodes.any { it.id == n.node_id }) {
                state.nodes.map { if (it.id == n.node_id) updated else it }
            } else {
                state.nodes + updated
            }
            val isBrain = updated.role == "BRAIN"
            state.copy(
                nodes       = nodes,
                nodesOnline = nodes.count { it.online },
                brainCpu    = if (isBrain) m?.cpu_percent ?: state.brainCpu else state.brainCpu,
                brainRam    = if (isBrain && m != null && m.ram_total_gb > 0)
                                  m.ram_used_gb / m.ram_total_gb * 100
                              else state.brainRam,
                brainNet    = if (isBrain) (m?.net_sent_mb ?: 0f).coerceIn(0f, 100f) else state.brainNet
            )
        }
    }

    fun onInputChange(text: String) = _state.update { it.copy(inputText = text) }

    fun sendChat() {
        val msg = _state.value.inputText.trim()
        if (msg.isEmpty()) return
        _state.update { it.copy(inputText = "", jarvisOutput = "") }
        stopIdle()
        addLog("You: $msg", "info")
        viewModelScope.launch {
            typeText("Processing...")
            brain.chat(msg)
                .onSuccess { resp ->
                    typeText(resp.response)
                    addLog("AI: ${resp.duration_ms}ms", "ok")
                }
                .onFailure { e ->
                    typeText("Error: ${e.message}")
                    addLog("Failed: ${e.message}", "err")
                }
        }
    }

    fun toggleListening() {
        val now = !_state.value.isListening
        _state.update { it.copy(isListening = now) }
        if (now) {
            addLog("Voice activated", "ok")
            typeText("Listening...")
        } else {
            addLog("Voice off", "info")
        }
    }

    private fun typeText(text: String) {
        typeJob?.cancel()
        typeJob = viewModelScope.launch {
            _state.update { it.copy(jarvisOutput = "") }
            var current = ""
            for (ch in text) {
                current += ch
                _state.update { it.copy(jarvisOutput = current) }
                delay(28L + (0..18).random())
            }
        }
    }

    private fun appendText(delta: String) {
        _state.update { it.copy(jarvisOutput = it.jarvisOutput + delta) }
    }

    private fun startIdle() {
        if (idleJob?.isActive == true) return
        idleJob = viewModelScope.launch {
            var i = 0
            while (isActive) {
                typeText(IDLE_TEXTS[i % IDLE_TEXTS.size])
                i++
                delay(4_500)
            }
        }
    }

    private fun stopIdle() { idleJob?.cancel(); idleJob = null }

    private fun addLog(msg: String, level: String) {
        val lv = when (level) {
            "ok"   -> EventLevel.OK
            "warn" -> EventLevel.WARN
            "err"  -> EventLevel.ERR
            else   -> EventLevel.INFO
        }
        val event = LogEvent(logFmt.format(Date()), msg, lv)
        _state.update { it.copy(eventLog = (it.eventLog + event).takeLast(50)) }
    }

    override fun onCleared() {
        super.onCleared()
        brain.close()
    }
}
