package com.kianirani.jarvis.data.graph

import com.kianirani.jarvis.brain.data.db.CheckpointDao
import com.kianirani.jarvis.brain.data.db.CheckpointEntity
import com.kianirani.jarvis.core.graph.Checkpointer
import com.kianirani.jarvis.core.graph.GraphState
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * VCF-G3 — durable [Checkpointer] backed by Room. Each [save] appends a new revision
 * (monotonic `seq` per thread) with the state serialized to JSON via kotlinx; [load]
 * returns the latest revision, [history] all of them (oldest→newest) for time-travel.
 * The seq-management and JSON round-trip are unit-tested against a fake DAO; the Room
 * query/migration itself follows the repo's existing Room pattern (on-device).
 */
class RoomCheckpointer(
    private val dao: CheckpointDao,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val clock: () -> Long = { System.currentTimeMillis() },
) : Checkpointer {

    override suspend fun save(threadId: String, state: GraphState, cursor: String) {
        val seq = (dao.maxSeq(threadId) ?: 0) + 1
        dao.insert(CheckpointEntity(threadId, seq, cursor, json.encodeToString(state), clock()))
    }

    override suspend fun load(threadId: String): GraphState? =
        dao.latest(threadId)?.let { json.decodeFromString(it.stateJson) }

    override suspend fun loadCursor(threadId: String): String? = dao.latest(threadId)?.cursor

    override suspend fun history(threadId: String): List<GraphState> =
        dao.all(threadId).map { json.decodeFromString(it.stateJson) }
}
