package com.kianirani.jarvis.core.palette

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DS-WIN2 acceptance: the palette matches exact/prefix/keyword/substring/subsequence queries,
 * ranks stronger matches first, abbreviations resolve, blank browses, and limit caps. Pure.
 */
class CommandPaletteTest {

    private val commands = listOf(
        Command("settings", "Settings", listOf("preferences", "config")),
        Command("dark", "Toggle Dark Mode", listOf("theme", "night")),
        Command("newnote", "New Note", listOf("create", "memo")),
        Command("search", "Search Everything", listOf("find")),
    )

    @Test fun `exact title match wins`() {
        assertEquals("settings", CommandPalette.match("settings", commands).first().id)
    }

    @Test fun `prefix beats keyword`() {
        // "se" prefixes "Settings" and "Search"; both 0.9 → tie broken by title ("search" < "settings")
        val out = CommandPalette.match("se", commands).map { it.id }
        assertEquals(listOf("search", "settings"), out)
    }

    @Test fun `keyword match surfaces a command whose title does not contain the query`() {
        assertEquals("dark", CommandPalette.match("night", commands).first().id)
    }

    @Test fun `substring inside the title matches`() {
        assertEquals("dark", CommandPalette.match("dark", commands).first().id)
    }

    @Test fun `abbreviation resolves via subsequence`() {
        // t-d-m is a subsequence of "toggledarkmode"
        assertEquals("dark", CommandPalette.match("tdm", commands).first().id)
    }

    @Test fun `stronger match ranks above a weaker one`() {
        val exact = CommandPalette.score("settings", commands[0])
        val sub = CommandPalette.score("stg", commands[0]) // subsequence only
        assertTrue(exact > sub)
    }

    @Test fun `blank query browses and limit caps`() {
        assertEquals(4, CommandPalette.match("", commands).size)
        assertEquals(2, CommandPalette.match("", commands, limit = 2).size)
    }

    @Test fun `no match yields empty`() {
        assertTrue(CommandPalette.match("zzzqqq", commands).isEmpty())
    }
}
