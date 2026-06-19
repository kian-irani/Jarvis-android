package com.kianirani.jarvis.data.launcher

/**
 * DS-L3 — undo/redo history for the launcher layout (PRD: "تاریخچه‌ی undo/redo چیدمان").
 *
 * A pure, immutable undo stack over [LauncherLayout] (itself an immutable `@Serializable`
 * value), so every edit-mode mutation — drag, folder, "clean home", "optimize productivity"
 * — is reversible. JVM-testable; the edit-mode UI just calls [record] after a mutation and
 * [undo]/[redo] on the buttons, then pushes the returned layout into [LauncherStore].
 *
 * Semantics (standard editor history): [record] pushes a new present and clears the redo
 * stack; recording the same layout as the present is a no-op (no dead history entries);
 * [undo]/[redo] move the cursor and return the layout to apply (null at the ends); [capacity]
 * bounds the past so a long session can't grow unbounded.
 */
class LayoutHistory(
    initial: LauncherLayout,
    private val capacity: Int = 50,
) {
    private val past = ArrayDeque<LauncherLayout>()
    private val future = ArrayDeque<LauncherLayout>()
    var present: LauncherLayout = initial
        private set

    init {
        require(capacity >= 1) { "capacity must be >= 1" }
    }

    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()

    /** Push [next] as the new present, pushing the old present onto the undo stack. */
    fun record(next: LauncherLayout) {
        if (next == present) return // identical edit — nothing to remember
        past.addLast(present)
        if (past.size > capacity) past.removeFirst()
        future.clear()
        present = next
    }

    /** Step back one edit; returns the restored layout, or null if there's nothing to undo. */
    fun undo(): LauncherLayout? {
        val prev = past.removeLastOrNull() ?: return null
        future.addLast(present)
        present = prev
        return present
    }

    /** Step forward one undone edit; returns the layout, or null if there's nothing to redo. */
    fun redo(): LauncherLayout? {
        val next = future.removeLastOrNull() ?: return null
        past.addLast(present)
        present = next
        return present
    }
}
