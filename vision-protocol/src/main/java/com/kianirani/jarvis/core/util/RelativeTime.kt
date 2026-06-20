package com.kianirani.jarvis.core.util

/**
 * Pure relative-time labels ("just now", "5m ago", "3d ago") for compact list
 * metadata. No Android/Locale deps → unit-tested; future timestamps clamp to "just now".
 */
object RelativeTime {
    fun ago(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val deltaMs = (nowMs - timestampMs).coerceAtLeast(0L)
        val sec = deltaMs / 1000
        val min = sec / 60
        val hr = min / 60
        val day = hr / 24
        return when {
            sec < 45 -> "just now"
            min < 60 -> "${min.coerceAtLeast(1)}m ago"
            hr < 24 -> "${hr}h ago"
            day < 7 -> "${day}d ago"
            day < 365 -> "${day / 7}w ago"
            else -> "${day / 365}y ago"
        }
    }
}
