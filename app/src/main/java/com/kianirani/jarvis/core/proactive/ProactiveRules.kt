package com.kianirani.jarvis.core.proactive

import com.kianirani.jarvis.core.event.VisionEvent

/**
 * C0.3 — proactive suggestion rules (PRD §, "بر اساس اپ/زمان/رویداد پیشنهاد بده"). Maps a
 * [VisionEvent] from the bus (VCF-R2 / DS-BG3) to a candidate [Suggestion] — e.g. an important
 * notification → "Want me to handle it?", a browser foreground → "Summarize this page?". The
 * candidate then goes through [SuggestionEngine] for the anti-nag gating. Pure → JVM-tested; the
 * EventBus subscription that feeds it is the runtime half.
 */
object ProactiveRules {

    /** A suggestion candidate for [event] at [now], or null if the event isn't actionable. */
    fun suggestionFor(event: VisionEvent, now: Long): Suggestion? = when (event) {
        is VisionEvent.Custom -> when (event.name) {
            "notification_important" -> Suggestion(
                id = "notif:${event.data}", kind = "notification",
                text = "You have an important notification from ${event.data}. Want me to handle it?",
                relevance = 0.8f, createdAtMillis = now,
            )
            else -> null
        }
        is VisionEvent.AppOpened -> browserSuggestion(event.packageName, now)
        else -> null
    }

    private fun browserSuggestion(pkg: String, now: Long): Suggestion? {
        val browsers = listOf("chrome", "firefox", "browser", "brave", "opera")
        if (browsers.none { pkg.lowercase().contains(it) }) return null
        return Suggestion(
            id = "browse:$pkg", kind = "writing", // reuse a low-frequency kind for the panel
            text = "Want me to summarize this page?",
            relevance = 0.6f, createdAtMillis = now,
        )
    }
}
