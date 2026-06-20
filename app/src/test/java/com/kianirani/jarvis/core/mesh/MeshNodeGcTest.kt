package com.kianirani.jarvis.core.mesh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mesh GC acceptance: stale nodes (beyond grace) are evicted, fresh ones kept. Pure. */
class MeshNodeGcTest {

    private val now = 1_000_000L

    @Test fun `a node within grace is live`() {
        val node = MeshNodeRecord("a", now - 10_000)
        assertFalse(MeshNodeGc.isStale(node, now, staleAfterMs = 90_000))
    }

    @Test fun `a node beyond grace is stale`() {
        val node = MeshNodeRecord("a", now - 120_000)
        assertTrue(MeshNodeGc.isStale(node, now, staleAfterMs = 90_000))
    }

    @Test fun `live and toEvict partition the roster`() {
        val nodes = listOf(
            MeshNodeRecord("fresh", now - 5_000),
            MeshNodeRecord("stale", now - 200_000),
            MeshNodeRecord("ok", now - 60_000),
        )
        assertEquals(listOf("fresh", "ok"), MeshNodeGc.live(nodes, now).map { it.id })
        assertEquals(listOf("stale"), MeshNodeGc.toEvict(nodes, now))
    }
}
