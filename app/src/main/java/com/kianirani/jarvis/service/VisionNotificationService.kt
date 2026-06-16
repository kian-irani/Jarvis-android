package com.kianirani.jarvis.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase 7.5 — read access to active notifications so Vision can answer
 * "what are my notifications?". The user grants Notification access once in
 * settings; tools read the live instance. We only read title/text on demand —
 * nothing is persisted.
 *
 * RD10 (2026-06-16): also publishes a live per-package **badge count** of
 * clearable notifications so the App Drawer can show launcher-style unread
 * badges. Counts are in-memory only and reset when access is revoked.
 */
class VisionNotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        refreshBadges()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        _badges.value = emptyMap()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refreshBadges()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refreshBadges()

    /** Recompute package -> count of clearable (non-ongoing) notifications. */
    private fun refreshBadges() {
        _badges.value = runCatching {
            activeNotifications
                ?.filter { it.isClearable }
                ?.groupingBy { it.packageName }
                ?.eachCount()
                ?: emptyMap()
        }.getOrDefault(emptyMap())
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

        // Launcher badge counts (package -> unread). Empty until access is granted.
        private val _badges = MutableStateFlow<Map<String, Int>>(emptyMap())
        val badges: StateFlow<Map<String, Int>> = _badges.asStateFlow()
    }
}
