package com.kianirani.jarvis.router.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.InputStream

/**
 * LM2.1 — the real [LocalModelManager.ModelSource] over OkHttp with HTTP **Range** so a model
 * download resumes from where a partial `.part` left off instead of restarting (GGUF files are
 * hundreds of MB). Streams the body bytes from [fromByte] to the manager, which verifies the
 * pinned SHA-256. OkHttp is already on the classpath (ktor-client-okhttp) — no new dependency.
 */
class OkHttpModelSource(private val client: OkHttpClient = OkHttpClient()) : LocalModelManager.ModelSource {

    override suspend fun open(url: String, fromByte: Long): InputStream = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(url)
        if (fromByte > 0) builder.header("Range", "bytes=$fromByte-")
        val response = client.newCall(builder.build()).execute()
        if (!response.isSuccessful) {
            response.close()
            throw IOException("model download failed: HTTP ${response.code}")
        }
        // 206 Partial Content when resuming; 200 when starting fresh. The body stream closes the
        // response when the manager finishes reading it.
        response.body?.byteStream() ?: run {
            response.close()
            throw IOException("empty model response body")
        }
    }
}
