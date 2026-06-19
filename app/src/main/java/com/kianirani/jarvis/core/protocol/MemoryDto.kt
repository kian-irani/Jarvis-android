package com.kianirani.jarvis.core.protocol

import com.kianirani.jarvis.core.memory.MemoryType
import kotlinx.serialization.Serializable

/**
 * DS-F2 — the wire form of a long-term memory item (CF4 / DS-B3). Surfaces and the
 * Brain exchange these so a memory can be synced cross-device (DS-C3) or browsed by a
 * remote surface without coupling to the Room entity. Pure data, KMP-ready.
 */
@Serializable
data class MemoryDto(
    val content: String,
    val type: MemoryType = MemoryType.FACT,
    val importance: Float = 0.5f,
    /** persisted row id; null for a not-yet-stored draft. */
    val id: String? = null,
    /** epoch millis; 0 when unknown. */
    val createdAt: Long = 0L,
    val metadata: Map<String, String> = emptyMap(),
)
