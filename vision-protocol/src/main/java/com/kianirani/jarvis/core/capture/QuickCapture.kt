package com.kianirani.jarvis.core.capture

import com.kianirani.jarvis.core.automation.ParsedSchedule
import com.kianirani.jarvis.core.automation.ScheduledCommandParser

/**
 * DS-W4 — the widget's quick-capture mini-panel. A single text box where the user dumps a
 * thought; this pure classifier routes it to the right place: a timed/keyworded entry becomes a
 * **Reminder** (→ the CF5 scheduler), anything else a **Note** (→ Memory). It reuses
 * [ScheduledCommandParser] (AGT-SCHED) for the time phrase, so "remind me at 6pm to call mom"
 * captures both the schedule and the cleaned action. Pure → JVM-tested; persisting the note to
 * Memory and registering the reminder with the scheduler are the on-device half.
 */
sealed interface Capture {
    data class Note(val text: String) : Capture
    data class Reminder(val text: String, val schedule: ParsedSchedule?) : Capture
}

object QuickCapture {

    private val REMINDER_HINT = Regex(
        """\b(remind|reminder|remember to|don'?t forget)\b|یادم|یادآوری|یادت""",
        RegexOption.IGNORE_CASE,
    )

    private val LEADING_REMINDER = Regex(
        """^(remind me to|remind me|reminder:?|remember to|note:?|یادم بنداز که|یادم بنداز)\s*""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Classify [input]: a parseable time phrase → [Capture.Reminder] with that schedule; a
     * reminder keyword without a time → [Capture.Reminder] with a null schedule (the caller
     * asks for a when); otherwise → [Capture.Note]. Blank input is an empty note.
     */
    fun classify(input: String): Capture {
        val text = input.trim()
        if (text.isEmpty()) return Capture.Note("")

        val parsed = ScheduledCommandParser.parse(text)
        if (parsed != null) {
            val body = stripReminderPrefix(parsed.action).ifBlank { stripReminderPrefix(text) }
            return Capture.Reminder(body, parsed.schedule)
        }
        if (REMINDER_HINT.containsMatchIn(text)) {
            return Capture.Reminder(stripReminderPrefix(text), schedule = null)
        }
        return Capture.Note(text)
    }

    private fun stripReminderPrefix(text: String): String =
        text.replace(LEADING_REMINDER, "").trim().ifBlank { text }
}
