package com.lu4p.fokuslauncher.home.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutSpacingTest {
    @Test
    fun normalSpacingKeepsOriginalPadding() {
        assertEquals(8, layoutPadDp(8, ElementSpacing.NORMAL, hasAbove = true))
        assertEquals(16, layoutPadDp(16, ElementSpacing.NORMAL, hasAbove = true))
    }

    @Test
    fun nothingAboveMeansNoPadding() {
        assertEquals(0, layoutPadDp(16, ElementSpacing.RELAXED, hasAbove = false))
    }

    @Test
    fun spacingShiftsPaddingRelativeToNormal() {
        assertEquals(24, layoutPadDp(16, ElementSpacing.RELAXED, hasAbove = true))
        assertEquals(12, layoutPadDp(16, ElementSpacing.COMPACT, hasAbove = true))
    }

    @Test
    fun paddingNeverGoesNegative() {
        assertEquals(0, layoutPadDp(4, ElementSpacing.NONE, hasAbove = true))
    }

    @Test
    fun movingAnElementChangesRenderOrder() {
        val moved = HomeLayoutConfig.Default.move(HomeElementType.NOTES, -10)
        assertEquals(HomeElementType.NOTES, moved.ordered().first().type)
        assertEquals(LayoutPreset.CUSTOM, moved.preset)
    }
}
