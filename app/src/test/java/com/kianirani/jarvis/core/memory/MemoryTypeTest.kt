package com.kianirani.jarvis.core.memory

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryTypeTest {

    @Test fun `fromName parses a known type case-insensitively`() {
        assertEquals(MemoryType.PREFERENCE, MemoryType.fromName("preference"))
        assertEquals(MemoryType.EPISODIC, MemoryType.fromName("EPISODIC"))
    }

    @Test fun `fromName falls back to FACT for unknown or null`() {
        assertEquals(MemoryType.FACT, MemoryType.fromName("nonsense"))
        assertEquals(MemoryType.FACT, MemoryType.fromName(null))
    }
}
