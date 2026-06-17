package com.kianirani.jarvis.data.launcher

/**
 * Pure grid geometry for the DragController (LR3). Mapping a pixel point inside
 * the workspace to a (col,row) cell is the one bit of drag math worth unit
 * testing, so it lives here — no Android, no Compose — and the UI just feeds it
 * the measured grid size.
 */
object LauncherGeometry {

    /**
     * The cell that contains pixel ([x],[y]) within a [width]×[height] grid of
     * [cols]×[rows] cells. Out-of-range points clamp to the nearest edge cell, so
     * a drag that ends slightly outside still resolves to a sensible target.
     */
    fun cellAt(x: Float, y: Float, width: Float, height: Float, cols: Int, rows: Int): Pair<Int, Int> {
        if (width <= 0f || height <= 0f || cols <= 0 || rows <= 0) return 0 to 0
        val col = (x / (width / cols)).toInt().coerceIn(0, cols - 1)
        val row = (y / (height / rows)).toInt().coerceIn(0, rows - 1)
        return col to row
    }
}
