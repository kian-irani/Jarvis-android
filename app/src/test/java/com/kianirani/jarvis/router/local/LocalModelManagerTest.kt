package com.kianirani.jarvis.router.local

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.security.MessageDigest

/** LM2 — on-demand model download: verify, resume, budget, idempotency (fail-closed). */
class LocalModelManagerTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun sha256(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private val payload = ByteArray(5000) { (it % 251).toByte() }
    private fun descriptor(sha: String = sha256(payload)) = LocalModelDescriptor(
        id = "test-model", displayName = "Test", url = "https://example/model.gguf",
        sha256 = sha, sizeBytes = payload.size.toLong(),
    )

    /** Full source: always serves from the requested offset. */
    private fun fullSource() = LocalModelManager.ModelSource { _, from ->
        ByteArrayInputStream(payload.copyOfRange(from.toInt(), payload.size))
    }

    /** Source that serves only [limit] bytes from the offset, then ends (dropped connection). */
    private fun partialSource(limit: Int) = LocalModelManager.ModelSource { _, from ->
        val end = minOf(from.toInt() + limit, payload.size)
        ByteArrayInputStream(payload.copyOfRange(from.toInt(), end))
    }

    private fun manager() = LocalModelManager(tmp.newFolder("models"))

    @Test
    fun `downloads and verifies a fresh model`() = runTest {
        val m = manager()
        val d = descriptor()
        val r = m.download(d, fullSource(), budgetBytes = 1_000_000)
        assertTrue(r.isSuccess)
        assertTrue(m.isReady(d))
        assertEquals(payload.size.toLong(), m.fileFor(d).length())
        assertTrue(m.state.value is LocalModelManager.State.Ready)
    }

    @Test
    fun `checksum mismatch is refused and the file is deleted`() = runTest {
        val m = manager()
        val d = descriptor(sha = "deadbeef") // wrong pin
        val r = m.download(d, fullSource(), budgetBytes = 1_000_000)
        assertTrue(r.isFailure)
        assertFalse(m.isReady(d))
        assertTrue((m.state.value as LocalModelManager.State.Failed).reason.contains("CHECKSUM"))
    }

    @Test
    fun `a dropped download resumes from the partial file`() = runTest {
        val m = manager()
        val d = descriptor()
        // first attempt only delivers 2000 bytes then ends → incomplete
        val first = m.download(d, partialSource(2000), budgetBytes = 1_000_000)
        assertTrue(first.isFailure)
        assertFalse(m.isReady(d))
        // second attempt resumes from offset 2000 and finishes
        val second = m.download(d, fullSource(), budgetBytes = 1_000_000)
        assertTrue(second.isSuccess)
        assertTrue(m.isReady(d))
        assertEquals(payload.size.toLong(), m.fileFor(d).length())
    }

    @Test
    fun `resume offset is honoured by the source`() = runTest {
        val m = manager()
        val d = descriptor()
        m.download(d, partialSource(2000), budgetBytes = 1_000_000) // leaves a 2000-byte .part
        var requestedFrom = -1L
        val spy = LocalModelManager.ModelSource { _, from ->
            requestedFrom = from
            ByteArrayInputStream(payload.copyOfRange(from.toInt(), payload.size))
        }
        m.download(d, spy, budgetBytes = 1_000_000)
        assertEquals(2000L, requestedFrom)
    }

    @Test
    fun `download is refused when it would exceed the storage budget`() = runTest {
        val m = manager()
        val d = descriptor()
        val r = m.download(d, fullSource(), budgetBytes = 1000) // payload is 5000
        assertTrue(r.isFailure)
        assertTrue((m.state.value as LocalModelManager.State.Failed).reason.contains("STORAGE_BUDGET"))
    }

    @Test
    fun `re-downloading a ready model is idempotent and does not refetch`() = runTest {
        val m = manager()
        val d = descriptor()
        m.download(d, fullSource(), budgetBytes = 1_000_000)
        var called = false
        val source = LocalModelManager.ModelSource { _, _ -> called = true; ByteArrayInputStream(ByteArray(0)) }
        val r = m.download(d, source, budgetBytes = 1_000_000)
        assertTrue(r.isSuccess)
        assertFalse("ready model must not hit the network again", called)
    }

    @Test
    fun `delete removes the model and partial file`() = runTest {
        val m = manager()
        val d = descriptor()
        m.download(d, fullSource(), budgetBytes = 1_000_000)
        assertTrue(m.isReady(d))
        m.delete(d)
        assertFalse(m.isReady(d))
    }

    @Test
    fun `progress reaches the full size on success`() = runTest {
        val m = manager()
        val d = descriptor()
        var last = 0L
        m.download(d, fullSource(), budgetBytes = 1_000_000) { received, _ -> last = received }
        assertEquals(payload.size.toLong(), last)
    }
}
