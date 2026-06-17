package com.kianirani.jarvis.brain

import com.kianirani.jarvis.brain.data.Embedder
import com.kianirani.jarvis.brain.data.MemoryRepository
import com.kianirani.jarvis.brain.data.db.MemoryDao
import com.kianirani.jarvis.brain.data.db.MemoryEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeEmbedder : Embedder {
    override val dim = 4
    override fun embed(texts: List<String>) = texts.map { t ->
        if (t.contains("cat")) floatArrayOf(1f, 0f, 0f, 0f) else floatArrayOf(0f, 1f, 0f, 0f)
    }
}

class MemoryRepositoryTest {
    @Test
    fun `store embeds content and persists`() = runTest {
        val dao = mockk<MemoryDao>()
        val saved = slot<MemoryEntity>()
        coEvery { dao.insert(capture(saved)) } returns Unit
        val repo = MemoryRepository(dao) { FakeEmbedder().embed(it) }
        repo.store(type = "episodic", content = "the cat sat", metadata = "{}")
        coVerify { dao.insert(any()) }
        assertEquals("episodic", saved.captured.type)
        assertEquals(4 * 4, saved.captured.embedding!!.size)
    }

    @Test
    fun `search ranks by cosine similarity`() = runTest {
        val dao = mockk<MemoryDao>()
        val repo0 = MemoryRepository(dao) { FakeEmbedder().embed(it) }
        val cat = repo0.toBlob(floatArrayOf(1f, 0f, 0f, 0f))
        val dog = repo0.toBlob(floatArrayOf(0f, 1f, 0f, 0f))
        coEvery { dao.allWithEmbedding() } returns listOf(
            MemoryEntity("a", "semantic", "about dogs", "{}", dog, 0, 0),
            MemoryEntity("b", "semantic", "about cats", "{}", cat, 0, 0),
        )
        val results = repo0.search("cat stuff", topK = 1)
        assertEquals("b", results.single().id)
    }

    @Test
    fun `searchDetailed ranks by cosine and carries type, metadata, createdAt`() = runTest {
        val dao = mockk<MemoryDao>()
        val repo0 = MemoryRepository(dao) { FakeEmbedder().embed(it) }
        val cat = repo0.toBlob(floatArrayOf(1f, 0f, 0f, 0f))
        val dog = repo0.toBlob(floatArrayOf(0f, 1f, 0f, 0f))
        coEvery { dao.allWithEmbedding() } returns listOf(
            MemoryEntity("a", "episodic", "about dogs", "{\"importance\":0.2}", dog, 10, 10),
            MemoryEntity("b", "fact", "about cats", "{\"importance\":0.9}", cat, 20, 20),
        )
        val top = repo0.searchDetailed("cat stuff", pool = 2).first()
        assertEquals("b", top.id)
        assertEquals("fact", top.type)
        assertEquals("{\"importance\":0.9}", top.metadata)
        assertEquals(20L, top.createdAt)
    }
}
