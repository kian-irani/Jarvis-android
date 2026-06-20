package com.kianirani.jarvis.core.meeting

/**
 * D1 — Meeting & Voice Intelligence (PRD §, "ضبط تماس/جلسه → خلاصه + استخراج Task/تصمیم"). The
 * pure structuring layer over a transcript: split it into speaker turns and heuristically pull
 * action items ("I'll …", "we need to …", "TODO") and decisions ("we decided …", "agreed"). The
 * model writes the prose summary; this gives it a deterministic skeleton + the prompt. Pure →
 * JVM-tested; recording/STT is the device half (VCF-M3).
 */
data class TranscriptTurn(val speaker: String, val text: String)

data class MeetingDigest(val actionItems: List<String>, val decisions: List<String>, val turns: Int)

object MeetingNotes {

    private val ACTION = Regex("""(?i)\b(i('| wi)ll|we (need|have) to|let's|todo|action item|follow up)\b""")
    private val DECISION = Regex("""(?i)\b(we (decided|agreed)|decision|let's go with|final(ize|ised|ized)?)\b""")

    /** Parse a flat transcript ("Alice: hi\nBob: hello") into speaker turns. */
    fun parseTurns(transcript: String): List<TranscriptTurn> =
        transcript.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                val i = line.indexOf(':')
                if (i in 1..30) TranscriptTurn(line.substring(0, i).trim(), line.substring(i + 1).trim())
                else TranscriptTurn("", line)
            }
            .toList()

    /** Extract action items + decisions from the [turns] by heuristic phrase match. */
    fun digest(turns: List<TranscriptTurn>): MeetingDigest {
        val actions = turns.filter { ACTION.containsMatchIn(it.text) }.map { it.text }
        val decisions = turns.filter { DECISION.containsMatchIn(it.text) }.map { it.text }
        return MeetingDigest(actions, decisions, turns.size)
    }

    /** The prompt handed to the model to write the narrative summary. */
    fun summaryPrompt(transcript: String): String =
        "Summarize this meeting in 3-4 sentences, then list action items (who/what) and decisions. " +
            "Transcript:\n$transcript"
}
