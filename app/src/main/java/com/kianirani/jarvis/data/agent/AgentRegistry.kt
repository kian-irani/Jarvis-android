package com.kianirani.jarvis.data.agent

import android.content.Context
import android.provider.Settings
import com.kianirani.jarvis.data.ai.AiProviderStore
import com.kianirani.jarvis.data.repository.BrainRepository
import com.kianirani.jarvis.data.tools.ToolRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Layer 2 registry (v12, 2026-06-14). Single source of truth for the four
 * agents shown on the home panel and managed on the Agents screen.
 *
 * Status is **derived from real capabilities** (not faked):
 * - Research  → an AI provider key is configured ([AiProviderStore.configured])
 * - Automation→ tools are registered ([ToolRegistry.ids])
 * - Developer → the brain is connected ([BrainRepository.connected])
 * - Device    → the Vision accessibility service is enabled
 *
 * Trust level + enabled flag are user choices, persisted in SharedPreferences
 * and surviving restarts. Call [refresh] when a screen appears to recompute
 * statuses (accessibility / brain / keys can change while the app runs).
 */
@Singleton
class AgentRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val providers: AiProviderStore,
    private val tools: ToolRegistry,
    private val brain: BrainRepository,
) {
    private val prefs = context.getSharedPreferences("vision_agents", Context.MODE_PRIVATE)

    private val _states = MutableStateFlow(compute())
    val states: StateFlow<List<AgentState>> = _states.asStateFlow()

    /** Mock recent activity — wired to a real audit log in a later phase. */
    val history: List<AgentAction> = listOf(
        AgentAction(AgentId.DEVELOPER, "now", "Brain-Lite health check passed"),
        AgentAction(AgentId.AUTOMATION, "2m", "Ran device tool: flashlight"),
        AgentAction(AgentId.RESEARCH, "14m", "Answered a cloud query via Groq"),
        AgentAction(AgentId.DEVICE, "1h", "Executed: navigate home"),
    )

    fun refresh() { _states.value = compute() }

    val activeCount: Int
        get() = _states.value.count { it.status == AgentStatus.ACTIVE || it.status == AgentStatus.WORKING }

    fun trust(id: AgentId): TrustLevel = runCatching {
        TrustLevel.valueOf(prefs.getString(keyTrust(id), defaultTrust(id).name)!!)
    }.getOrDefault(defaultTrust(id))

    fun enabled(id: AgentId): Boolean = prefs.getBoolean(keyEnabled(id), true)

    fun setTrust(id: AgentId, level: TrustLevel) {
        prefs.edit().putString(keyTrust(id), level.name).apply()
        refresh()
    }

    fun setEnabled(id: AgentId, on: Boolean) {
        prefs.edit().putBoolean(keyEnabled(id), on).apply()
        refresh()
    }

    private fun compute(): List<AgentState> = AgentId.entries.map { id ->
        val on = enabled(id)
        val status = if (!on) {
            AgentStatus.OFF
        } else {
            val capable = when (id) {
                AgentId.RESEARCH -> providers.configured().isNotEmpty()
                AgentId.AUTOMATION -> tools.ids.isNotEmpty()
                AgentId.DEVELOPER -> brain.connected.value
                AgentId.DEVICE -> isAccessibilityEnabled()
            }
            if (capable) AgentStatus.ACTIVE else AgentStatus.IDLE
        }
        AgentState(id, status, trust(id), on)
    }

    private fun isAccessibilityEnabled(): Boolean = runCatching {
        val flat = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        flat.contains(context.packageName + "/")
    }.getOrDefault(false)

    private fun defaultTrust(id: AgentId) = when (id) {
        AgentId.DEVICE -> TrustLevel.READ // device control is sensitive — observe by default
        else -> TrustLevel.SUGGEST
    }

    private fun keyTrust(id: AgentId) = "trust_${id.name}"
    private fun keyEnabled(id: AgentId) = "enabled_${id.name}"
}
