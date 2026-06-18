package com.kianirani.jarvis.core.memory

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryFilterTest {

    private fun stored(content: String, type: MemoryType) =
        MemoryEngine.Stored(id = content, content = content, type = type, importance = 0.5f, createdAt = 0L)

    private val rows = listOf(
        stored("My VPS IP is 1.2.3.4", MemoryType.FACT),
        stored("User dislikes neumorphism", MemoryType.PREFERENCE),
        stored("Ali is a developer friend", MemoryType.PERSON),
        stored("Vision OS project context", MemoryType.PROJECT),
    )

    @Test fun `empty query and null type returns everything`() {
        assertEquals(rows, MemoryFilter.filter(rows))
    }

    @Test fun `type filter keeps only that type`() {
        val out = MemoryFilter.filter(rows, type = MemoryType.PERSON)
        assertEquals(listOf("Ali is a developer friend"), out.map { it.content })
    }

    @Test fun `query matches case-insensitive substring`() {
        assertEquals(listOf("User dislikes neumorphism"), MemoryFilter.filter(rows, query = "NEUMORPH").map { it.content })
    }

    @Test fun `query and type combine`() {
        assertEquals(emptyList<String>(), MemoryFilter.filter(rows, query = "vision", type = MemoryType.FACT).map { it.content })
        assertEquals(
            listOf("Vision OS project context"),
            MemoryFilter.filter(rows, query = "vision", type = MemoryType.PROJECT).map { it.content },
        )
    }

    @Test fun `no match returns empty`() {
        assertEquals(emptyList<MemoryEngine.Stored>(), MemoryFilter.filter(rows, query = "zzz-nope"))
    }

    @Test fun `blank query is ignored after trim`() {
        assertEquals(rows, MemoryFilter.filter(rows, query = "   "))
    }

    @Test fun `typesPresent is distinct in first-seen order`() {
        val dupes = rows + stored("another fact", MemoryType.FACT)
        assertEquals(
            listOf(MemoryType.FACT, MemoryType.PREFERENCE, MemoryType.PERSON, MemoryType.PROJECT),
            MemoryFilter.typesPresent(dupes),
        )
    }
}
