package com.kianirani.jarvis.core.mesh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** MX3 acceptance: pick the best-scoring online node serving the model; address built. Pure. */
class MeshInferenceSelectorTest {

    private val qwen = "qwen2.5:0.5b"
    private fun node(id: String, score: Double, online: Boolean = true, models: Set<String> = setOf(qwen)) =
        InferenceNode(id, "10.0.0.$id", 7799, models, score, online)

    @Test fun `selects the highest-score node and builds the target`() {
        val t = MeshInferenceSelector.select(listOf(node("1", 0.4), node("2", 0.9)), qwen)
        assertEquals("2", t?.nodeId)
        assertEquals("http://10.0.0.2:7799", t?.baseUrl)
        assertEquals(qwen, t?.model)
    }

    @Test fun `skips offline and zero-score and wrong-model nodes`() {
        val nodes = listOf(
            node("off", 0.9, online = false),
            node("zero", 0.0),
            node("wrong", 0.9, models = setOf("gemma:2b")),
            node("good", 0.5),
        )
        assertEquals("good", MeshInferenceSelector.select(nodes, qwen)?.nodeId)
    }

    @Test fun `null when no node serves the model`() {
        assertNull(MeshInferenceSelector.select(listOf(node("1", 0.9, models = setOf("other"))), qwen))
    }

    @Test fun `ties break by id`() {
        assertEquals("alpha", MeshInferenceSelector.select(listOf(node("zeta", 0.5), node("alpha", 0.5)), qwen)?.nodeId)
    }
}
