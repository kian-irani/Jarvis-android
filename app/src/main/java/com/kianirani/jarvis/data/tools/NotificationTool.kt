package com.kianirani.jarvis.data.tools

import com.kianirani.jarvis.service.VisionNotificationService
import javax.inject.Inject
import javax.inject.Singleton

/** Phase 7.5 — reads current notifications when the user asks (EN/FA). */
@Singleton
class NotificationTool @Inject constructor() : Tool {
    override val id = "notifications"

    override fun matches(message: String): Boolean = message.lowercase().let { m ->
        m.contains("notification") || m.contains("نوتیف") ||
            m.contains("اعلان") || m.contains("اعلان‌ها")
    }

    override fun run(message: String): ToolResult {
        val svc = VisionNotificationService.instance
            ?: return ToolResult("Grant notification access first: SYSTEM CONFIG → Notification Access.")
        val list = svc.recent(8)
        return ToolResult(
            if (list.isEmpty()) "No notifications right now."
            else "You have ${list.size} notification(s):\n" + list.joinToString("\n") { "• $it" },
        )
    }
}
