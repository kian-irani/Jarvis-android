package com.kianirani.jarvis.brain

import com.kianirani.jarvis.brain.data.FileRepository
import com.kianirani.jarvis.brain.server.BrainException
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `write then read roundtrips within root`() {
        val repo = FileRepository(tmp.root)
        repo.write("notes/a.txt", "hello")
        assertEquals("hello", repo.read("notes/a.txt"))
        assertEquals(listOf("notes/a.txt"), repo.list("notes"))
    }

    @Test
    fun `path traversal is rejected`() {
        val repo = FileRepository(tmp.root)
        try { repo.read("../etc/passwd"); fail("expected") } catch (e: BrainException) {
            assertEquals("VALIDATION", e.code)
        }
    }
}
