package com.kianirani.jarvis.core.twin

import kotlinx.serialization.Serializable

/**
 * TWIN — the Digital Twin (PRD §14.2): Vision's durable, structured model of the user —
 * their [preferences], daily [routines], known [contacts], active [projects], and app
 * [usage]. It is what makes the assistant feel like it *knows* you across sessions and
 * devices; the agent reads [summary] into its system prompt and writes back as it learns.
 *
 * Pure & serializable (kotlinx) → JVM-tested and ready to persist (DataStore) and sync over
 * the mesh ([merge] is order-independent for the cross-device case, DS-C3). All updates are
 * copy-on-write via [DigitalTwinOps] so the value itself stays immutable.
 */
@Serializable
data class DigitalTwin(
    val preferences: Map<String, String> = emptyMap(),
    val routines: List<Routine> = emptyList(),
    val contacts: Map<String, ContactFact> = emptyMap(),
    val projects: List<String> = emptyList(),
    val usage: Map<String, Int> = emptyMap(),
    val updatedAtMillis: Long = 0L,
)

@Serializable
data class Routine(val id: String, val description: String, val minuteOfDay: Int)

@Serializable
data class ContactFact(val name: String, val relation: String = "", val note: String = "")

object DigitalTwinOps {

    fun setPreference(twin: DigitalTwin, key: String, value: String, now: Long): DigitalTwin =
        twin.copy(preferences = twin.preferences + (key to value), updatedAtMillis = maxOf(now, twin.updatedAtMillis))

    fun recordUsage(twin: DigitalTwin, appId: String, now: Long): DigitalTwin =
        twin.copy(
            usage = twin.usage + (appId to ((twin.usage[appId] ?: 0) + 1)),
            updatedAtMillis = maxOf(now, twin.updatedAtMillis),
        )

    fun upsertContact(twin: DigitalTwin, contact: ContactFact, now: Long): DigitalTwin =
        twin.copy(contacts = twin.contacts + (contact.name to contact), updatedAtMillis = maxOf(now, twin.updatedAtMillis))

    /** Add or replace a routine by id. */
    fun putRoutine(twin: DigitalTwin, routine: Routine, now: Long): DigitalTwin =
        twin.copy(
            routines = twin.routines.filterNot { it.id == routine.id } + routine,
            updatedAtMillis = maxOf(now, twin.updatedAtMillis),
        )

    fun addProject(twin: DigitalTwin, project: String, now: Long): DigitalTwin =
        if (project in twin.projects) twin else
            twin.copy(projects = twin.projects + project, updatedAtMillis = maxOf(now, twin.updatedAtMillis))

    /** The apps the user launches most, highest first. */
    fun topApps(twin: DigitalTwin, n: Int = 5): List<String> =
        twin.usage.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(n).map { it.key }

    /**
     * Merge two replicas of the twin (e.g. phone + desktop). Order-independent: scalar maps
     * (preferences/contacts) take the **newer** replica's value on a key conflict; usage counts
     * are **summed** across devices; routines (by id) and projects are unioned. The result's
     * timestamp is the later of the two.
     */
    fun merge(a: DigitalTwin, b: DigitalTwin): DigitalTwin {
        val (older, newer) = if (a.updatedAtMillis <= b.updatedAtMillis) a to b else b to a
        val prefs = older.preferences + newer.preferences // newer wins on conflict
        val contacts = older.contacts + newer.contacts
        val routines = (older.routines + newer.routines).associateBy { it.id }.values.toList()
        val projects = (older.projects + newer.projects).distinct()
        val usage = (older.usage.keys + newer.usage.keys).associateWith { k ->
            (older.usage[k] ?: 0) + (newer.usage[k] ?: 0)
        }
        return DigitalTwin(prefs, routines, contacts, projects, usage, newer.updatedAtMillis)
    }

    /** A compact, prompt-ready description of the user for the agent's system context. */
    fun summary(twin: DigitalTwin): String {
        val parts = buildList {
            if (twin.preferences.isNotEmpty()) {
                add("Preferences: " + twin.preferences.entries.joinToString(", ") { "${it.key}=${it.value}" })
            }
            if (twin.projects.isNotEmpty()) add("Active projects: " + twin.projects.joinToString(", "))
            if (twin.contacts.isNotEmpty()) add("Known contacts: " + twin.contacts.values.joinToString(", ") { it.name })
            topApps(twin, 3).takeIf { it.isNotEmpty() }?.let { add("Most-used apps: " + it.joinToString(", ")) }
        }
        return parts.joinToString("\n")
    }
}
