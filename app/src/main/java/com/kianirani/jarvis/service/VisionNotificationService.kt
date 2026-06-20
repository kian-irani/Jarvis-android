package com.kianirani.jarvis.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.kianirani.jarvis.core.event.VisionEvent
import com.kianirani.jarvis.core.event.VisionEventBus
import com.kianirani.jarvis.core.notif.NotificationInfo
import com.kianirani.jarvis.core.notif.NotificationTriage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
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
@AndroidEntryPoint
class VisionNotificationService : NotificationListenerService() {

    // DS-BG3 — publish notification events onto the shared bus so proactive triggers can react.
    @Inject lateinit var eventBus: VisionEventBus

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

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshBadges()
        // DS-BG3 / B4 — triage the new notification; only emit an event for IMPORTANT ones so
        // the proactive layer (PAS) isn't spammed by promos/social noise.
        sbn?.let { n ->
            val info = NotificationInfo(
                pkg = n.packageName ?: "",
                title = n.notification?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "",
                text = n.notification?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "",
                category = n.notification?.category ?: "",
                ongoing = n.isOngoing,
            )
            if (NotificationTriage.classify(info) == com.kianirani.jarvis.core.notif.Importance.IMPORTANT) {
                eventBus.tryEmit(VisionEvent.Custom("notification_important", info.pkg))
            }
        }
    }

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

    /** DS-BG3 — dismiss a notification by its key (real "clear that notification"). */
    fun dismiss(key: String): Boolean = runCatching { cancelNotification(key); true }.getOrDefault(false)

    /** Dismiss every clearable notification ("clear all"). */
    fun dismissAll(): Boolean = runCatching { cancelAllNotifications(); true }.getOrDefault(false)

    /** Keys of the active clearable notifications (so a tool can target one to dismiss/reply). */
    fun activeKeys(): List<String> =
        runCatching { activeNotifications?.filter { it.isClearable }?.map { it.key } ?: emptyList() }.getOrDefault(emptyList())

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
