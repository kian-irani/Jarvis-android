package com.kianirani.jarvis.brain.data

import com.kianirani.jarvis.brain.score.BrainScoreCalculator
import com.kianirani.jarvis.brain.score.DeviceMetrics
import com.kianirani.jarvis.brain.score.NodeMetricsCodec
import com.kianirani.jarvis.brain.server.routes.NodeRegisterRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Periodic node heartbeat: every [intervalMs] POSTs this device's metrics to the
 * brain's /nodes endpoint with a STABLE [nodeId], so the registry refreshes
 * `last_seen` on the same row (see NodeRepository.register) and the Election UI
 * sees the node as online within its 90s freshness window.
 */
class HeartbeatSender(
    private val nodeId: String,
    private val nodeName: String,
    private val address: String,
    private val brainBaseUrl: () -> String,
    private val metrics: () -> DeviceMetrics,
    private val client: OkHttpClient = OkHttpClient(),
    private val intervalMs: Long = 30_000,
) {
    private val json = Json { encodeDefaults = true }

    fun start(scope: CoroutineScope): Job = scope.launch {
        while (isActive) {
            runCatching { beat() } // brain may be down; next beat retries
            delay(intervalMs)
        }
    }

    /** One heartbeat. Throws on network/HTTP failure so callers/tests can observe it. */
    suspend fun beat(): Unit = withContext(Dispatchers.IO) {
        val m = metrics()
        val body = json.encodeToString(
            NodeRegisterRequest.serializer(),
            NodeRegisterRequest(
                name = nodeName,
                address = address,
                capabilities = NodeMetricsCodec.encode(m),
                brain_score = BrainScoreCalculator.score(m),
                id = nodeId,
            ),
        )
        client.newCall(
            Request.Builder()
                .url("${brainBaseUrl().trimEnd('/')}/nodes")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build(),
        ).execute().use { res ->
            check(res.isSuccessful) { "heartbeat failed: HTTP ${res.code}" }
        }
    }
}
