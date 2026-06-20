package com.kianirani.jarvis.core.mesh

/**
 * Broadcast / presence (PRD §, "Broadcast — هم‌رسانی وضعیت بین دستگاه‌ها"). The pure presence
 * model: each device broadcasts its status; peers merge the announcements keeping the freshest
 * per device, and a device silent past a grace window is shown offline. Pure (timestamps
 * injected) → JVM-tested; the mesh transport that carries the announcements is the network half.
 */
enum class PresenceStatus { ONLINE, AWAY, BUSY, OFFLINE }

data class Presence(val deviceId: String, val status: PresenceStatus, val updatedMillis: Long)

object DevicePresence {

    const val DEFAULT_AWAY_AFTER_MS = 120_000L // silent this long → shown OFFLINE

    /**
     * Merge incoming [announcements] into [current], keeping the newest presence per device.
     * Order-independent: only the latest [Presence.updatedMillis] per device survives.
     */
    fun merge(current: List<Presence>, announcements: List<Presence>): List<Presence> {
        val latest = HashMap<String, Presence>()
        (current + announcements).forEach { p ->
            val existing = latest[p.deviceId]
            if (existing == null || p.updatedMillis >= existing.updatedMillis) latest[p.deviceId] = p
        }
        return latest.values.sortedBy { it.deviceId }
    }

    /** Effective status at [now]: a device silent past [awayAfterMs] is OFFLINE regardless. */
    fun effectiveStatus(presence: Presence, now: Long, awayAfterMs: Long = DEFAULT_AWAY_AFTER_MS): PresenceStatus =
        if (now - presence.updatedMillis > awayAfterMs) PresenceStatus.OFFLINE else presence.status

    /** Device ids currently reachable (not effectively OFFLINE). */
    fun onlineDevices(presences: List<Presence>, now: Long, awayAfterMs: Long = DEFAULT_AWAY_AFTER_MS): List<String> =
        presences.filter { effectiveStatus(it, now, awayAfterMs) != PresenceStatus.OFFLINE }.map { it.deviceId }
}
