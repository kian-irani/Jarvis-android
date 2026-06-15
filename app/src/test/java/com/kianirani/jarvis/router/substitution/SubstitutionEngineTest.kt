package com.kianirani.jarvis.router.substitution

import com.kianirani.jarvis.router.registry.ModelBackend
import com.kianirani.jarvis.router.registry.ModelRegistry
import com.kianirani.jarvis.router.registry.ModelSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** VB5 — Smart Substitution Engine: chain building, cloud cap, local guarantee, privacy. */
class SubstitutionEngineTest {

    private fun spec(id: String, backend: ModelBackend, provider: String? = null, node: String? = null) =
        ModelSpec(id = id, displayName = id, backend = backend, provider = provider, nodeId = node)

    private val local = spec("qwen2.5-0.5b", ModelBackend.LOCAL)

    /** Registry that knows about the on-device model (used for the always-append fallback). */
    private fun registryWithLocal(): ModelRegistry = ModelRegistry().apply { upsert(local) }
    private fun engine(reg: ModelRegistry = registryWithLocal()) = SubstitutionEngine(reg)

    @Test
    fun `dedupes by key while preserving rank order`() {
        val a = spec("a", ModelBackend.CLOUD, provider = "GROQ")
        val b = spec("b", ModelBackend.CLOUD, provider = "OPENAI")
        val chain = engine().chain(listOf(a, b, a), SubstitutionPolicy(alwaysAppendLocal = false))
        assertEquals(listOf("a", "b"), chain.map { it.id })
    }

    @Test
    fun `caps cloud attempts but keeps mesh and local past the cap`() {
        val ranked = listOf(
            spec("c1", ModelBackend.CLOUD, provider = "GROQ"),
            spec("c2", ModelBackend.CLOUD, provider = "OPENAI"),
            spec("c3", ModelBackend.CLOUD, provider = "XAI"),
            spec("mesh1", ModelBackend.MESH, node = "n1"),
            local,
        )
        val chain = engine().chain(ranked, SubstitutionPolicy(maxCloudAttempts = 2))
        assertEquals(listOf("c1", "c2", "mesh1", "qwen2.5-0.5b"), chain.map { it.id })
    }

    @Test
    fun `always appends the on-device model when the chain has none`() {
        val ranked = listOf(spec("c1", ModelBackend.CLOUD, provider = "GROQ"))
        val chain = engine().chain(ranked, SubstitutionPolicy.DEFAULT)
        assertEquals("qwen2.5-0.5b", chain.last().id)
        assertTrue(chain.last().isLocal)
    }

    @Test
    fun `does not duplicate local when it is already in the ranked list`() {
        val ranked = listOf(spec("c1", ModelBackend.CLOUD, provider = "GROQ"), local)
        val chain = engine().chain(ranked, SubstitutionPolicy.DEFAULT)
        assertEquals(1, chain.count { it.isLocal })
        assertEquals(listOf("c1", "qwen2.5-0.5b"), chain.map { it.id })
    }

    @Test
    fun `local-only policy drops cloud and mesh leaving just the device model`() {
        val ranked = listOf(
            spec("c1", ModelBackend.CLOUD, provider = "GROQ"),
            spec("mesh1", ModelBackend.MESH, node = "n1"),
            local,
        )
        val chain = engine().chain(ranked, SubstitutionPolicy.LOCAL_ONLY)
        assertEquals(listOf("qwen2.5-0.5b"), chain.map { it.id })
    }

    @Test
    fun `keepMesh false removes mesh links`() {
        val ranked = listOf(
            spec("c1", ModelBackend.CLOUD, provider = "GROQ"),
            spec("mesh1", ModelBackend.MESH, node = "n1"),
        )
        val chain = engine().chain(ranked, SubstitutionPolicy(keepMesh = false))
        assertFalse(chain.any { it.backend == ModelBackend.MESH })
    }

    @Test
    fun `no enabled local yields a cloud-only chain without crashing`() {
        // The registry self-seeds a local model; disable every local so the
        // always-append fallback has nothing to add.
        val reg = ModelRegistry()
        reg.byBackend(ModelBackend.LOCAL).forEach { reg.setEnabled(it.key, false) }
        val ranked = listOf(spec("c1", ModelBackend.CLOUD, provider = "GROQ"))
        val chain = SubstitutionEngine(reg).chain(ranked, SubstitutionPolicy.DEFAULT)
        assertEquals(listOf("c1"), chain.map { it.id })
    }

    @Test
    fun `seeded local is appended to a cloud-only chain by default`() {
        val ranked = listOf(spec("c1", ModelBackend.CLOUD, provider = "GROQ"))
        val chain = SubstitutionEngine(ModelRegistry()).chain(ranked, SubstitutionPolicy.DEFAULT)
        assertTrue(chain.last().isLocal)
        assertEquals("c1", chain.first().id)
    }

    @Test
    fun `empty input stays empty (no phantom local) when append is off`() {
        val chain = engine().chain(emptyList(), SubstitutionPolicy(alwaysAppendLocal = false))
        assertTrue(chain.isEmpty())
    }
}
