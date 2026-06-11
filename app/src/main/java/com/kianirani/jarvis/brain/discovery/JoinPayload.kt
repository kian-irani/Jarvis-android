package com.kianirani.jarvis.brain.discovery

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Brain Discovery transport payload (ROADMAP Phase 1). One format for every
 * channel: rendered as a QR code by the brain, scanned or pasted by joining
 * devices. Pure JVM (no android.net.Uri) so it is unit-testable.
 *
 * Wire format: `vision://join?host=<ip-or-dns>&port=<int>&token=<urlencoded>`
 */
data class JoinPayload(val host: String, val port: Int, val token: String) {

    fun encode(): String =
        "$SCHEME://$ACTION?host=${enc(host)}&port=$port&token=${enc(token)}"

    companion object {
        const val SCHEME = "vision"
        const val ACTION = "join"
        private const val PREFIX = "$SCHEME://$ACTION?"

        private fun enc(v: String) = URLEncoder.encode(v, Charsets.UTF_8.name())

        /** Returns null when [raw] is not a valid join payload. */
        fun decode(raw: String): JoinPayload? {
            val trimmed = raw.trim()
            if (!trimmed.startsWith(PREFIX)) return null
            val params = trimmed.removePrefix(PREFIX).split('&').mapNotNull { p ->
                val i = p.indexOf('=')
                if (i <= 0) null
                else p.take(i) to URLDecoder.decode(p.substring(i + 1), Charsets.UTF_8.name())
            }.groupBy({ it.first }, { it.second }).mapValues { it.value.first() }
            val host = params["host"]?.takeIf { it.isNotBlank() } ?: return null
            val port = params["port"]?.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
            val token = params["token"]?.takeIf { it.isNotBlank() } ?: return null
            return JoinPayload(host, port, token)
        }
    }
}
