package com.kianirani.jarvis.ui.screen.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kianirani.jarvis.brain.discovery.BrainCandidate
import com.kianirani.jarvis.brain.discovery.BrainHandshake
import com.kianirani.jarvis.brain.discovery.BrainSelectionStore
import com.kianirani.jarvis.brain.discovery.DiscoveryScanner
import com.kianirani.jarvis.brain.discovery.JoinPayload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DiscoveryMethod(val title: String, val subtitle: String) {
    MDNS("AUTO DISCOVERY", "Scan local network (mDNS)"),
    QR("QR CODE", "Scan the code shown by your Brain"),
    TOKEN("PAIRING TOKEN", "Enter token manually"),
}

enum class ConnectStatus { IDLE, CONNECTING, OK, FAILED }

data class SetupWizardState(
    val step: Int = 0,
    val deviceName: String = "",
    val discoveryMethod: DiscoveryMethod = DiscoveryMethod.MDNS,
    val token: String = "",
    /** Parsed when the user pastes/scans a full vision://join URI; null for plain tokens. */
    val joinPayload: JoinPayload? = null,
    /** Live mDNS results while on the discovery step. */
    val candidates: List<BrainCandidate> = emptyList(),
    val selectedCandidate: BrainCandidate? = null,
    val connectStatus: ConnectStatus = ConnectStatus.IDLE,
) {
    val canAdvance: Boolean
        get() = when (step) {
            0 -> deviceName.isNotBlank()
            1 -> discoveryMethod != DiscoveryMethod.TOKEN || token.isNotBlank()
            2 -> connectStatus != ConnectStatus.CONNECTING
            else -> true
        }
}

/**
 * Wizard flow state. The discovery step streams live mDNS [BrainCandidate]s;
 * CONNECT performs a real /health handshake against the join target (payload
 * host or selected candidate). With no known target (plain token, dev) the
 * legacy simulated path keeps the flow usable.
 */
@HiltViewModel
class SetupWizardViewModel @Inject constructor(
    scanner: DiscoveryScanner,
    private val handshake: BrainHandshake,
    private val selectionStore: BrainSelectionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(SetupWizardState())
    val state: StateFlow<SetupWizardState> = _state

    private val scan = scanner.scan { list ->
        _state.update { s ->
            s.copy(
                candidates = list,
                selectedCandidate = s.selectedCandidate?.takeIf { sel -> list.any { it.name == sel.name } },
            )
        }
    }

    fun onDeviceNameChanged(v: String) = _state.update { it.copy(deviceName = v.take(32)) }
    fun onDiscoveryMethodSelected(m: DiscoveryMethod) = _state.update { it.copy(discoveryMethod = m) }
    fun onCandidateSelected(c: BrainCandidate) = _state.update { it.copy(selectedCandidate = c) }

    fun onTokenChanged(v: String) = _state.update {
        val payload = JoinPayload.decode(v)
        it.copy(token = if (payload != null) payload.token else v.trim(), joinPayload = payload)
    }

    fun back() = _state.update { if (it.step > 0) it.copy(step = it.step - 1, connectStatus = ConnectStatus.IDLE) else it }

    fun next() {
        val s = _state.value
        if (!s.canAdvance) return
        when (s.step) {
            2 -> connect()
            else -> _state.update { it.copy(step = it.step + 1) }
        }
    }

    private fun connect() {
        _state.update { it.copy(connectStatus = ConnectStatus.CONNECTING) }
        viewModelScope.launch {
            val s = _state.value
            val target = s.joinPayload?.let { it.host to it.port }
                ?: s.selectedCandidate?.let { it.host to it.port }
            val ok = if (target != null) {
                handshake.check(target.first, target.second)
            } else {
                delay(600) // no known target (plain token / dev) — keep flow usable
                true
            }
            if (ok && target != null) {
                selectionStore.save(
                    s.joinPayload ?: JoinPayload(target.first, target.second, s.token.ifBlank { "mdns" }),
                )
            }
            _state.update {
                if (ok) it.copy(connectStatus = ConnectStatus.OK, step = 3)
                else it.copy(connectStatus = ConnectStatus.FAILED)
            }
        }
    }

    override fun onCleared() {
        runCatching { scan.close() }
        super.onCleared()
    }
}
