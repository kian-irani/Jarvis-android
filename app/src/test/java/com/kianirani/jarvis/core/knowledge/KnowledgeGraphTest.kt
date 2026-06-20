package com.kianirani.jarvis.core.knowledge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A2 acceptance: entities and relations add/dedup, neighbors traverse both directions, relations
 * query by label, name search is case-insensitive, and removing an entity cascades. Pure.
 */
class KnowledgeGraphTest {

    private fun graph(): KnowledgeGraph = KnowledgeGraph().apply {
        addEntity(Entity("kian", "Kian", EntityType.PERSON))
        addEntity(Entity("vpn", "Kian VPN", EntityType.PROJECT))
        addEntity(Entity("frankfurt", "Frankfurt VPS", EntityType.SERVER))
        addRelation(Relation("kian", "vpn", "owns"))
        addRelation(Relation("vpn", "frankfurt", "runs_on"))
    }

    @Test fun `addEntity replaces by id and entitiesOfType filters`() {
        val g = graph()
        g.addEntity(Entity("kian", "Kian Irani", EntityType.PERSON)) // replace
        assertEquals("Kian Irani", g.entity("kian")?.name)
        assertEquals(listOf("frankfurt"), g.entitiesOfType(EntityType.SERVER).map { it.id })
    }

    @Test fun `addRelation dedupes`() {
        val g = graph()
        g.addRelation(Relation("kian", "vpn", "owns")) // duplicate
        assertEquals(1, g.relationsOf("kian").size)
    }

    @Test fun `neighbors traverse both directions`() {
        val g = graph()
        assertEquals(setOf("kian", "frankfurt"), g.neighbors("vpn").map { it.id }.toSet())
        assertEquals(listOf("vpn"), g.neighbors("kian").map { it.id })
    }

    @Test fun `related follows an outgoing relation label`() {
        val g = graph()
        assertEquals(listOf("frankfurt"), g.related("vpn", "runs_on").map { it.id })
        assertTrue(g.related("vpn", "owns").isEmpty()) // wrong direction/label
    }

    @Test fun `findByName is case-insensitive and substring`() {
        val g = graph()
        assertEquals(setOf("vpn"), g.findByName("vpn").map { it.id }.toSet())
        // both "Kian" (person) and "Kian VPN" (project) contain "kian" — case-insensitive substring
        assertEquals(setOf("kian", "vpn"), g.findByName("KIAN").map { it.id }.toSet())
        assertTrue(g.findByName("  ").isEmpty())
    }

    @Test fun `removeEntity cascades its relations`() {
        val g = graph()
        g.removeEntity("vpn")
        assertNull(g.entity("vpn"))
        assertTrue(g.relationsOf("kian").isEmpty()) // owns relation gone
        assertTrue(g.neighbors("frankfurt").isEmpty()) // runs_on relation gone
    }
}
