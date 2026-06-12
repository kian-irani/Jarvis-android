package com.kianirani.jarvis.data.tools

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 5 — the agent's tool layer. Holds every [Tool] and routes a message to
 * the first one that matches. New tools are added to the constructor and Hilt
 * wires them automatically. Returns null when nothing matched (flows to AI).
 */
@Singleton
class ToolRegistry @Inject constructor(
    flashlight: FlashlightTool,
    deviceSettings: DeviceSettingsTool,
    navigation: NavigationTool,
    notifications: NotificationTool,
) {
    private val tools: List<Tool> = listOf(flashlight, deviceSettings, navigation, notifications)

    /** Ids of registered tools — useful for diagnostics / future tool-calling. */
    val ids: List<String> get() = tools.map { it.id }

    fun dispatch(message: String): String? {
        val m = message.trim().lowercase()
        if (m.isEmpty()) return null
        return tools.firstOrNull { it.matches(m) }?.run(m)?.reply
    }
}
