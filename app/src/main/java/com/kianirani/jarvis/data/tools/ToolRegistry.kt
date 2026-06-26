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
    call: CallTool,
    sms: SmsTool,
    web: WebTool,
    uiAutomation: UiAutomationTool,
) {
    // call/sms first: they execute a real action (and report honestly) before any
    // looser matcher or the LLM can pretend the action happened (v20 fix). web/uiAutomation
    // last — they only claim explicit URL/search/YouTube or click/scroll/type phrasings the
    // others don't, so they never hijack a plain request.
    private val tools: List<Tool> =
        listOf(call, sms, flashlight, deviceSettings, navigation, notifications, web, uiAutomation)

    /** Ids of registered tools — useful for diagnostics / future tool-calling. */
    val ids: List<String> get() = tools.map { it.id }

    fun dispatch(message: String): String? {
        // Pass the ORIGINAL-case (trimmed) message through: each tool lowercases internally for
        // matching but extracts content (an SMS body, a search query, text to type) from the
        // original, so capitalization is preserved end to end ("text Ali: See you at 5PM" used to
        // become "see you at 5pm"). Tools are case-insensitive matchers, so this is safe.
        val m = message.trim()
        if (m.isEmpty()) return null
        return tools.firstOrNull { it.matches(m) }?.run(m)?.reply
    }
}
