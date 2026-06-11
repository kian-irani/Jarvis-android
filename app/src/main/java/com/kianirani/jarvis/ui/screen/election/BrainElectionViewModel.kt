package com.kianirani.jarvis.ui.screen.election

import androidx.lifecycle.ViewModel
import com.kianirani.jarvis.brain.score.BrainScoreCalculator
import com.kianirani.jarvis.brain.score.DeviceMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class BrainNodeUi(
    val id: String,
    val name: String,
    val score: Int,
    val online: Boolean,
    val isElected: Boolean,
    val detail: String,
)

data class BrainElectionState(
    val nodes: List<BrainNodeUi> = emptyList(),
    val manualOverrideId: String? = null,
)

/**
 * Holds mesh metrics and runs [BrainScoreCalculator] election. Metrics arrive
 * from the node registry (follow-up: live wiring); seeded with local device
 * until heartbeats stream in.
 */
@HiltViewModel
class BrainElectionViewModel @Inject constructor() : ViewModel() {

    private val metrics = MutableStateFlow<Map<String, DeviceMetrics>>(
        mapOf("local" to DeviceMetrics(ramFreeGb = 2.0, cpuCores = 8, batteryPercent = 100))
    )
    private val names = mutableMapOf("local" to "this device")
    private val online = mutableSetOf("local")
    private val override = MutableStateFlow<String?>(null)

    private val _state = MutableStateFlow(BrainElectionState())
    val state: StateFlow<BrainElectionState> = _state

    init { recompute() }

    /** Node registry heartbeat entry point. */
    fun onNodeMetrics(id: String, name: String, m: DeviceMetrics, isOnline: Boolean = true) {
        names[id] = name
        if (isOnline) online += id else online -= id
        metrics.update { it + (id to m) }
        recompute()
    }

    /** Tap elected node → clear override (back to auto); tap other node → manual override. */
    fun onNodeTapped(id: String) {
        val electedNow = elect()
        override.update { if (id == electedNow) null else id }
        recompute()
    }

    private fun elect(): String? =
        BrainScoreCalculator.elect(metrics.value.filterKeys { it in online }, override.value)

    private fun recompute() {
        val elected = elect()
        _state.update {
            BrainElectionState(
                nodes = metrics.value.entries
                    .map { (id, m) ->
                        BrainNodeUi(
                            id = id,
                            name = names[id] ?: id,
                            score = BrainScoreCalculator.score(m),
                            online = id in online,
                            isElected = id == elected,
                            detail = "RAM ${m.ramFreeGb}G · ${m.cpuCores}C · BAT ${m.batteryPercent}%" +
                                (if (m.isVps) " · VPS" else "") + (if (m.thermalThrottling) " · THERMAL" else ""),
                        )
                    }
                    .sortedByDescending { it.score },
                manualOverrideId = override.value,
            )
        }
    }
}
