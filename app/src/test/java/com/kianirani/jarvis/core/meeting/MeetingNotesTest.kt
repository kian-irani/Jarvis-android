package com.kianirani.jarvis.core.meeting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** D1 acceptance: transcript turns parse, action items + decisions extract. Pure. */
class MeetingNotesTest {

    private val transcript = """
        Alice: Welcome everyone.
        Bob: I'll send the report by Friday.
        Alice: We decided to ship on Monday.
        Bob: nice weather today.
    """.trimIndent()

    @Test fun `parses speaker turns`() {
        val turns = MeetingNotes.parseTurns(transcript)
        assertEquals(4, turns.size)
        assertEquals("Alice", turns.first().speaker)
        assertEquals("Welcome everyone.", turns.first().text)
    }

    @Test fun `extracts action items and decisions`() {
        val d = MeetingNotes.digest(MeetingNotes.parseTurns(transcript))
        assertTrue(d.actionItems.any { it.contains("send the report") })
        assertTrue(d.decisions.any { it.contains("ship on Monday") })
        assertEquals(4, d.turns)
    }

    @Test fun `a line without a speaker still becomes a turn`() {
        val turns = MeetingNotes.parseTurns("just a thought")
        assertEquals("", turns.single().speaker)
        assertEquals("just a thought", turns.single().text)
    }

    @Test fun `summary prompt embeds the transcript`() {
        assertTrue(MeetingNotes.summaryPrompt("X: hi").contains("X: hi"))
    }
}
