package com.kianirani.jarvis.core.brain

/**
 * C3 — Shared / Org Brain (PRD §, "Personal → Shared (تیمی) → Organization"). The pure access
 * model that scopes memory/data to a tier: PERSONAL (only you), SHARED (a team), or ORG (the
 * whole org). A query at a given scope may read everything at or below it that it's a member of;
 * writes stay at the item's own scope. Pure → JVM-tested; the synced store + auth are the
 * network half.
 */
enum class BrainTier { PERSONAL, SHARED, ORG }

data class ScopedItem(val id: String, val tier: BrainTier, val ownerOrGroup: String, val content: String)

/** Who's asking: their identity + the groups/orgs they belong to. */
data class Viewer(val userId: String, val groups: Set<String> = emptySet(), val orgs: Set<String> = emptySet())

object BrainScope {

    /** Can [viewer] read [item]? Personal → must own it; Shared → in the group; Org → in the org. */
    fun canRead(viewer: Viewer, item: ScopedItem): Boolean = when (item.tier) {
        BrainTier.PERSONAL -> item.ownerOrGroup == viewer.userId
        BrainTier.SHARED -> item.ownerOrGroup in viewer.groups
        BrainTier.ORG -> item.ownerOrGroup in viewer.orgs
    }

    /** The items [viewer] is allowed to see, preserving order. */
    fun visible(viewer: Viewer, items: List<ScopedItem>): List<ScopedItem> = items.filter { canRead(viewer, it) }
}
