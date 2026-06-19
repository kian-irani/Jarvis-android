package com.kianirani.jarvis.data.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** v79 fix — relation words resolve to contacts saved under a different label. Pure. */
class ContactRelationsTest {

    @Test fun `the spoken name is always tried first`() {
        assertEquals("مامان", ContactRelations.candidates("مامان").first())
        assertEquals("Ali", ContactRelations.candidates("Ali").first())
    }

    @Test fun `persian relation expands to its synonyms both ways`() {
        assertTrue(ContactRelations.candidates("مامان").contains("مادر"))
        assertTrue(ContactRelations.candidates("مادر").contains("مامان"))
        assertTrue(ContactRelations.candidates("بابا").contains("پدر"))
    }

    @Test fun `english relation maps onto persian candidates`() {
        val c = ContactRelations.candidates("mom")
        assertEquals("mom", c.first())
        assertTrue(c.contains("مامان"))
    }

    @Test fun `a plain name has no synonyms`() {
        assertEquals(listOf("Ali"), ContactRelations.candidates("Ali"))
    }

    @Test fun `blank input yields nothing`() {
        assertTrue(ContactRelations.candidates("   ").isEmpty())
    }

    @Test fun `candidates are de-duplicated`() {
        val c = ContactRelations.candidates("مامان")
        assertEquals(c.size, c.distinct().size)
    }
}
