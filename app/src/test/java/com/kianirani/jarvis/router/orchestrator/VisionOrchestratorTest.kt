package com.kianirani.jarvis.router.orchestrator

import com.kianirani.jarvis.data.ai.AiProvider
import com.kianirani.jarvis.data.ai.AiProviderStore
import com.kianirani.jarvis.router.capability.CapabilityRouter
import com.kianirani.jarvis.router.registry.ModelBackend
import com.kianirani.jarvis.router.registry.ModelRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisionOrchestratorTest {

    private class FakeStore(private val withTokens: Set<AiProvider>) : AiProviderStore {
        override fun tokens(p: AiProvider) = if (p in withTokens) listOf("k") else emptyList()
        override fun addToken(p: AiProvider, token: String) = Unit
        override fun removeToken(p: AiProvider, token: String) = Unit
        override fun clear(p: AiProvider) = Unit
        override fun configured() = AiProvider.entries.filter { it in withTokens }
        override fun model(p: AiProvider) = p.defaultModel
        override fun setModel(p: AiProvider, model: String) = Unit
    }

    private fun orchestrator(tokens: Set<AiProvider> = AiProvider.entries.toSet()): VisionOrchestrator {
        val reg = ModelRegistry()
        return VisionOrchestrator(IntentClassifier(), CapabilityRouter(reg, FakeStore(tokens)))
    }

    @Test
    fun `code request routes to a CODE intent`() {
        val d = orchestrator().decide("fix this kotlin compile error in my function")
        assertEquals(Intent.CODE, d.intent)
        assertEquals(Modality.TEXT, d.modality)
        assertTrue(d.hasAnswerer)
    }

    @Test
    fun `persian reasoning request routes to REASONING`() {
        val d = orchestrator().decide("چرا این معماری بهتر است؟ مقایسه کن")
        assertEquals(Intent.REASONING, d.intent)
    }

    @Test
    fun `image flag forces IMAGE intent and vision requirement`() {
        val d = orchestrator().decide("what is in this screenshot", hasImage = true)
        assertEquals(Intent.IMAGE, d.intent)
        assertEquals(Modality.IMAGE, d.modality)
        assertTrue(d.request.needsVision)
        // Chosen model must support vision.
        assertTrue(d.chosen!!.supportsVision)
    }

    @Test
    fun `voice short lookup is QUICK and audio modality`() {
        val d = orchestrator().decide("time in tokyo", isVoice = true)
        assertEquals(Intent.QUICK, d.intent)
        assertEquals(Modality.AUDIO, d.modality)
    }

    @Test
    fun `action request needs tools`() {
        val d = orchestrator().decide("turn on the flashlight")
        assertEquals(Intent.ACTION, d.intent)
        assertTrue(d.request.needsTools)
    }

    @Test
    fun `privacy mode picks a local model only`() {
        val d = orchestrator().decide("summarize this", privacyMode = true)
        assertTrue(d.request.localOnly)
        assertEquals(ModelBackend.LOCAL, d.chosen?.backend)
    }

    @Test
    fun `no tokens still answers via the local model and never hard-fails`() {
        val d = orchestrator(tokens = emptySet()).decide("hello there friend")
        // Local model is always reachable, so there is always an answerer.
        assertTrue(d.hasAnswerer)
        assertEquals(ModelBackend.LOCAL, d.chosen?.backend)
    }

    @Test
    fun `vision request with no vision-capable reachable model has no answerer`() {
        // Only Groq (text-only) has a token -> nothing can satisfy vision.
        val d = orchestrator(tokens = setOf(AiProvider.GROQ)).decide("describe", hasImage = true)
        assertTrue(d.request.needsVision)
        assertNull(d.chosen)
        assertFalse(d.hasAnswerer)
    }
}
