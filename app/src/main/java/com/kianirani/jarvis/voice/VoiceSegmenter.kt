package com.kianirani.jarvis.voice

/**
 * Code-switch segmenter (Voice Engine v2, 2026-06-16). The old voice layer spoke
 * an entire reply with ONE locale chosen by "any Persian char → fa", so a mixed
 * "سلام، یک playlist از Shakira بساز" was read either with a Persian voice
 * mangling the English words, or an English voice mangling the Persian — the
 * "گیج زدنِ ترکیبِ فارسی/انگلیسی" the user reported.
 *
 * This splits text into ordered [Segment]s by script so each run can be spoken
 * with its own voice/locale in sequence: Persian runs with a Persian voice,
 * Latin runs with an English (or other-language) voice, and neutral runs (digits,
 * punctuation, spaces, emoji) folded into the neighbouring segment so we never
 * over-split. Pure + deterministic → fully JVM-testable.
 */
object VoiceSegmenter {

    /** Which speaking voice a run of text wants. */
    enum class Script { PERSIAN, LATIN, NEUTRAL }

    /** A contiguous run of text plus the voice it should be spoken with. */
    data class Segment(val text: String, val script: Script)

    /** Classify a single character. ZWNJ is treated as Persian (it joins Persian words). */
    fun scriptOf(ch: Char): Script = when {
        ch == '‌' -> Script.PERSIAN // zero-width non-joiner — Persian word-internal
        ch in '؀'..'ۿ' -> Script.PERSIAN // Arabic
        ch in 'ݐ'..'ݿ' -> Script.PERSIAN // Arabic Supplement
        ch in 'ࢠ'..'ࣿ' -> Script.PERSIAN // Arabic Extended-A
        ch in 'ﭐ'..'﷿' -> Script.PERSIAN // Arabic Presentation Forms-A
        ch in 'ﹰ'..'﻿' -> Script.PERSIAN // Arabic Presentation Forms-B
        ch in 'A'..'Z' || ch in 'a'..'z' -> Script.LATIN
        ch in 'À'..'ɏ' -> Script.LATIN // Latin-1 Supplement + Extended-A/B letters
        Character.isLetter(ch) -> Script.LATIN // any other alphabetic → Latin voice bucket
        else -> Script.NEUTRAL // digits, punctuation, whitespace, symbols, emoji
    }

    /**
     * Split [text] into ordered segments. Neutral runs attach to the preceding
     * segment (or the following one if they lead the string); adjacent runs of the
     * same script are merged. Concatenating the segment texts reproduces [text].
     */
    fun segment(text: String): List<Segment> {
        if (text.isEmpty()) return emptyList()

        // 1) char-level runs of identical script.
        data class Run(var script: Script, val sb: StringBuilder)
        val runs = ArrayList<Run>()
        for (ch in text) {
            val s = scriptOf(ch)
            val last = runs.lastOrNull()
            if (last != null && last.script == s) last.sb.append(ch)
            else runs.add(Run(s, StringBuilder().append(ch)))
        }

        // 2) fold neutral runs into a neighbour; merge same-script neighbours.
        val out = ArrayList<Run>()
        for (run in runs) {
            val last = out.lastOrNull()
            when {
                run.script == Script.NEUTRAL && last != null -> last.sb.append(run.sb)
                last != null && last.script == Script.NEUTRAL && run.script != Script.NEUTRAL -> {
                    // a leading neutral run adopts the first real script that follows.
                    last.script = run.script; last.sb.append(run.sb)
                }
                last != null && last.script == run.script -> last.sb.append(run.sb)
                else -> out.add(Run(run.script, StringBuilder().append(run.sb)))
            }
        }
        return out.map { Segment(it.sb.toString(), it.script) }
    }

    /** True when the text mixes Persian and Latin scripts (a code-switch reply). */
    fun isMixed(text: String): Boolean {
        var hasFa = false
        var hasLatin = false
        for (ch in text) when (scriptOf(ch)) {
            Script.PERSIAN -> hasFa = true
            Script.LATIN -> hasLatin = true
            Script.NEUTRAL -> {}
        }
        return hasFa && hasLatin
    }
}
