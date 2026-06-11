package com.kianirani.jarvis.brain.data

import com.kianirani.jarvis.brain.data.db.NodeDao
import com.kianirani.jarvis.brain.data.db.NodeEntity
import com.kianirani.jarvis.brain.score.BrainScoreCalculator
import com.kianirani.jarvis.brain.score.NodeMetricsCodec
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NodeRepository(private val dao: NodeDao) {
    suspend fun register(name: String, address: String, capabilities: String, brainScore: Int, stableId: String? = null): String {
        // Heartbeats reuse their stable id so REPLACE refreshes last_seen instead of duplicating the node.
        val id = stableId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        // When the caller sends raw metrics in capabilities but no score, derive it here
        // so GET /nodes ordering stays correct for clients that never run the calculator.
        val score = if (brainScore == 0) {
            NodeMetricsCodec.decode(capabilities)?.let(BrainScoreCalculator::score) ?: 0
        } else {
            brainScore
        }
        dao.upsert(NodeEntity(id, name, address, capabilities, score, System.currentTimeMillis()))
        return id
    }
    suspend fun list(): List<NodeEntity> = dao.list()
    fun observe(): Flow<List<NodeEntity>> = dao.observe()
}
