package com.kianirani.jarvis.brain.data

import com.kianirani.jarvis.brain.data.db.NodeDao
import com.kianirani.jarvis.brain.data.db.NodeEntity
import java.util.UUID

class NodeRepository(private val dao: NodeDao) {
    suspend fun register(name: String, address: String, capabilities: String, brainScore: Int): String {
        val id = UUID.randomUUID().toString()
        dao.upsert(NodeEntity(id, name, address, capabilities, brainScore, System.currentTimeMillis()))
        return id
    }
    suspend fun list(): List<NodeEntity> = dao.list()
}
