package com.kianirani.jarvis.core.text

/**
 * BUG-1b — a tiny, pure Markdown-subset parser for chat output: bold (`**x**` /
 * `__x__`), inline `` `code` ``, and bullet / numbered lists. No Compose or Android
 * deps so it is fully JVM-unit-tested; a thin Compose renderer ([ui] MarkdownText)
 * turns the [MdBlock]s into an AnnotatedString. Unmatched markup degrades to literal
 * text — the parser never throws.
 */
enum class MdBlockType { PARAGRAPH, BULLET, NUMBERED }

/** An inline run of text with optional bold / monospace styling. */
data class MdSpan(val text: String, val bold: Boolean = false, val code: Boolean = false)

/** A block-level element: a paragraph line or one list item. [ordinal] is the number for [NUMBERED]. */
data class MdBlock(val type: MdBlockType, val spans: List<MdSpan>, val ordinal: Int = 1)

object MarkdownLite {
    private val bullet = Regex("""^\s*[-*]\s+(.*)$""")
    private val numbered = Regex("""^\s*(\d+)[.)]\s+(.*)$""")

    /** Parse text into block elements; blank lines are dropped (they become spacing). */
    fun parse(input: String): List<MdBlock> {
        val blocks = mutableListOf<MdBlock>()
        for (raw in input.split('\n')) {
            val line = raw.trimEnd()
            if (line.isBlank()) continue
            val b = bullet.matchEntire(line)
            val n = numbered.matchEntire(line)
            when {
                b != null -> blocks += MdBlock(MdBlockType.BULLET, parseInline(b.groupValues[1]))
                n != null -> blocks += MdBlock(MdBlockType.NUMBERED, parseInline(n.groupValues[2]), n.groupValues[1].toIntOrNull() ?: 1)
                else -> blocks += MdBlock(MdBlockType.PARAGRAPH, parseInline(line))
            }
        }
        return blocks
    }

    /** Tokenize one line into styled runs. Inline code wins over bold; unmatched markers are literal. */
    fun parseInline(text: String): List<MdSpan> {
        val spans = mutableListOf<MdSpan>()
        val buf = StringBuilder()
        fun flush() {
            if (buf.isNotEmpty()) {
                spans += MdSpan(buf.toString())
                buf.clear()
            }
        }
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '`' -> {
                    val close = text.indexOf('`', i + 1)
                    if (close > i) {
                        flush()
                        spans += MdSpan(text.substring(i + 1, close), code = true)
                        i = close + 1
                    } else {
                        buf.append(c); i++
                    }
                }
                text.startsWith("**", i) || text.startsWith("__", i) -> {
                    val delim = text.substring(i, i + 2)
                    val close = text.indexOf(delim, i + 2)
                    if (close > i + 1) {
                        flush()
                        spans += MdSpan(text.substring(i + 2, close), bold = true)
                        i = close + 2
                    } else {
                        buf.append(c); i++
                    }
                }
                else -> {
                    buf.append(c); i++
                }
            }
        }
        flush()
        return spans
    }
}
