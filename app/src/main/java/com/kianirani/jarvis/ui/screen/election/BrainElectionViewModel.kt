package com.kianirani.jarvis.ui.screen.election

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kianirani.jarvis.brain.data.NodeRepository
import com.kianirani.jarvis.brain.score.BrainScoreCalculator
import com.kianirani.jarvis.brain.score.DeviceMetrics
import com.kianirani.jarvis.brain.score.LocalDeviceMetricsProvider
import com.kianirani.jarvis.brain.score.NodeMetricsCodec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
 * Runs [BrainScoreCalculator] election over live node-registry data: observes
 * the Room `nodes` table (heartbeats land there via POST /nodes), decodes each
 * row's capabilities into [DeviceMetrics], and treats rows with a stale
 * `last_seen` as offline. The local device is always a candidate, seeded from
 * [LocalDeviceMetricsProvider].
 */
@HiltViewModel
class BrainElectionViewModel(
    nodeRepository: NodeRepository,
    localMetrics: LocalDeviceMetricsProvider,
    private val clock: () -> Long,
) : ViewModel() {

    @Inject constructor(nodeRepository: NodeRepository, localMetrics: LocalDeviceMetricsProvider) :
        this(nodeRepository, localMetrics, System::currentTimeMillis)

    companion object {
        /** Heartbeats arrive every ~30s; 3 missed beats ⇒ offline. */
        const val ONLINE_WINDOW_MS: Long = 90_000
        const val LOCAL_ID = "local"
    }

    private val metrics = MutableStateFlow(mapOf(LOCAL_ID to localMetrics.current()))
    private val names = mutableMapOf(LOCAL_ID to "this device")
    private val online = mutableSetOf(LOCAL_ID)
    private val override = MutableStateFlow<String?>(null)

    private val _state = MutableStateFlow(BrainElectionState())
    val state: StateFlow<BrainElectionState> = _state

    init {
        recompute()
        viewModelScope.launch {
            nodeRepository.observe().collect { entities ->
                entities.forEach { e ->
                    onNodeMetrics(
                        id = e.id,
                        name = e.name,
                        m = NodeMetricsCodec.decode(e.capabilities)
                            ?: DeviceMetrics(ramFreeGb = 0.0, cpuCores = 0, batteryPercent = 0),
                        isOnline = clock() - e.last_seen <= ONLINE_WINDOW_MS,
                    )
                }
            }
        }
    }

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
