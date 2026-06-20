package com.kianirani.jarvis.core.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** File-transfer acceptance: chunk counts/ranges, short last chunk, resume, progress. Pure. */
class ChunkPlanTest {

    @Test fun `chunk count rounds up`() {
        assertEquals(0, ChunkPlan.chunkCount(0, 100))
        assertEquals(1, ChunkPlan.chunkCount(50, 100))
        assertEquals(2, ChunkPlan.chunkCount(150, 100))
        assertEquals(3, ChunkPlan.chunkCount(300, 100))
    }

    @Test fun `last chunk is short`() {
        val last = ChunkPlan.chunkAt(2, 250, 100)
        assertEquals(Chunk(2, 200, 50), last)
    }

    @Test fun `all chunks cover the file exactly`() {
        val chunks = ChunkPlan.allChunks(250, 100)
        assertEquals(250L, chunks.sumOf { it.size.toLong() })
        assertEquals(0L, chunks.first().offset)
    }

    @Test fun `remaining skips acked chunks in order`() {
        val rem = ChunkPlan.remaining(300, acked = setOf(0, 2), chunkSize = 100)
        assertEquals(listOf(1), rem.map { it.index })
    }

    @Test fun `progress tracks acked fraction`() {
        assertEquals(0.5f, ChunkPlan.progress(400, acked = setOf(0, 1), chunkSize = 100), 1e-6f)
        assertEquals(1f, ChunkPlan.progress(0, acked = emptySet(), chunkSize = 100), 1e-6f) // empty file = done
    }

    @Test fun `out of range chunk throws`() {
        var threw = false
        try { ChunkPlan.chunkAt(5, 100, 100) } catch (e: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }
}
