package com.kianirani.jarvis.router.backend

import com.kianirani.jarvis.router.capability.CapabilityRequest
import com.kianirani.jarvis.router.orchestrator.DecisionObject
import com.kianirani.jarvis.router.orchestrator.Intent
import com.kianirani.jarvis.router.orchestrator.Modality
import com.kianirani.jarvis.router.registry.Capability
import com.kianirani.jarvis.router.registry.ModelBackend
import com.kianirani.jarvis.router.registry.ModelSpec
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendRouterTest {

    private fun spec(id: String, backend: ModelBackend, provider: String? = null, node: String? = null) =
        ModelSpec(id = id, displayName = id, backend = backend, provider = provider, nodeId = node)

    private fun decision(vararg candidates: ModelSpec) = DecisionObject(
        intent = Intent.CHAT, modality = Modality.TEXT,
        request = CapabilityRequest(Capability.REASONING),
        candidates = candidates.toList(), chosen = candidates.firstOrNull(), reason = "test",
    )

    /** Scriptable backend: succeeds for listed ids, records call order. */
    private class FakeBackend(
        override val kind: ModelBackend,
        private val succeedIds: Set<String> = emptySet(),
    ) : Backend {
        val calls = mutableListOf<String>()
        override suspend fun generate(spec: ModelSpec, message: String): Result<BackendReply> {
            calls += spec.id
            return if (spec.id in succeedIds) Result.success(BackendReply("ok:${spec.id}", spec))
            else Result.failure(RuntimeException("fail ${spec.id}"))
        }
    }

    private fun router(vararg backends: Backend) =
        BackendRouter(backends.associateBy { it.kind })

    @Test
    fun `first successful candidate wins and later ones are not called`() = runTest {
        val cloud = FakeBackend(ModelBackend.CLOUD, succeedIds = setOf("groq-1", "openai-1"))
        val r = router(cloud)
        val res = r.execute(
            decision(
                spec("groq-1", ModelBackend.CLOUD, provider = "GROQ"),
                spec("openai-1", ModelBackend.CLOUD, provider = "OPENAI"),
            ),
            "hi",
        )
        assertTrue(res.isSuccess)
        assertEquals("ok:groq-1", res.getOrNull()?.text)
        assertEquals(listOf("groq-1"), cloud.calls) // openai-1 never reached
    }

    @Test
    fun `substitutes to the next candidate when the first fails`() = runTest {
        val cloud = FakeBackend(ModelBackend.CLOUD, succeedIds = setOf("openai-1"))
        val r = router(cloud)
        val res = r.execute(
            decision(
                spec("groq-1", ModelBackend.CLOUD, provider = "GROQ"),
                spec("openai-1", ModelBackend.CLOUD, provider = "OPENAI"),
            ),
            "hi",
        )
        assertEquals("ok:openai-1", res.getOrNull()?.text)
        assertEquals(listOf("groq-1", "openai-1"), cloud.calls)
    }

    @Test
    fun `falls back across backend kinds cloud then local`() = runTest {
        val cloud = FakeBackend(ModelBackend.CLOUD, succeedIds = emptySet())
        val local = FakeBackend(ModelBackend.LOCAL, succeedIds = setOf("local"))
        val r = router(cloud, local)
        val res = r.execute(
            decision(
                spec("groq-1", ModelBackend.CLOUD, provider = "GROQ"),
                spec("local", ModelBackend.LOCAL),
            ),
            "hi",
        )
        assertEquals("ok:local", res.getOrNull()?.text)
        assertEquals(listOf("groq-1"), cloud.calls)
        assertEquals(listOf("local"), local.calls)
    }

    @Test
    fun `empty candidate list fails with NO_CANDIDATE`() = runTest {
        val res = router(FakeBackend(ModelBackend.CLOUD)).execute(decision(), "hi")
        assertTrue(res.isFailure)
        assertTrue(res.exceptionOrNull()?.message?.contains("NO_CANDIDATE") == true)
    }

    @Test
    fun `real LocalBackend with no engine fails cleanly without throwing`() = runTest {
        val r = router(LocalBackend())
        val res = r.execute(decision(spec("local", ModelBackend.LOCAL)), "hi")
        assertTrue(res.isFailure)
        assertTrue(res.exceptionOrNull()?.message?.contains("LOCAL_MODEL_UNAVAILABLE") == true)
    }
}
