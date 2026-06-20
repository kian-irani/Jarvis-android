package com.kianirani.jarvis.core.knowledge

import kotlinx.serialization.Serializable

/**
 * A2 — the personal Knowledge Graph (Agent-OS §, "گره‌ها (Person/Project/Server/Service/Token) +
 * یال‌ها؛ استخراج خودکار از مکالمات؛ کوئری روابط"). A directed graph of the people, projects,
 * servers, services and tokens Vision learns about, with labelled relations between them, so the
 * agent can answer "which server runs the VPN?" or "who's on the Kian project?".
 *
 * Pure, serializable, deterministic → JVM-tested. Extracting entities/relations from chat (NER /
 * the model) and persisting to Room are the on-device half; this is the queryable core.
 */
enum class EntityType { PERSON, PROJECT, SERVER, SERVICE, TOKEN, APP, PLACE, NOTE, OTHER }

@Serializable
data class Entity(
    val id: String,
    val name: String,
    val type: EntityType,
    val attrs: Map<String, String> = emptyMap(),
)

/** A directed, labelled edge: [from] —[label]→ [to] (e.g. project —"runs_on"→ server). */
@Serializable
data class Relation(val from: String, val to: String, val label: String)

class KnowledgeGraph {

    private val entities = LinkedHashMap<String, Entity>()
    private val relations = LinkedHashSet<Relation>()

    /** Add or replace an entity by id. */
    fun addEntity(entity: Entity) {
        entities[entity.id] = entity
    }

    /** Add a relation (deduped). Endpoints don't have to exist yet (graph can be sparse). */
    fun addRelation(relation: Relation) {
        relations.add(relation)
    }

    fun entity(id: String): Entity? = entities[id]

    fun entitiesOfType(type: EntityType): List<Entity> = entities.values.filter { it.type == type }

    /** Relations touching [id] in either direction. */
    fun relationsOf(id: String): List<Relation> = relations.filter { it.from == id || it.to == id }

    /** Entities directly connected to [id] (either direction), in id order, deduped. */
    fun neighbors(id: String): List<Entity> =
        relations.asSequence()
            .mapNotNull {
                when (id) {
                    it.from -> it.to
                    it.to -> it.from
                    else -> null
                }
            }
            .distinct()
            .mapNotNull { entities[it] }
            .toList()

    /** Entities reached from [id] by an outgoing relation with [label] (e.g. "runs_on"). */
    fun related(id: String, label: String): List<Entity> =
        relations.filter { it.from == id && it.label == label }.mapNotNull { entities[it.to] }

    /** Case-insensitive name search. */
    fun findByName(query: String): List<Entity> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return entities.values.filter { it.name.lowercase().contains(q) }
    }

    /** Remove an entity and every relation touching it. */
    fun removeEntity(id: String) {
        entities.remove(id)
        relations.removeAll { it.from == id || it.to == id }
    }

    fun allEntities(): List<Entity> = entities.values.toList()
}
