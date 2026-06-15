package com.kianirani.jarvis.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

/** FNT — pure font-pack resolution rules (id validity + auto-Persian default). */
class FontCatalogTest {

    @Test
    fun `auto resolves to Space Grotesk for non-Persian UI`() {
        assertEquals(FontCatalog.SPACE_GROTESK, FontCatalog.resolve(FontCatalog.AUTO, persian = false))
    }

    @Test
    fun `auto resolves to Vazirmatn for Persian UI`() {
        assertEquals(FontCatalog.VAZIRMATN, FontCatalog.resolve(FontCatalog.AUTO, persian = true))
    }

    @Test
    fun `an explicit choice is honoured regardless of language`() {
        assertEquals(FontCatalog.DM_SANS, FontCatalog.resolve(FontCatalog.DM_SANS, persian = true))
        assertEquals(FontCatalog.INTER, FontCatalog.resolve(FontCatalog.INTER, persian = false))
        // Explicit Space Grotesk stays Space Grotesk even on a Persian UI.
        assertEquals(FontCatalog.SPACE_GROTESK, FontCatalog.resolve(FontCatalog.SPACE_GROTESK, persian = true))
    }

    @Test
    fun `coerce clamps unknown ids to auto and keeps valid ones`() {
        assertEquals(FontCatalog.AUTO, FontCatalog.coerce(-1))
        assertEquals(FontCatalog.AUTO, FontCatalog.coerce(99))
        FontCatalog.ids.forEach { assertEquals(it, FontCatalog.coerce(it)) }
    }

    @Test
    fun `unknown stored id falls back to the auto default behaviour`() {
        // resolve() coerces first, so a garbage id behaves like AUTO.
        assertEquals(FontCatalog.SPACE_GROTESK, FontCatalog.resolve(42, persian = false))
        assertEquals(FontCatalog.VAZIRMATN, FontCatalog.resolve(42, persian = true))
    }

    @Test
    fun `every id has a display name and the picker lists all packs`() {
        assertEquals(listOf(0, 1, 2, 3, 4, 5), FontCatalog.ids)
        assertEquals("Auto", FontCatalog.name(FontCatalog.AUTO))
        assertEquals("Space Grotesk", FontCatalog.name(FontCatalog.SPACE_GROTESK))
        assertEquals("Inter", FontCatalog.name(FontCatalog.INTER))
        assertEquals("DM Sans", FontCatalog.name(FontCatalog.DM_SANS))
        assertEquals("Exo 2", FontCatalog.name(FontCatalog.EXO_2))
        assertEquals("Vazirmatn", FontCatalog.name(FontCatalog.VAZIRMATN))
    }
}
