package com.kianirani.jarvis.router.registry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRegistryTest {

    @Test
    fun `seeds cloud providers plus the local model`() {
        val reg = ModelRegistry()
        val all = reg.all()
        // 6 cloud providers + 1 local
        assertEquals(7, all.size)
        assertTrue(all.any { it.backend == ModelBackend.LOCAL && it.id == ModelSeed.LOCAL_DEFAULT_ID })
        assertEquals(6, all.count { it.backend == ModelBackend.CLOUD })
    }

    @Test
    fun `rankedFor REASONING puts strongest reasoner first`() {
        val reg = ModelRegistry()
        val ranked = reg.rankedFor(Capability.REASONING)
        // Anthropic seed has reasoning 9 — the top prior.
        assertEquals("ANTHROPIC", ranked.first().provider)
    }

    @Test
    fun `rankedFor COST favours the free local model`() {
        val reg = ModelRegistry()
        val ranked = reg.rankedFor(Capability.COST)
        assertEquals(ModelBackend.LOCAL, ranked.first().backend)
    }

    @Test
    fun `requireVision filters out text-only models`() {
        val reg = ModelRegistry()
        val visionModels = reg.rankedFor(Capability.REASONING, requireVision = true)
        assertTrue(visionModels.isNotEmpty())
        assertTrue(visionModels.all { it.supportsVision })
        // Groq seed is text-only -> excluded.
        assertFalse(visionModels.any { it.provider == "GROQ" })
    }

    @Test
    fun `upsert and removeNode manage mesh models`() {
        val reg = ModelRegistry()
        val mesh = ModelSpec(
            id = "qwen2.5-7b",
            displayName = "Server Qwen 7B",
            backend = ModelBackend.MESH,
            nodeId = "node-A",
            scores = CapabilityScores(reasoning = 6, coding = 6, cost = 9),
        )
        reg.upsert(mesh)
        assertNotNull(reg.byKey(mesh.key))
        assertEquals(8, reg.all().size)

        reg.removeNode("node-A")
        assertNull(reg.byKey(mesh.key))
        assertEquals(7, reg.all().size)
    }

    @Test
    fun `setEnabled hides a model from the candidate list but keeps it catalogued`() {
        val reg = ModelRegistry()
        val groqKey = reg.all().first { it.provider == "GROQ" }.key
        reg.setEnabled(groqKey, false)
        assertFalse(reg.enabled().any { it.key == groqKey })
        assertNotNull(reg.byKey(groqKey)) // still in the catalogue
    }

    @Test
    fun `applyRemote overrides seeded scores without an app update`() {
        val reg = ModelRegistry()
        val anthropicKey = reg.all().first { it.provider == "ANTHROPIC" }.key
        val patch = """
            [{"id":"claude-sonnet-4-6","displayName":"Claude patched","backend":"CLOUD",
              "provider":"ANTHROPIC","scores":{"reasoning":2,"coding":2,"speed":1,"cost":1}}]
        """.trimIndent()
        val result = reg.applyRemote(patch)
        assertEquals(1, result.getOrNull())
        // Score lowered -> Anthropic no longer ranks first for reasoning.
        assertNotEquals("ANTHROPIC", reg.rankedFor(Capability.REASONING).first().provider)
        assertEquals("Claude patched", reg.byKey(anthropicKey)?.displayName)
    }

    @Test
    fun `applyRemote with malformed json leaves the seed intact`() {
        val reg = ModelRegistry()
        val before = reg.all().size
        val result = reg.applyRemote("{ not an array")
        assertTrue(result.isFailure)
        assertEquals(before, reg.all().size)
    }

    private fun assertNotEquals(unexpected: Any?, actual: Any?) =
        assertFalse("expected not <$unexpected>", unexpected == actual)
}
