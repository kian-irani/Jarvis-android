package com.kianirani.jarvis.core.perception

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageEncodingTest {

    private fun bytes(vararg b: Int) = ByteArray(b.size) { b[it].toByte() }

    private val png = bytes(0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0)
    private val jpeg = bytes(0xFF, 0xD8, 0xFF, 0)

    @Test fun `sniffs common image formats by magic bytes`() {
        assertEquals("image/png", ImageEncoding.sniffMime(png))
        assertEquals("image/jpeg", ImageEncoding.sniffMime(jpeg))
        assertEquals("image/gif", ImageEncoding.sniffMime(bytes(0x47, 0x49, 0x46, 0x38)))
        assertEquals("image/webp", ImageEncoding.sniffMime(bytes(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50)))
        assertEquals("image/png", ImageEncoding.sniffMime(bytes(1, 2, 3))) // default
    }

    @Test fun `openai encoding uses a base64 data url`() {
        val json = ImageEncoding.encode(jpeg, ImageEncoding.Provider.OPENAI)
        assertEquals("image_url", json["type"]!!.jsonPrimitive.content)
        assertTrue(json["image_url"]!!.jsonObject["url"]!!.jsonPrimitive.content.startsWith("data:image/jpeg;base64,"))
    }

    @Test fun `anthropic encoding uses a base64 source block`() {
        val json = ImageEncoding.encode(png, ImageEncoding.Provider.ANTHROPIC)
        assertEquals("image", json["type"]!!.jsonPrimitive.content)
        val source = json["source"]!!.jsonObject
        assertEquals("base64", source["type"]!!.jsonPrimitive.content)
        assertEquals("image/png", source["media_type"]!!.jsonPrimitive.content)
    }

    @Test fun `gemini encoding uses inline_data`() {
        val json = ImageEncoding.encode(png, ImageEncoding.Provider.GEMINI)
        assertEquals("image/png", json["inline_data"]!!.jsonObject["mime_type"]!!.jsonPrimitive.content)
    }
}
