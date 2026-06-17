package com.kianirani.jarvis.voice

import com.kianirani.jarvis.voice.VoiceSegmenter.Script
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSegmenterTest {

    private fun scripts(text: String) = VoiceSegmenter.segment(text).map { it.script }
    private fun texts(text: String) = VoiceSegmenter.segment(text).map { it.text }

    @Test
    fun `pure english is one latin segment`() {
        val segs = VoiceSegmenter.segment("Play some music")
        assertEquals(1, segs.size)
        assertEquals(Script.LATIN, segs.single().script)
        assertEquals("Play some music", segs.single().text)
    }

    @Test
    fun `pure persian is one persian segment`() {
        val segs = VoiceSegmenter.segment("سلام حالت چطوره")
        assertEquals(1, segs.size)
        assertEquals(Script.PERSIAN, segs.single().script)
    }

    @Test
    fun `code switch splits persian and latin runs`() {
        // "بساز یک playlist از Shakira الان"
        val text = "بساز یک playlist از Shakira الان"
        val segs = VoiceSegmenter.segment(text)
        // persian … latin "playlist " … persian "از " … latin "Shakira " … persian
        assertEquals(listOf(Script.PERSIAN, Script.LATIN, Script.PERSIAN, Script.LATIN, Script.PERSIAN), segs.map { it.script })
        // concatenation must reproduce the original text exactly.
        assertEquals(text, segs.joinToString("") { it.text })
    }

    @Test
    fun `neutral run attaches to previous segment not its own`() {
        val segs = VoiceSegmenter.segment("Hello سلام")
        assertEquals(2, segs.size)
        assertEquals(Script.LATIN, segs[0].script)
        assertEquals("Hello ", segs[0].text) // the space folded into the English run
        assertEquals("سلام", segs[1].text)
    }

    @Test
    fun `leading punctuation adopts the following script`() {
        val segs = VoiceSegmenter.segment("«سلام»")
        assertEquals(1, segs.size)
        assertEquals(Script.PERSIAN, segs.single().script)
        assertEquals("«سلام»", segs.single().text)
    }

    @Test
    fun `digits and punctuation only stay neutral`() {
        val segs = VoiceSegmenter.segment("123 — 456")
        assertEquals(1, segs.size)
        assertEquals(Script.NEUTRAL, segs.single().script)
    }

    @Test
    fun `empty text yields no segments`() {
        assertTrue(VoiceSegmenter.segment("").isEmpty())
    }

    @Test
    fun `isMixed detects code switch only`() {
        assertTrue(VoiceSegmenter.isMixed("یک message بفرست"))
        assertFalse(VoiceSegmenter.isMixed("just english"))
        assertFalse(VoiceSegmenter.isMixed("فقط فارسی"))
        assertFalse(VoiceSegmenter.isMixed("123 !!!"))
    }

    @Test
    fun `concatenation round trips for a long mixed reply`() {
        val text = "باشه! الان یک reminder برای ساعت 8 صبح می‌سازم، okay?"
        assertEquals(text, texts(text).joinToString(""))
        // at least one persian and one latin segment present.
        assertTrue(scripts(text).contains(Script.PERSIAN))
        assertTrue(scripts(text).contains(Script.LATIN))
    }
}
