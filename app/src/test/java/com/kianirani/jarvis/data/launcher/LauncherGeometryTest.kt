package com.kianirani.jarvis.data.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherGeometryTest {

    // A 500×500 grid of 5×5 → each cell is 100px.
    @Test fun `top-left point maps to cell 0,0`() {
        assertEquals(0 to 0, LauncherGeometry.cellAt(10f, 10f, 500f, 500f, 5, 5))
    }

    @Test fun `centre point maps to the centre cell`() {
        assertEquals(2 to 2, LauncherGeometry.cellAt(250f, 250f, 500f, 500f, 5, 5))
    }

    @Test fun `bottom-right point maps to the last cell`() {
        assertEquals(4 to 4, LauncherGeometry.cellAt(499f, 499f, 500f, 500f, 5, 5))
    }

    @Test fun `out-of-range points clamp to edge cells`() {
        assertEquals(0 to 0, LauncherGeometry.cellAt(-50f, -50f, 500f, 500f, 5, 5))
        assertEquals(4 to 4, LauncherGeometry.cellAt(9999f, 9999f, 500f, 500f, 5, 5))
    }

    @Test fun `column and row are independent`() {
        assertEquals(3 to 1, LauncherGeometry.cellAt(350f, 150f, 500f, 500f, 5, 5))
    }

    @Test fun `zero size is safe`() {
        assertEquals(0 to 0, LauncherGeometry.cellAt(10f, 10f, 0f, 0f, 5, 5))
    }
}
