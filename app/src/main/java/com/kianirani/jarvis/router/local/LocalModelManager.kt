package com.kianirani.jarvis.router.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VISION BRAIN V1 — Module 17 (Local AI Engine / LM2): on-demand model download.
 *
 * Streams a [LocalModelDescriptor]'s GGUF into the app's models dir with:
 *  - **resume** — bytes already in the `.part` file are kept and the transport is asked
 *    to continue from that offset (HTTP Range), so a dropped 0.4 GB download isn't restarted,
 *  - a **storage budget** check before committing more bytes,
 *  - **progress** reporting via [state] for the Setup Wizard,
 *  - a **fail-closed SHA-256 pin** — a finished file whose hash doesn't match the pin is
 *    deleted and refused, never handed to the (LM1) engine.
 *
 * Pure/JVM-testable: file IO uses a plain [File] dir (a temp dir in tests) and the network
 * is behind the [ModelSource] seam, so the resume/verify/budget logic runs with no Android
 * or real download.
 */
@Singleton
class LocalModelManager internal constructor(
    private val modelsDir: File,
) {
    @Inject constructor(@ApplicationContext context: Context) : this(File(context.filesDir, "models"))

    sealed interface State {
        data object Idle : State
        data class Downloading(val received: Long, val total: Long) : State {
            val fraction: Float get() = if (total > 0) (received.toFloat() / total).coerceIn(0f, 1f) else 0f
        }
        data object Verifying : State
        data class Ready(val file: File) : State
        data class Failed(val reason: String) : State
    }

    /** Streams the bytes of [url] starting at [fromByte] (HTTP Range on Android). */
    fun interface ModelSource {
        suspend fun open(url: String, fromByte: Long): InputStream
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun fileFor(d: LocalModelDescriptor): File = File(modelsDir, d.fileName)
    fun isReady(d: LocalModelDescriptor): Boolean = fileFor(d).exists()
    fun usedBytes(): Long =
        if (modelsDir.exists()) modelsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L

    /**
     * Ensure [d] is downloaded and verified. Idempotent (a present, valid file returns
     * immediately). [budgetBytes] caps total models-dir size. Resumes a partial `.part`.
     */
    suspend fun download(
        d: LocalModelDescriptor,
        source: ModelSource,
        budgetBytes: Long,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ): Result<File> = withContext(Dispatchers.IO) {
        val finalFile = fileFor(d)
        if (finalFile.exists()) {
            return@withContext if (sha256(finalFile) == d.sha256) {
                _state.value = State.Ready(finalFile); Result.success(finalFile)
            } else {
                finalFile.delete()
                fail("MODEL_CHECKSUM_MISMATCH — refused unverified ${d.id}")
            }
        }
        modelsDir.mkdirs()
        val part = File(modelsDir, "${d.fileName}.part")
        val offset = if (part.exists()) part.length() else 0L

        val remaining = (d.sizeBytes - offset).coerceAtLeast(0L)
        if (usedBytes() + remaining > budgetBytes) {
            return@withContext fail("STORAGE_BUDGET — ${d.id} would exceed the ${budgetBytes}B budget")
        }

        var received = offset
        _state.value = State.Downloading(received, d.sizeBytes)
        runCatching {
            source.open(d.url, offset).use { input ->
                FileOutputStream(part, /* append = */ true).use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        received += n
                        _state.value = State.Downloading(received, d.sizeBytes)
                        onProgress(received, d.sizeBytes)
                    }
                }
            }
        }.onFailure { return@withContext fail("DOWNLOAD_FAILED — ${it.message}") }

        if (part.length() != d.sizeBytes) {
            // Incomplete (connection dropped). Keep .part so a later call resumes.
            return@withContext fail("INCOMPLETE — got ${part.length()} of ${d.sizeBytes} bytes; retry to resume")
        }
        _state.value = State.Verifying
        if (sha256(part) != d.sha256) {
            part.delete()
            return@withContext fail("MODEL_CHECKSUM_MISMATCH — refused unverified ${d.id}")
        }
        if (!part.renameTo(finalFile)) {
            part.copyTo(finalFile, overwrite = true); part.delete()
        }
        _state.value = State.Ready(finalFile)
        Result.success(finalFile)
    }

    /** Delete a downloaded (or partial) model to reclaim space. */
    fun delete(d: LocalModelDescriptor) {
        fileFor(d).delete()
        File(modelsDir, "${d.fileName}.part").delete()
        _state.value = State.Idle
    }

    private fun fail(reason: String): Result<File> {
        _state.value = State.Failed(reason)
        return Result.failure(IllegalStateException(reason))
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
