package com.kianirani.jarvis.core.notif

/**
 * B4 — semantic notification triage (Agent-OS §, "تحلیل نوتیف‌ها → Important/Medium/Ignore"). A
 * pure classifier that scores an incoming notification by sender, category, content and ongoing
 * state so Vision can surface what matters and mute the noise. Pure → JVM-tested; reading the
 * live notifications (NotificationListener, MON2) and acting on the verdict are on-device.
 */
enum class Importance { IGNORE, MEDIUM, IMPORTANT }

data class NotificationInfo(
    val pkg: String,
    val title: String,
    val text: String,
    /** Android category, e.g. "msg", "call", "email", "promo", "social", "transport". */
    val category: String = "",
    /** Ongoing (music, navigation, download) — informational, rarely urgent. */
    val ongoing: Boolean = false,
)

/** User-tunable triage knobs. */
data class TriagePolicy(
    val vipSenders: Set<String> = emptySet(),
    val mutedPackages: Set<String> = emptySet(),
    val urgentKeywords: Set<String> = setOf("urgent", "asap", "otp", "code", "verify", "emergency"),
)

object NotificationTriage {

    private val IMPORTANT_CATEGORIES = setOf("call", "msg", "email", "alarm", "event", "reminder")
    private val LOW_CATEGORIES = setOf("promo", "social", "transport", "recommendation", "status")

    /** Classify [n] under [policy]. Muted apps are always ignored; VIP senders and urgent keywords escalate. */
    fun classify(n: NotificationInfo, policy: TriagePolicy = TriagePolicy()): Importance {
        if (n.pkg in policy.mutedPackages) return Importance.IGNORE

        val haystack = "${n.title} ${n.text}".lowercase()
        val fromVip = policy.vipSenders.any { n.title.lowercase().contains(it.lowercase()) }
        val hasUrgent = policy.urgentKeywords.any { haystack.contains(it.lowercase()) }
        if (fromVip || hasUrgent) return Importance.IMPORTANT

        var score = 0
        if (n.category in IMPORTANT_CATEGORIES) score += 2
        if (n.category in LOW_CATEGORIES) score -= 2
        if (n.ongoing) score -= 1

        return when {
            score >= 2 -> Importance.IMPORTANT
            score <= -1 -> Importance.IGNORE
            else -> Importance.MEDIUM
        }
    }

    /** Notifications worth interrupting the user for (IMPORTANT), in input order. */
    fun important(items: List<NotificationInfo>, policy: TriagePolicy = TriagePolicy()): List<NotificationInfo> =
        items.filter { classify(it, policy) == Importance.IMPORTANT }
}
