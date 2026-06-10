package com.kianirani.jarvis.brain

import com.kianirani.jarvis.brain.data.ModelManager
import com.kianirani.jarvis.brain.server.BrainException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.security.MessageDigest

class ModelManagerTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun sha256(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    @Test
    fun `download verifies checksum and reports ready`() = runTest {
        val payload = "model-bytes".toByteArray()
        val mm = ModelManager(
            modelDir = tmp.root, expectedSha256 = sha256(payload),
            budgetBytes = 1024, download = { payload },
        )
        mm.ensureModel()
        assertTrue(mm.isReady())
    }

    @Test
    fun `bad checksum deletes file and stays not-ready`() = runTest {
        val mm = ModelManager(
            modelDir = tmp.root, expectedSha256 = "deadbeef",
            budgetBytes = 1024, download = { "corrupt".toByteArray() },
        )
        try { mm.ensureModel(); fail("expected") } catch (e: BrainException) {
            assertEquals("MODEL_CHECKSUM_MISMATCH", e.code)
        }
        assertTrue(!mm.isReady())
    }

    @Test
    fun `budget exceeded throws STORAGE_BUDGET_EXCEEDED`() = runTest {
        val payload = ByteArray(2048)
        val mm = ModelManager(
            modelDir = tmp.root, expectedSha256 = sha256(payload),
            budgetBytes = 1024, download = { payload },
        )
        try { mm.ensureModel(); fail("expected") } catch (e: BrainException) {
            assertEquals("STORAGE_BUDGET_EXCEEDED", e.code)
        }
    }
}
