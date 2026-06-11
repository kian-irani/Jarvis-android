package com.kianirani.jarvis.brain.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Verifies a discovered/entered brain is actually reachable before finishing setup. */
fun interface BrainHandshake {
    suspend fun check(host: String, port: Int): Boolean
}

/** Real handshake: GET http://host:port/health with a short timeout. */
class HttpBrainHandshake(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build(),
) : BrainHandshake {
    override suspend fun check(host: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            client.newCall(Request.Builder().url("http://$host:$port/health").build())
                .execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }
}

/** Live mDNS scan surfaced as a callback of the current candidate list. */
fun interface DiscoveryScanner {
    /** Starts scanning; invoke [AutoCloseable.close] to stop. */
    fun scan(onUpdate: (List<BrainCandidate>) -> Unit): AutoCloseable
}

/** [DiscoveryScanner] backed by [NsdDiscovery], accumulating found/lost services. */
class NsdDiscoveryScanner(private val nsd: NsdDiscovery) : DiscoveryScanner {
    override fun scan(onUpdate: (List<BrainCandidate>) -> Unit): AutoCloseable {
        val found = LinkedHashMap<String, BrainCandidate>()
        nsd.discover(
            onFound = { c -> synchronized(found) { found[c.name] = c; onUpdate(found.values.toList()) } },
            onLost = { name -> synchronized(found) { found.remove(name); onUpdate(found.values.toList()) } },
        )
        return AutoCloseable { nsd.stopDiscovery() }
    }
}
