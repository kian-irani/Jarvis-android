package com.kianirani.jarvis.ui.screen.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * Wizard flow state. Step 2 CONNECT performs the brain handshake (follow-up:
 * real mDNS/QR/token transport); simulated success path for now so the UI
 * flow is testable end-to-end.
 */
@HiltViewModel
class SetupWizardViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(SetupWizardState())
    val state: StateFlow<SetupWizardState> = _state

    fun onDeviceNameChanged(v: String) = _state.update { it.copy(deviceName = v.take(32)) }
    fun onDiscoveryMethodSelected(m: DiscoveryMethod) = _state.update { it.copy(discoveryMethod = m) }
    fun onTokenChanged(v: String) = _state.update { it.copy(token = v.trim()) }

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
            delay(600) // handshake placeholder — replaced by real discovery transport
            _state.update { it.copy(connectStatus = ConnectStatus.OK, step = 3) }
        }
    }
}
