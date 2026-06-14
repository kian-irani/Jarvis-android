package com.kianirani.jarvis.router.capability

import com.kianirani.jarvis.data.ai.AiProvider
import com.kianirani.jarvis.data.ai.AiProviderStore
import com.kianirani.jarvis.router.registry.Capability
import com.kianirani.jarvis.router.registry.CapabilityScores
import com.kianirani.jarvis.router.registry.ModelBackend
import com.kianirani.jarvis.router.registry.ModelRegistry
import com.kianirani.jarvis.router.registry.ModelSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityRouterTest {

    /** Minimal in-memory store: only [configured] matters for reachability tests. */
    private class FakeStore(private val withTokens: Set<AiProvider>) : AiProviderStore {
        override fun tokens(p: AiProvider) = if (p in withTokens) listOf("k") else emptyList()
        override fun addToken(p: AiProvider, token: String) = Unit
        override fun removeToken(p: AiProvider, token: String) = Unit
        override fun clear(p: AiProvider) = Unit
        override fun configured() = AiProvider.entries.filter { it in withTokens }
        override fun model(p: AiProvider) = p.defaultModel
        override fun setModel(p: AiProvider, model: String) = Unit
    }

    @Test
    fun `cloud candidates are limited to providers with a token`() {
        val router = CapabilityRouter(ModelRegistry(), FakeStore(setOf(AiProvider.GROQ)))
        val cloud = router.candidates(CapabilityRequest(Capability.SPEED))
            .filter { it.backend == ModelBackend.CLOUD }
        assertTrue(cloud.isNotEmpty())
        assertTrue(cloud.all { it.provider == "GROQ" })
    }

    @Test
    fun `local model is always reachable even with no tokens`() {
        val router = CapabilityRouter(ModelRegistry(), FakeStore(emptySet()))
        val cands = router.candidates(CapabilityRequest(Capability.REASONING))
        assertTrue(cands.any { it.backend == ModelBackend.LOCAL })
        // No tokens -> no cloud candidates.
        assertTrue(cands.none { it.backend == ModelBackend.CLOUD })
    }

    @Test
    fun `best picks strongest reachable reasoner`() {
        val router = CapabilityRouter(ModelRegistry(), FakeStore(setOf(AiProvider.ANTHROPIC, AiProvider.GROQ)))
        val best = router.best(CapabilityRequest(Capability.REASONING))
        assertEquals("ANTHROPIC", best?.provider)
    }

    @Test
    fun `localOnly ignores cloud even when tokens exist`() {
        val router = CapabilityRouter(ModelRegistry(), FakeStore(setOf(AiProvider.ANTHROPIC)))
        val cands = router.candidates(CapabilityRequest(Capability.REASONING, localOnly = true))
        assertTrue(cands.isNotEmpty())
        assertTrue(cands.all { it.backend == ModelBackend.LOCAL })
    }

    @Test
    fun `mesh model is offered when present`() {
        val reg = ModelRegistry()
        reg.upsert(
            ModelSpec(
                id = "qwen2.5-7b", displayName = "Server Qwen", backend = ModelBackend.MESH,
                nodeId = "node-A", scores = CapabilityScores(reasoning = 6, cost = 9),
            ),
        )
        val router = CapabilityRouter(reg, FakeStore(emptySet()))
        val cands = router.candidates(CapabilityRequest(Capability.REASONING))
        assertTrue(cands.any { it.backend == ModelBackend.MESH && it.nodeId == "node-A" })
    }

    @Test
    fun `requireVision with a vision-less provider yields no cloud candidate`() {
        // Groq is text-only; asking for vision should drop it, leaving only local (no vision either).
        val router = CapabilityRouter(ModelRegistry(), FakeStore(setOf(AiProvider.GROQ)))
        val best = router.best(CapabilityRequest(Capability.VISION, needsVision = true))
        assertNull(best) // no reachable vision-capable model
    }
}
