package com.kianirani.jarvis.data.tools

import com.kianirani.jarvis.service.VisionAccessibilityService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 7.5 — Home / Back / Recents / Lock via the accessibility service.
 * If the service isn't enabled yet, returns a clear hint instead of failing.
 */
@Singleton
class NavigationTool @Inject constructor() : Tool {
    override val id = "navigation"

    override fun matches(message: String): Boolean = actionFor(message) != null

    override fun run(message: String): ToolResult {
        val (action, reply) = actionFor(message) ?: return ToolResult("Unknown navigation command.")
        val svc = VisionAccessibilityService.instance
            ?: return ToolResult("Turn on Vision Device Control first: SYSTEM CONFIG → Device Control.")
        val ok = when (action) {
            "home" -> svc.goHome()
            "back" -> svc.goBack()
            "recents" -> svc.openRecents()
            "lock" -> svc.lockScreen()
            else -> false
        }
        return ToolResult(if (ok) reply else "Couldn't do that right now.")
    }

    private fun actionFor(raw: String): Pair<String, String>? = raw.lowercase().let { m -> when {
        m == "home" || m == "go home" || m.contains("برو خانه") || m.contains("صفحه اصلی") || m.contains("هوم") ->
            "home" to "Going home."
        m == "back" || m == "go back" || m.contains("برگرد") || m.contains("بازگشت") ->
            "back" to "Going back."
        m.contains("recent") || m.contains("اخیر") ||
            (m.contains("برنامه") && m.contains("باز")) || m.contains("برنامه‌های باز") ->
            "recents" to "Opening recents."
        m.contains("lock screen") || m == "lock" || m.contains("قفل") ->
            "lock" to "Locking the screen."
        else -> null
    } }
}
