package com.kianirani.jarvis.core.text

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownLiteTest {

    @Test fun `plain line is one paragraph with one plain span`() {
        val blocks = MarkdownLite.parse("hello world")
        assertEquals(1, blocks.size)
        assertEquals(MdBlockType.PARAGRAPH, blocks[0].type)
        assertEquals(listOf(MdSpan("hello world")), blocks[0].spans)
    }

    @Test fun `double-star and double-underscore are bold`() {
        assertEquals(listOf(MdSpan("hi", bold = true)), MarkdownLite.parseInline("**hi**"))
        assertEquals(listOf(MdSpan("hi", bold = true)), MarkdownLite.parseInline("__hi__"))
    }

    @Test fun `backticks are inline code`() {
        assertEquals(listOf(MdSpan("x = 1", code = true)), MarkdownLite.parseInline("`x = 1`"))
    }

    @Test fun `mixed spans keep order and surrounding text`() {
        assertEquals(
            listOf(MdSpan("run "), MdSpan("build", bold = true), MdSpan(" then "), MdSpan("gw", code = true)),
            MarkdownLite.parseInline("run **build** then `gw`"),
        )
    }

    @Test fun `code wins over bold inside it`() {
        assertEquals(listOf(MdSpan("**x**", code = true)), MarkdownLite.parseInline("`**x**`"))
    }

    @Test fun `unmatched markers stay literal`() {
        assertEquals(listOf(MdSpan("a ** b")), MarkdownLite.parseInline("a ** b"))
        assertEquals(listOf(MdSpan("a `b")), MarkdownLite.parseInline("a `b"))
    }

    @Test fun `single star is literal not italic`() {
        assertEquals(listOf(MdSpan("a *b* c")), MarkdownLite.parseInline("a *b* c"))
    }

    @Test fun `bullet lines become bullet blocks`() {
        val blocks = MarkdownLite.parse("- one\n* two")
        assertEquals(listOf(MdBlockType.BULLET, MdBlockType.BULLET), blocks.map { it.type })
        assertEquals("one", blocks[0].spans.single().text)
        assertEquals("two", blocks[1].spans.single().text)
    }

    @Test fun `numbered lines keep their ordinal and parse inline`() {
        val blocks = MarkdownLite.parse("1. first\n2. do **x**")
        assertEquals(MdBlockType.NUMBERED, blocks[0].type)
        assertEquals(1, blocks[0].ordinal)
        assertEquals(2, blocks[1].ordinal)
        assertEquals(listOf(MdSpan("do "), MdSpan("x", bold = true)), blocks[1].spans)
    }

    @Test fun `blank lines are dropped between paragraphs`() {
        val blocks = MarkdownLite.parse("a\n\n\nb")
        assertEquals(2, blocks.size)
        assertEquals("a", blocks[0].spans.single().text)
        assertEquals("b", blocks[1].spans.single().text)
    }

    @Test fun `empty input yields no blocks`() {
        assertEquals(emptyList<MdBlock>(), MarkdownLite.parse(""))
        assertEquals(emptyList<MdBlock>(), MarkdownLite.parse("   \n  "))
    }
}
