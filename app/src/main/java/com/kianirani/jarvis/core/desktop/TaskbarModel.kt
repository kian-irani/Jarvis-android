package com.kianirani.jarvis.core.desktop

/**
 * DS-WIN3 — the AI taskbar/dock model (PRD §, "AI taskbar/dock — جایگزینِ taskbar"). The pure
 * state of the desktop dock: the open windows plus the user's pinned apps, merged into the
 * ordered entries the dock renders (pinned first, then running-not-pinned), with the active one
 * marked and duplicate windows of one app grouped. The Compose-MP dock renders these; Win32
 * `EnumWindows` feeds the [DesktopWindow]s. Pure → JVM-tested.
 */
data class DesktopWindow(val handle: Long, val appId: String, val title: String, val focused: Boolean = false)

/** One dock slot: an app, whether it's pinned, its open windows, and if it's the active app. */
data class TaskbarEntry(
    val appId: String,
    val pinned: Boolean,
    val windows: List<DesktopWindow>,
    val active: Boolean,
) {
    val running: Boolean get() = windows.isNotEmpty()
}

object TaskbarModel {

    /**
     * Build the dock entries from [pinned] app ids and the live [windows]. Order: pinned apps in
     * their pin order first, then running apps that aren't pinned (in first-seen order). An app's
     * windows are grouped; the entry is [TaskbarEntry.active] if any of its windows is focused.
     */
    fun entries(pinned: List<String>, windows: List<DesktopWindow>): List<TaskbarEntry> {
        val byApp = windows.groupBy { it.appId }
        val seenRunning = windows.map { it.appId }.distinct()
        val orderedApps = pinned + seenRunning.filter { it !in pinned }
        return orderedApps.map { appId ->
            val wins = byApp[appId].orEmpty()
            TaskbarEntry(
                appId = appId,
                pinned = appId in pinned,
                windows = wins,
                active = wins.any { it.focused },
            )
        }
    }

    /** The currently focused window across the desktop, or null. */
    fun focused(windows: List<DesktopWindow>): DesktopWindow? = windows.firstOrNull { it.focused }
}
