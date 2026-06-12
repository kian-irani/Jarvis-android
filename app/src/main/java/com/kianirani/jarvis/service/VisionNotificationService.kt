package com.kianirani.jarvis.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Phase 7.5 — read access to active notifications so Vision can answer
 * "what are my notifications?". The user grants Notification access once in
 * settings; tools read the live instance. We only read title/text on demand —
 * nothing is persisted.
 */
class VisionNotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        super.onListenerDisconnected()
    }

    /** Current notifications as "Title: text" lines, most recent last. */
    fun recent(limit: Int = 8): List<String> =
        runCatching {
            activeNotifications
                ?.sortedBy { it.postTime }
                ?.mapNotNull { it.summary() }
                ?.takeLast(limit)
                ?: emptyList()
        }.getOrDefault(emptyList())

    private fun StatusBarNotification.summary(): String? {
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        if (title.isNullOrEmpty() && text.isNullOrEmpty()) return null
        return listOfNotNull(title, text).filter { it.isNotEmpty() }.joinToString(": ")
    }

    companion object {
        @Volatile
        var instance: VisionNotificationService? = null
            private set

        val isEnabled: Boolean get() = instance != null
    }
}
