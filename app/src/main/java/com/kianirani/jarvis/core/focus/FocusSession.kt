package com.kianirani.jarvis.core.focus

/**
 * Focus Mode (PRD §, "Focus Mode — محدودسازی نوتیف/اپ‌ها"). A timed focus session: for its
 * duration, notifications from non-allowed apps are muted and "distracting" apps are flagged for
 * blocking. Pure (the caller supplies "now") → JVM-tested; muting a notification / closing an app
 * is the device half (NotificationListener + Accessibility). Distinct from the desktop
 * [com.kianirani.jarvis.core.desktop.FocusMode]: this is the Android, time-boxed session.
 */
data class FocusSession(
    val startMillis: Long,
    val durationMillis: Long,
    /** Apps allowed to interrupt during focus (calendar, calls). Empty = mute all. */
    val allowedApps: Set<String> = emptySet(),
    /** Apps to actively keep the user out of (social, games). */
    val blockedApps: Set<String> = emptySet(),
)

object FocusManager {

    /** Is the session running at [now]? */
    fun isActive(session: FocusSession, now: Long): Boolean =
        now in session.startMillis until (session.startMillis + session.durationMillis)

    /** Milliseconds left in the session at [now] (0 once it ends). */
    fun remainingMillis(session: FocusSession, now: Long): Long =
        (session.startMillis + session.durationMillis - now).coerceAtLeast(0)

    /** Should a notification from [appId] be shown? Always shown when the session is over. */
    fun allowsNotification(session: FocusSession, appId: String, now: Long): Boolean =
        !isActive(session, now) || appId in session.allowedApps

    /** Should opening [appId] be blocked (a distracting app during focus)? */
    fun blocksApp(session: FocusSession, appId: String, now: Long): Boolean =
        isActive(session, now) && appId in session.blockedApps
}
