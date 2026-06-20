package com.kianirani.jarvis.core.desktop

/**
 * DS-WIN5 — multi-desktop + Focus mode (PRD §, "پنهان‌کردنِ حواس‌پرتی توسط AI"). The pure policy
 * deciding what the user is allowed to see while focusing: only allow-listed apps' windows stay
 * visible, distracting notifications are muted, and an optional per-virtual-desktop scope hides
 * windows that belong to other desktops. The desktop shell enforces the verdicts (hide window,
 * suppress toast); this object is the decision brain. Pure → JVM-tested.
 */
data class FocusPolicy(
    val active: Boolean = false,
    /** App ids allowed during focus; empty + active = allow nothing but the foreground task. */
    val allowedApps: Set<String> = emptySet(),
    /** Allow notifications from these apps even during focus (e.g. calendar, calls). */
    val allowedNotifApps: Set<String> = emptySet(),
    /** When set, only windows on this virtual desktop are shown. */
    val desktopScope: Int? = null,
)

object FocusMode {

    /** Should [window] be visible under [policy]? When focus is off, everything is visible. */
    fun isWindowVisible(window: DesktopWindow, policy: FocusPolicy, windowDesktop: Int? = null): Boolean {
        if (!policy.active) return true
        if (policy.desktopScope != null && windowDesktop != null && windowDesktop != policy.desktopScope) return false
        if (window.focused) return true // never hide what the user is actively using
        return policy.allowedApps.isEmpty() || window.appId in policy.allowedApps
    }

    /** Should a notification from [appId] be shown under [policy]? */
    fun isNotificationAllowed(appId: String, policy: FocusPolicy): Boolean =
        !policy.active || appId in policy.allowedNotifApps

    /** Visible subset of [windows] under [policy] (desktop scope optional via [desktopOf]). */
    fun visibleWindows(
        windows: List<DesktopWindow>,
        policy: FocusPolicy,
        desktopOf: (DesktopWindow) -> Int? = { null },
    ): List<DesktopWindow> = windows.filter { isWindowVisible(it, policy, desktopOf(it)) }
}
