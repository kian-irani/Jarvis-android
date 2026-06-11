package com.kianirani.jarvis.brain.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Verifies a discovered/entered brain is reachable — and, when [token] is known, that it owns the pairing secret (X-Pair-Ack = sha256(token)). */
fun interface BrainHandshake {
    suspend fun check(host: String, port: Int, token: String?): Boolean
}

internal fun sha256Hex(v: String): String =
    java.security.MessageDigest.getInstance("SHA-256").digest(v.encodeToByteArray())
        .joinToString("") { "%02x".format(it) }

/** Real handshake: GET http://host:port/health with a short timeout. */
class HttpBrainHandshake(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build(),
) : BrainHandshake {
    override suspend fun check(host: String, port: Int, token: String?): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            client.newCall(Request.Builder().url("http://$host:$port/health").build())
                .execute().use { res ->
                    val ack = res.header("X-Pair-Ack")
                    // rogue-brain guard: if we know the token AND the brain echoes an ack, it must match
                    res.isSuccessful && (token == null || ack == null || ack == sha256Hex(token))
                }
        }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
            .getOrDefault(false)
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
        // snapshot inside the lock, call out after releasing it (no lock-while-calling-out)
        nsd.discover(
            onFound = { c -> onUpdate(synchronized(found) { found[c.name] = c; found.values.toList() }) },
            onLost = { name -> onUpdate(synchronized(found) { found.remove(name); found.values.toList() }) },
        )
        return AutoCloseable { nsd.stopDiscovery() }
    }
}
