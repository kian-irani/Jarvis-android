package com.kianirani.jarvis.core.mesh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MX1 acceptance: the router only considers online nodes advertising the model, prefers more
 * free RAM and lower load, breaks ties deterministically, and returns null when no peer can
 * serve. Pure, no network.
 */
class MeshModelRouterTest {

    private fun node(id: String, models: Set<String>, ram: Float, load: Float, online: Boolean = true) =
        MeshNode(id, models, ram, load, online)

    private val qwen = "qwen2.5:0.5b"

    @Test fun `only online nodes advertising the model are candidates`() {
        val nodes = listOf(
            node("desktop", setOf(qwen), 8f, 0.2f),
            node("offline", setOf(qwen), 16f, 0.0f, online = false),
            node("phone", setOf("gemma:2b"), 2f, 0.1f), // wrong model
        )
        assertEquals(listOf("desktop"), MeshModelRouter.candidates(nodes, qwen).map { it.id })
    }

    @Test fun `the node with more free ram and lower load wins`() {
        val nodes = listOf(
            node("busy", setOf(qwen), 16f, 0.9f),  // lots of ram but saturated → score 1.6
            node("free", setOf(qwen), 8f, 0.1f),   // score 7.2
        )
        assertEquals("free", MeshModelRouter.best(nodes, qwen)?.id)
    }

    @Test fun `a saturated or ram-starved node scores zero`() {
        assertEquals(0.0, MeshModelRouter.score(node("x", setOf(qwen), 16f, 1.0f)), 1e-9)
        assertEquals(0.0, MeshModelRouter.score(node("y", setOf(qwen), 0f, 0.1f)), 1e-9)
    }

    @Test fun `equal scores break by id`() {
        val nodes = listOf(
            node("zeta", setOf(qwen), 8f, 0.5f),
            node("alpha", setOf(qwen), 8f, 0.5f),
        )
        assertEquals("alpha", MeshModelRouter.best(nodes, qwen)?.id)
    }

    @Test fun `no node serving the model yields null`() {
        val nodes = listOf(node("phone", setOf("gemma:2b"), 4f, 0.1f))
        assertNull(MeshModelRouter.best(nodes, qwen))
        assertTrue(MeshModelRouter.candidates(nodes, qwen).isEmpty())
    }

    @Test fun `negative or over-range inputs are clamped`() {
        // load > 1 clamps to 1 (score 0), negative ram clamps to 0
        assertEquals(0.0, MeshModelRouter.score(node("a", setOf(qwen), 8f, 1.5f)), 1e-9)
        assertEquals(0.0, MeshModelRouter.score(node("b", setOf(qwen), -4f, 0.2f)), 1e-9)
    }
}
