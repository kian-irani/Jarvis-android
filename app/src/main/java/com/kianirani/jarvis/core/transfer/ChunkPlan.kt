package com.kianirani.jarvis.core.transfer

/**
 * Device-to-device file transfer (PRD §, "انتقال فایل دستگاه‌به‌دستگاه روی LAN — chunked, resume").
 * The pure chunking math: split a file of [totalBytes] into fixed-size chunks, compute each
 * chunk's byte range, and resume from the next un-acked chunk after an interruption. Keeping this
 * pure makes transfer deterministic and JVM-tested; the LAN socket + the security confirmation are
 * the device half.
 */
data class Chunk(val index: Int, val offset: Long, val size: Int)

object ChunkPlan {

    const val DEFAULT_CHUNK = 256 * 1024 // 256 KiB

    /** Number of chunks for [totalBytes] at [chunkSize]. 0 bytes → 0 chunks. */
    fun chunkCount(totalBytes: Long, chunkSize: Int = DEFAULT_CHUNK): Int {
        require(chunkSize > 0) { "chunkSize must be positive" }
        if (totalBytes <= 0) return 0
        return ((totalBytes + chunkSize - 1) / chunkSize).toInt()
    }

    /** The [Chunk] at [index] (last chunk is short). Throws if out of range. */
    fun chunkAt(index: Int, totalBytes: Long, chunkSize: Int = DEFAULT_CHUNK): Chunk {
        val count = chunkCount(totalBytes, chunkSize)
        require(index in 0 until count) { "chunk $index out of 0..${count - 1}" }
        val offset = index.toLong() * chunkSize
        val size = minOf(chunkSize.toLong(), totalBytes - offset).toInt()
        return Chunk(index, offset, size)
    }

    /** Every chunk in order. */
    fun allChunks(totalBytes: Long, chunkSize: Int = DEFAULT_CHUNK): List<Chunk> =
        (0 until chunkCount(totalBytes, chunkSize)).map { chunkAt(it, totalBytes, chunkSize) }

    /**
     * Resume: the chunks still to send given the set of already-[acked] indices. Returns them in
     * order so the receiver reassembles correctly even after a drop.
     */
    fun remaining(totalBytes: Long, acked: Set<Int>, chunkSize: Int = DEFAULT_CHUNK): List<Chunk> =
        (0 until chunkCount(totalBytes, chunkSize)).filter { it !in acked }.map { chunkAt(it, totalBytes, chunkSize) }

    /** Transfer progress 0f..1f from the acked count. */
    fun progress(totalBytes: Long, acked: Set<Int>, chunkSize: Int = DEFAULT_CHUNK): Float {
        val count = chunkCount(totalBytes, chunkSize)
        if (count == 0) return 1f
        return acked.count { it in 0 until count }.toFloat() / count
    }
}
