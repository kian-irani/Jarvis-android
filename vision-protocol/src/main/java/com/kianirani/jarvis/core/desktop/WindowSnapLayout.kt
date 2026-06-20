package com.kianirani.jarvis.core.desktop

/**
 * DS-WIN4 — window snap geometry (PRD §, "Window manager + AI snap"). The pure layout math that
 * maps a [SnapZone] onto a monitor's work area to produce a target window rectangle. The desktop
 * shell then applies it with Win32 (`SetWindowPos`/`MonitorFromWindow`) — this object is the
 * platform-free brain so snapping is deterministic and JVM-tested. Coordinates are work-area
 * relative (the caller offsets by the monitor origin).
 */
data class Rect(val x: Int, val y: Int, val width: Int, val height: Int)

/** Where to snap a window within the work area. */
enum class SnapZone {
    LEFT, RIGHT, TOP, BOTTOM, MAXIMIZE,
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, // quarters
    CENTER, // centered, not resized to a fraction
}

object WindowSnapLayout {

    /** Fraction of the work area a centered window occupies on each axis. */
    const val CENTER_FRACTION = 0.6

    /**
     * Target rect for [zone] within a [workWidth]×[workHeight] work area. Halves/quarters tile
     * exactly (right/bottom take the remainder so two halves cover the full area with no gap).
     */
    fun rectFor(zone: SnapZone, workWidth: Int, workHeight: Int): Rect {
        val w = workWidth.coerceAtLeast(0)
        val h = workHeight.coerceAtLeast(0)
        val halfW = w / 2
        val halfH = h / 2
        val rightW = w - halfW // remainder → no 1px gap on odd widths
        val bottomH = h - halfH
        return when (zone) {
            SnapZone.MAXIMIZE -> Rect(0, 0, w, h)
            SnapZone.LEFT -> Rect(0, 0, halfW, h)
            SnapZone.RIGHT -> Rect(halfW, 0, rightW, h)
            SnapZone.TOP -> Rect(0, 0, w, halfH)
            SnapZone.BOTTOM -> Rect(0, halfH, w, bottomH)
            SnapZone.TOP_LEFT -> Rect(0, 0, halfW, halfH)
            SnapZone.TOP_RIGHT -> Rect(halfW, 0, rightW, halfH)
            SnapZone.BOTTOM_LEFT -> Rect(0, halfH, halfW, bottomH)
            SnapZone.BOTTOM_RIGHT -> Rect(halfW, halfH, rightW, bottomH)
            SnapZone.CENTER -> {
                val cw = (w * CENTER_FRACTION).toInt()
                val ch = (h * CENTER_FRACTION).toInt()
                Rect((w - cw) / 2, (h - ch) / 2, cw, ch)
            }
        }
    }

    /**
     * Map a natural-language / AI snap instruction to a zone, or null if unrecognized (the AI
     * taskbar lets the user say "snap left", "top right", "maximize", "center").
     */
    fun zoneFor(instruction: String): SnapZone? {
        val s = instruction.lowercase()
        return when {
            "maxim" in s || "full" in s -> SnapZone.MAXIMIZE
            "center" in s || "centre" in s -> SnapZone.CENTER
            "top" in s && "left" in s -> SnapZone.TOP_LEFT
            "top" in s && "right" in s -> SnapZone.TOP_RIGHT
            "bottom" in s && "left" in s -> SnapZone.BOTTOM_LEFT
            "bottom" in s && "right" in s -> SnapZone.BOTTOM_RIGHT
            "left" in s -> SnapZone.LEFT
            "right" in s -> SnapZone.RIGHT
            "top" in s -> SnapZone.TOP
            "bottom" in s -> SnapZone.BOTTOM
            else -> null
        }
    }
}
