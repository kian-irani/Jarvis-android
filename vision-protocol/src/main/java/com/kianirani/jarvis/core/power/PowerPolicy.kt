package com.kianirani.jarvis.core.power

/**
 * DS-W6 / DS-BG4 — power & low-memory policy (PRD §, "notifِ کم‌اولویت، idle-collapse،
 * Doze-safe، wake-word کم‌مصرف، degrade در حافظه‌ی کم"). A pure decision layer the floating
 * widget + background services consult to stay light: pick a [PowerMode] from the live battery/
 * memory state, then derive whether to collapse to a dot, run the wake word, or drop
 * animations. Pure → JVM-tested; reading BatteryManager/ActivityManager and applying the
 * effects (collapse, disable listeners) are the on-device half.
 */
data class PowerState(
    val batteryPercent: Int,
    val charging: Boolean,
    val lowMemory: Boolean = false,
    val idleMillis: Long = 0L,
)

enum class PowerMode { FULL, CONSERVE, MINIMAL }

object PowerPolicy {

    const val MINIMAL_BATTERY = 15
    const val CONSERVE_BATTERY = 30
    const val IDLE_COLLAPSE_MS = 8_000L

    /**
     * MINIMAL on a critically low battery (unplugged) or under memory pressure; CONSERVE on a
     * low battery (unplugged); FULL otherwise. Charging always keeps FULL on the battery axis,
     * but memory pressure still forces MINIMAL (an OOM doesn't care that you're plugged in).
     */
    fun mode(state: PowerState): PowerMode = when {
        state.lowMemory -> PowerMode.MINIMAL
        state.charging -> PowerMode.FULL
        state.batteryPercent <= MINIMAL_BATTERY -> PowerMode.MINIMAL
        state.batteryPercent <= CONSERVE_BATTERY -> PowerMode.CONSERVE
        else -> PowerMode.FULL
    }

    /** Collapse the orb to a dot when idle long enough, or whenever we're conserving power. */
    fun shouldCollapseToDot(state: PowerState): Boolean =
        state.idleMillis >= IDLE_COLLAPSE_MS || mode(state) != PowerMode.FULL

    /** Keep the always-listening wake word on unless we're in MINIMAL (its budget is too high). */
    fun wakeWordEnabled(state: PowerState): Boolean = mode(state) != PowerMode.MINIMAL

    /** Drop non-essential animations under memory pressure or MINIMAL power. */
    fun degradeAnimations(state: PowerState): Boolean = state.lowMemory || mode(state) == PowerMode.MINIMAL
}
