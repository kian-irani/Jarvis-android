package com.kianirani.jarvis.core.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * DS-WIN4 acceptance: halves/quarters tile the work area exactly (no gap on odd sizes),
 * maximize fills, center is fractional, and NL instructions map to zones. Pure.
 */
class WindowSnapLayoutTest {

    @Test fun `maximize fills the work area`() {
        assertEquals(Rect(0, 0, 1920, 1080), WindowSnapLayout.rectFor(SnapZone.MAXIMIZE, 1920, 1080))
    }

    @Test fun `left and right halves cover the full width with no gap on odd width`() {
        val left = WindowSnapLayout.rectFor(SnapZone.LEFT, 1921, 1080)
        val right = WindowSnapLayout.rectFor(SnapZone.RIGHT, 1921, 1080)
        assertEquals(0, left.x)
        assertEquals(left.width, right.x) // right starts where left ends
        assertEquals(1921, left.width + right.width) // exact coverage
    }

    @Test fun `quarters tile the area`() {
        val tl = WindowSnapLayout.rectFor(SnapZone.TOP_LEFT, 1000, 800)
        val br = WindowSnapLayout.rectFor(SnapZone.BOTTOM_RIGHT, 1000, 800)
        assertEquals(Rect(0, 0, 500, 400), tl)
        assertEquals(Rect(500, 400, 500, 400), br)
    }

    @Test fun `center is fractional and centered`() {
        val c = WindowSnapLayout.rectFor(SnapZone.CENTER, 1000, 1000)
        assertEquals(600, c.width)
        assertEquals(600, c.height)
        assertEquals(200, c.x)
        assertEquals(200, c.y)
    }

    @Test fun `zoneFor maps natural instructions`() {
        assertEquals(SnapZone.MAXIMIZE, WindowSnapLayout.zoneFor("maximize it"))
        assertEquals(SnapZone.TOP_RIGHT, WindowSnapLayout.zoneFor("snap to the top right"))
        assertEquals(SnapZone.LEFT, WindowSnapLayout.zoneFor("move left"))
        assertEquals(SnapZone.CENTER, WindowSnapLayout.zoneFor("center the window"))
        assertNull(WindowSnapLayout.zoneFor("do a barrel roll"))
    }

    @Test fun `zero work area is safe`() {
        assertEquals(Rect(0, 0, 0, 0), WindowSnapLayout.rectFor(SnapZone.MAXIMIZE, 0, 0))
    }
}
