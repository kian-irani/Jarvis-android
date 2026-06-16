package com.kianirani.jarvis.ui.screen.home

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kianirani.jarvis.brain.data.MemoryRepository
import com.kianirani.jarvis.brain.data.TaskRepository
import com.kianirani.jarvis.data.agent.AgentRegistry
import com.kianirani.jarvis.data.settings.QuickActionsStore
import com.kianirani.jarvis.data.settings.VisionSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Live home stats fed into the stats row + widgets card. */
data class HomeStats(
    val tasks: Int = 0,
    val recentTask: String? = null,
    val memories: Int = 0,
    val battery: Int = -1,
    val freeRamMb: Int = -1,
)

/**
 * Home dashboard data (v12). Owns the Agents registry, quick-action order,
 * persona name (greeting) and derived device/brain stats. Command bar, clock,
 * voice and chat stay in [com.kianirani.jarvis.ui.screen.hud.HudViewModel] — the
 * home view reuses that VM rather than duplicating its logic.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val agents: AgentRegistry,
    val quickActions: QuickActionsStore,
    private val tasks: TaskRepository,
    private val memory: MemoryRepository,
    private val settings: VisionSettings,
) : ViewModel() {

    val agentStates = agents.states
    val personaName: StateFlow<String> = settings.personaName

    private val _stats = MutableStateFlow(HomeStats())
    val stats: StateFlow<HomeStats> = _stats.asStateFlow()

    fun refresh() {
        agents.refresh()
        viewModelScope.launch { _stats.value = compute() }
    }

    private suspend fun compute(): HomeStats = withContext(Dispatchers.IO) {
        val taskCount = runCatching { tasks.pendingCount() }.getOrDefault(0)
        val recent = runCatching { tasks.nextPending()?.kind }.getOrNull()
        val mem = runCatching { memory.count() }.getOrDefault(0)
        val battery = runCatching {
            (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }.getOrDefault(-1)
        val freeRam = runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            (mi.availMem / (1024 * 1024)).toInt()
        }.getOrDefault(-1)
        HomeStats(taskCount, recent, mem, battery, freeRam)
    }
}
