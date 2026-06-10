package com.kianirani.jarvis.brain.data

import com.kianirani.jarvis.brain.server.BrainException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

interface Embedder { fun embed(texts: List<String>): List<FloatArray>; val dim: Int }

class ModelManager(
    private val modelDir: File,
    private val expectedSha256: String,
    private val budgetBytes: Long,
    private val download: suspend () -> ByteArray,
) {
    val modelFile: File get() = File(modelDir, "minilm-l12-v2.onnx")
    fun isReady(): Boolean = modelFile.exists()
    fun usedBytes(): Long = modelDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    suspend fun ensureModel() = withContext(Dispatchers.IO) {
        if (isReady()) return@withContext
        val bytes = download()
        if (usedBytes() + bytes.size > budgetBytes) throw BrainException.storageBudget()
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        if (sha != expectedSha256) {
            throw BrainException("MODEL_CHECKSUM_MISMATCH", HttpStatusCode.ServiceUnavailable, "Model checksum mismatch")
        }
        modelFile.parentFile?.mkdirs()
        modelFile.writeBytes(bytes)
    }
}

class EmbeddingRepository(private val manager: ModelManager, private val embedderFactory: () -> Embedder) : com.kianirani.jarvis.brain.server.routes.EmbedPort {
    private var embedder: Embedder? = null
    override fun isReady() = manager.isReady()
    fun usedBytes() = manager.usedBytes()
    suspend fun ensureModel() = manager.ensureModel()

    override fun embed(texts: List<String>): List<FloatArray> {
        if (!manager.isReady()) throw BrainException.modelNotReady()
        val e = embedder ?: embedderFactory().also { embedder = it }
        return e.embed(texts)
    }
}

class OnnxEmbedder(modelFile: File) : Embedder {
    override val dim = 384
    private val env = ai.onnxruntime.OrtEnvironment.getEnvironment()
    private val session = env.createSession(modelFile.absolutePath)

    override fun embed(texts: List<String>): List<FloatArray> =
        texts.map { text ->
            // MVP tokenizer: byte-level fallback (proper WordPiece tokenizer tracked in PLAN.md).
            val ids = text.encodeToByteArray().take(256).map { it.toLong() }.toLongArray()
            val shape = longArrayOf(1, ids.size.toLong())
            ai.onnxruntime.OnnxTensor.createTensor(env, java.nio.LongBuffer.wrap(ids), shape).use { input ->
                ai.onnxruntime.OnnxTensor.createTensor(env, java.nio.LongBuffer.wrap(LongArray(ids.size) { 1L }), shape).use { mask ->
                    session.run(mapOf("input_ids" to input, "attention_mask" to mask)).use { out ->
                        @Suppress("UNCHECKED_CAST")
                        val raw = (out[0].value as Array<Array<FloatArray>>)[0]
                        FloatArray(dim) { d -> raw.map { it[d] }.average().toFloat() }
                    }
                }
            }
        }
}
