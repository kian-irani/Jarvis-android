package com.kianirani.jarvis.core.launcher

/**
 * DS-L1 — AI smart grouping (PRD §, "خوشه‌بندیِ اپ‌ها بر اساسِ usage+category").
 *
 * The pure clustering core: given apps tagged with a [AppGroupCategory] and a usage score
 * (the recency×frequency signal from [com.kianirani.jarvis.data.launcher.AppUsageRanker],
 * DS-L5), produce named **context folders** (Work / Social / Media / Tools / System). Pure
 * & deterministic → JVM-testable; resolving each app's real category from
 * `ApplicationInfo.category` and rendering/persisting the folders are the on-device half.
 *
 * Rules: one folder per category that has at least [minGroupSize] apps; smaller categories
 * are left ungrouped (a one-app folder is noise). Folders are ordered by aggregate usage
 * (the cluster you use most comes first); apps within a folder are sorted by usage desc.
 */
enum class AppGroupCategory(val folderName: String) {
    COMMUNICATION("Social"),
    PRODUCTIVITY("Work"),
    MEDIA("Media"),
    TOOLS("Tools"),
    SYSTEM("System"),
}

/** An app handed to the grouper, already tagged with its category + usage score. */
data class GroupableApp(
    val id: String,
    val label: String,
    val category: AppGroupCategory,
    val usageScore: Float = 0f,
)

/** A produced context folder: its [name] and the app ids it contains, most-used first. */
data class AppGroup(val name: String, val appIds: List<String>)

object AppGrouper {

    const val minGroupSize: Int = 2

    /**
     * Cluster [apps] into context folders. Returns folders ordered by aggregate usage
     * (desc); ties keep category declaration order. Apps in under-[minGroupSize] categories
     * are omitted (left on the home grid) — see [ungrouped].
     */
    fun group(apps: List<GroupableApp>): List<AppGroup> {
        val byCategory = apps.groupBy { it.category }
        return AppGroupCategory.entries // stable category order for ties
            .mapNotNull { cat -> byCategory[cat]?.takeIf { it.size >= minGroupSize }?.let { cat to it } }
            .sortedByDescending { (_, members) -> members.sumOf { it.usageScore.toDouble() } }
            .map { (cat, members) ->
                AppGroup(cat.folderName, members.sortedByDescending { it.usageScore }.map { it.id })
            }
    }

    /** Ids of apps NOT placed in any folder (their category had too few apps to cluster). */
    fun ungrouped(apps: List<GroupableApp>): List<String> {
        val grouped = group(apps).flatMap { it.appIds }.toSet()
        return apps.map { it.id }.filter { it !in grouped }
    }
}
