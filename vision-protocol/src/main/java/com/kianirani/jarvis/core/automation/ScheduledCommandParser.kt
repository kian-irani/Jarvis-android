package com.kianirani.jarvis.core.automation

/**
 * AGT-SCHED — turns a natural command like *"today at 16:00 call Mr X and say don't forget the
 * meeting"* or *"in 30 minutes remind me to drink water"* into a structured
 * [ScheduledCommand]: **when** (a clock time / relative delay / repeating interval) plus the
 * **action** text the agent should run at that time. Pure & deterministic (no clock, no
 * Android) → JVM-tested. The caller resolves [ParsedSchedule] against its own [Clock] to make
 * a concrete [Schedule], registers it, and at fire time runs [ScheduledCommand.action]
 * through the agent (with the SAFE trust gate for critical actions).
 *
 * Bilingual-friendly: Persian/Arabic-Indic digits are normalized before matching so
 * «ساعت ۱۶:۰۰» parses the same as "at 16:00".
 */
sealed interface ParsedSchedule {
    /** "at 16:00" / "at 9am" → a minute of the day (0..1439), local. */
    data class AtClock(val minuteOfDay: Int) : ParsedSchedule

    /** "in 30 minutes" / "in 2 hours" → a delay from now. */
    data class After(val deltaMillis: Long) : ParsedSchedule

    /** "every 2 hours" → a repeating interval. */
    data class EveryInterval(val everyMillis: Long) : ParsedSchedule
}

data class ScheduledCommand(val schedule: ParsedSchedule, val action: String)

object ScheduledCommandParser {

    private const val MIN_MS = 60_000L
    private const val HOUR_MS = 60 * MIN_MS

    private val EVERY = Regex("""\bevery\s+(\d+)\s*(hours?|hrs?|h|minutes?|mins?|m)\b""")
    private val IN = Regex("""\bin\s+(\d+)\s*(hours?|hrs?|h|minutes?|mins?|m)\b""")
    private val AT_HHMM = Regex("""\bat\s+(\d{1,2}):(\d{2})\b""")
    private val AT_AMPM = Regex("""\bat\s+(\d{1,2})\s*(am|pm)\b""")

    private val LEADING_FILLER = Regex(
        """^(today|tonight|tomorrow|please|then|also|and|to|,|\.|:|امروز|لطفا|و)\b[\s,]*""",
        RegexOption.IGNORE_CASE,
    )

    /** Map Persian (۰-۹) and Arabic (٠-٩) digits to ASCII; length-preserving. */
    private fun normalizeDigits(s: String): String = buildString(s.length) {
        for (c in s) append(
            when (c) {
                in '۰'..'۹' -> '0' + (c - '۰')
                in '٠'..'٩' -> '0' + (c - '٠')
                else -> c
            },
        )
    }

    private fun unitMillis(unit: String): Long = if (unit.startsWith("h")) HOUR_MS else MIN_MS

    /**
     * Parse [input] into a [ScheduledCommand], or null if it carries no recognizable time
     * phrase. The first time phrase found wins (interval > relative > absolute), and the
     * action is the rest of the sentence with that phrase and leading filler removed.
     */
    fun parse(input: String): ScheduledCommand? {
        val norm = normalizeDigits(input)
        val lower = norm.lowercase()

        data class Hit(val range: IntRange, val schedule: ParsedSchedule)

        val hit: Hit? = EVERY.find(lower)?.let {
            Hit(it.range, ParsedSchedule.EveryInterval(it.groupValues[1].toLong() * unitMillis(it.groupValues[2])))
        } ?: IN.find(lower)?.let {
            Hit(it.range, ParsedSchedule.After(it.groupValues[1].toLong() * unitMillis(it.groupValues[2])))
        } ?: AT_HHMM.find(lower)?.let {
            val h = it.groupValues[1].toInt()
            val m = it.groupValues[2].toInt()
            if (h > 23 || m > 59) null else Hit(it.range, ParsedSchedule.AtClock(h * 60 + m))
        } ?: AT_AMPM.find(lower)?.let {
            val raw = it.groupValues[1].toInt()
            if (raw < 1 || raw > 12) {
                null
            } else {
                val pm = it.groupValues[2] == "pm"
                val h = when {
                    pm && raw != 12 -> raw + 12
                    !pm && raw == 12 -> 0
                    else -> raw
                }
                Hit(it.range, ParsedSchedule.AtClock(h * 60))
            }
        }

        hit ?: return null
        val withoutTime = norm.removeRange(hit.range)
        return ScheduledCommand(hit.schedule, cleanAction(withoutTime))
    }

    private fun cleanAction(text: String): String {
        var t = text.replace(Regex("""\s+"""), " ").trim().trim(',', '.', ':').trim()
        // peel a couple of leading filler words ("today", "and", "remind me to" handled loosely)
        repeat(3) {
            val stripped = t.replace(LEADING_FILLER, "")
            if (stripped == t) return@repeat
            t = stripped.trim()
        }
        return t
    }
}
