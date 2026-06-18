package com.kianirani.jarvis.core.perception

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.util.Base64

/**
 * VCF-M1 — per-provider image encoding (PRD §8.3, AutoGen `Image.to_openai_format`).
 * Turns raw image bytes into the JSON content shape each provider expects, with a
 * magic-byte MIME sniff. Pure (uses `java.util.Base64`, not `android.util.Base64`) so it
 * is unit-tested against fixtures.
 */
object ImageEncoding {
    enum class Provider { OPENAI, ANTHROPIC, GEMINI }

    fun encode(bytes: ByteArray, provider: Provider, mime: String = sniffMime(bytes)): JsonObject {
        val b64 = Base64.getEncoder().encodeToString(bytes)
        return when (provider) {
            Provider.OPENAI -> buildJsonObject {
                put("type", "image_url")
                putJsonObject("image_url") { put("url", "data:$mime;base64,$b64") }
            }
            Provider.ANTHROPIC -> buildJsonObject {
                put("type", "image")
                putJsonObject("source") {
                    put("type", "base64")
                    put("media_type", mime)
                    put("data", b64)
                }
            }
            Provider.GEMINI -> buildJsonObject {
                putJsonObject("inline_data") {
                    put("mime_type", mime)
                    put("data", b64)
                }
            }
        }
    }

    /** Sniff a MIME type from magic bytes; defaults to image/png. */
    fun sniffMime(bytes: ByteArray): String = when {
        bytes.startsWith(0xFF, 0xD8, 0xFF) -> "image/jpeg"
        bytes.startsWith(0x89, 0x50, 0x4E, 0x47) -> "image/png"
        bytes.startsWith(0x47, 0x49, 0x46) -> "image/gif"
        bytes.size >= 12 && bytes.regionMatches(8, 0x57, 0x45, 0x42, 0x50) -> "image/webp"
        else -> "image/png"
    }

    private fun ByteArray.startsWith(vararg prefix: Int): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it].toByte() }

    private fun ByteArray.regionMatches(offset: Int, vararg expected: Int): Boolean =
        size >= offset + expected.size && expected.indices.all { this[offset + it] == expected[it].toByte() }
}
