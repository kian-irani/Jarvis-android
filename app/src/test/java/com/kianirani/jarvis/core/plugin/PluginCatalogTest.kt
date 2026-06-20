package com.kianirani.jarvis.core.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** B3 acceptance: catalog search (verified-first), source filter, installability check. Pure. */
class PluginCatalogTest {

    private fun catalog() = PluginCatalog(listOf(
        CatalogEntry("notion", "Notion Sync", "save notes to Notion", PluginSource.NOTION, setOf(Capability.NETWORK), verified = true),
        CatalogEntry("disc", "Discord Bridge", "post to Discord", PluginSource.DISCORD, setOf(Capability.NETWORK, Capability.MESSAGING)),
        CatalogEntry("notes2", "Notion Exporter", "export to Notion", PluginSource.NOTION, setOf(Capability.NETWORK)),
    ))

    @Test fun `search ranks verified first then name`() {
        val out = catalog().search("notion").map { it.id }
        assertEquals(listOf("notion", "notes2"), out) // verified "Notion Sync" first
    }

    @Test fun `bySource filters`() {
        assertEquals(setOf("notion", "notes2"), catalog().bySource(PluginSource.NOTION).map { it.id }.toSet())
    }

    @Test fun `installableWith checks capability subset`() {
        val c = catalog()
        assertTrue(c.installableWith("notion", setOf(Capability.NETWORK)))
        assertFalse(c.installableWith("disc", setOf(Capability.NETWORK))) // also needs MESSAGING
    }

    @Test fun `blank search returns all sorted`() {
        assertEquals(3, catalog().search("").size)
    }
}
