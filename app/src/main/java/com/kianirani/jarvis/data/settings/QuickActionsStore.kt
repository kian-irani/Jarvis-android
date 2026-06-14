package com.kianirani.jarvis.data.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persisted order of the home quick-actions row (v12, 2026-06-14). Order is a
 * comma-separated list of [QuickAction] names; [reset] restores the default
 * (spec: "Reset Layout"). [move] shifts one action left/right (edit-mode arrows).
 */
enum class QuickAction(val label: String, val glyph: String) {
    APPS("Apps", "▦"),
    FILES("Files", "🗀"),
    BROWSER("Browser", "🌐"),
    TASKS("Tasks", "✓"),
    AUTOMATION("Automation", "⚡"),
    AGENTS("Agents", "🤖"),
}

@Singleton
class QuickActionsStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("vision_quick_actions", Context.MODE_PRIVATE)
    private val default = QuickAction.entries.toList()

    private val _order = MutableStateFlow(load())
    val order: StateFlow<List<QuickAction>> = _order

    private fun load(): List<QuickAction> {
        val saved = prefs.getString(KEY, null) ?: return default
        val parsed = saved.split(",").mapNotNull { runCatching { QuickAction.valueOf(it) }.getOrNull() }
        // Heal against added/removed actions across versions.
        val complete = parsed + default.filter { it !in parsed }
        return complete.distinct()
    }

    private fun persist(list: List<QuickAction>) {
        _order.value = list
        prefs.edit().putString(KEY, list.joinToString(",") { it.name }).apply()
    }

    /** Move [action] by [delta] (-1 left, +1 right), clamped. */
    fun move(action: QuickAction, delta: Int) {
        val list = _order.value.toMutableList()
        val i = list.indexOf(action)
        val j = (i + delta).coerceIn(0, list.size - 1)
        if (i < 0 || i == j) return
        list.removeAt(i); list.add(j, action)
        persist(list)
    }

    fun reset() {
        prefs.edit().remove(KEY).apply()
        _order.value = default
    }

    private companion object { const val KEY = "order" }
}
