package com.kianirani.jarvis.core.context

import com.kianirani.jarvis.core.protocol.DeviceContext

/**
 * DS-B2 / CTX (PRD §8.2) — turns a [DeviceContext] snapshot (the DS-F2 wire DTO a surface
 * fills from the device: foreground app, battery, network, locale, time-of-day, unread
 * notifications, …) into a compact **system-prompt fragment** the chat/agent injects so
 * answers are *grounded* ("you can see the user is in Maps, on cellular, at 18% battery").
 *
 * Pure & deterministic — sensing the device (Accessibility / BatteryManager / Connectivity)
 * is the surface's job and is opt-in; this object only formats what it's handed. Mirrors
 * [com.kianirani.jarvis.core.memory.MemoryEngine.buildContextWindow]'s `[BLOCK]…[/BLOCK]`
 * convention so the two fragments compose cleanly in one prompt. Empty in → empty out
 * (no wasted tokens, never crashes).
 */
object ContextEngine {

    /** Build the `[CONTEXT]` block, or "" when [ctx] carries nothing worth injecting. */
    fun buildContextBlock(ctx: DeviceContext?): String {
        if (ctx == null) return ""
        val lines = buildList {
            ctx.foregroundApp?.takeIf { it.isNotBlank() }?.let { add("Foreground app: $it") }
            batteryLine(ctx)?.let { add(it) }
            ctx.network?.takeIf { it.isNotBlank() }?.let { add("Network: $it") }
            ctx.locale?.takeIf { it.isNotBlank() }?.let { add("Locale: $it") }
            ctx.timeOfDay?.takeIf { it.isNotBlank() }?.let { add("Time of day: $it") }
            ctx.unreadNotifications?.takeIf { it > 0 }?.let { add("Unread notifications: $it") }
            ctx.extras.forEach { (k, v) -> if (k.isNotBlank() && v.isNotBlank()) add("$k: $v") }
        }
        if (lines.isEmpty()) return ""
        return "\n\n[CONTEXT — the user's current device state]\n" +
            lines.joinToString("\n") { "• $it" } +
            "\n[/CONTEXT]"
    }

    private fun batteryLine(ctx: DeviceContext): String? {
        val pct = ctx.batteryPercent ?: return null
        val clamped = pct.coerceIn(0, 100)
        val charging = when (ctx.charging) {
            true -> ", charging"
            false -> ""
            null -> ""
        }
        return "Battery: $clamped%$charging"
    }
}
