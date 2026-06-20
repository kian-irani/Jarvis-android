package com.kianirani.jarvis.core.tutor

/**
 * Vision Tutor (PRD §, "راهنمای تعاملی درون‌اپ"). The pure model of an in-app interactive guide:
 * an ordered list of steps, a cursor, and progress, with skip/complete. The Compose overlay
 * renders the current step (highlight + caption); this keeps the flow deterministic and
 * JVM-tested. A step points at a UI anchor by key so the surface can spotlight it.
 */
data class TutorStep(val key: String, val title: String, val body: String)

class TutorScript(private val steps: List<TutorStep>) {

    private var index = 0
    private var done = steps.isEmpty()

    /** The step the user is on, or null when the tour is finished. */
    fun current(): TutorStep? = if (done || index !in steps.indices) null else steps[index]

    /** Advance to the next step; finishes the tour after the last one. Returns the new current. */
    fun next(): TutorStep? {
        if (done) return null
        if (index >= steps.lastIndex) { done = true; return null }
        index++
        return current()
    }

    /** Go back a step (no-op at the start). */
    fun previous(): TutorStep? {
        if (index > 0 && !done) index--
        return current()
    }

    /** End the tour immediately (user tapped Skip). */
    fun skip() { done = true }

    fun isComplete(): Boolean = done

    /** 0f..1f progress through the tour. */
    fun progress(): Float {
        if (steps.isEmpty()) return 1f
        return if (done) 1f else index.toFloat() / steps.size
    }

    /** 1-based step number for the "Step n of m" label. */
    fun position(): Pair<Int, Int> = (index + 1).coerceAtMost(steps.size) to steps.size
}
