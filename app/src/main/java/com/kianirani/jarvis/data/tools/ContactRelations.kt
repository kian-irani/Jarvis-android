package com.kianirani.jarvis.data.tools

/**
 * Device-bug fix (2026-06-19, user report on v67: "messaging/calling didn't understand").
 *
 * A spoken target is usually a **relationship word** ("مامان", "بابا", "mom") while the
 * contact is saved under a different label ("مادر", a real name, …). The old lookup did a
 * single `DISPLAY_NAME LIKE '%مامان%'`, so "به مامان زنگ بزن" failed whenever the contact
 * wasn't literally named "مامان". This pure map expands a relation into an **ordered list
 * of candidate name fragments** to try, so [ContactResolver] can match "مادر" when the user
 * said "مامان" (and vice-versa). Pure → JVM-testable; no Android.
 */
object ContactRelations {

    // Each synonym maps to the same ordered candidate list (Persian first, then English).
    private val MAP: Map<String, List<String>> = buildMap {
        fun rel(synonyms: List<String>, candidates: List<String>) =
            synonyms.forEach { put(it.lowercase(), candidates) }

        rel(listOf("مامان", "ماما", "مامانی", "مادر", "مام", "mom", "mommy", "mum", "mother"),
            listOf("مامان", "مادر", "ماما", "mom", "mother"))
        rel(listOf("بابا", "بابایی", "پدر", "baba", "dad", "daddy", "father"),
            listOf("بابا", "پدر", "dad", "father"))
        rel(listOf("خاله", "aunt", "auntie"), listOf("خاله", "aunt"))
        rel(listOf("عمه"), listOf("عمه", "aunt"))
        rel(listOf("دایی", "uncle"), listOf("دایی", "uncle"))
        rel(listOf("عمو"), listOf("عمو", "uncle"))
        rel(listOf("داداش", "برادر", "bro", "brother"), listOf("داداش", "برادر", "brother"))
        rel(listOf("خواهر", "آبجی", "sis", "sister"), listOf("خواهر", "آبجی", "sister"))
        rel(listOf("همسر", "زنم", "خانمم", "wife"), listOf("همسر", "wife", "زنم"))
        rel(listOf("شوهر", "شوهرم", "husband"), listOf("همسر", "شوهر", "husband"))
        rel(listOf("مامان‌بزرگ", "مامان بزرگ", "مادربزرگ", "grandma", "granny"),
            listOf("مادربزرگ", "مامان‌بزرگ", "grandma"))
        rel(listOf("بابابزرگ", "بابا بزرگ", "پدربزرگ", "grandpa", "grandad"),
            listOf("پدربزرگ", "بابابزرگ", "grandpa"))
    }

    /**
     * Ordered candidate name fragments for [name]: the spoken name first (so an exact
     * contact still wins), then any relationship synonyms. Always at least the input.
     */
    fun candidates(name: String): List<String> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return emptyList()
        val synonyms = MAP[trimmed.lowercase()].orEmpty()
        return (listOf(trimmed) + synonyms).distinct()
    }
}
