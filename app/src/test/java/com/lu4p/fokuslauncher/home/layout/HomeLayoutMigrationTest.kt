package com.lu4p.fokuslauncher.home.layout

import com.lu4p.fokuslauncher.home.layout.HomeElementType.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutMigrationTest {
    private val v1 = "v1|CUSTOM|CLOCK:1:1:CENTER:LARGE:RELAXED|DATE:1:0:END:SMALL:COMPACT|BATTERY:0:2:START:MEDIUM:NORMAL" +
            "|NOTES:1:3:START:MEDIUM:NORMAL|FAVOURITES:1:4:START:MEDIUM:NORMAL|APP_SEARCH:0:5:START:MEDIUM:NORMAL" +
            "|FOCUS_STATUS:0:6:START:MEDIUM:NORMAL|SPACER:0:7:START:MEDIUM:NORMAL"

    @Test
    fun v1DataDecodesAndMigratesSizesAndKeepsOrder() {
        val c = HomeLayoutCodec.decode(v1)!!
        assertEquals(HomeLayoutConfig.CURRENT_VERSION, c.version)
        assertEquals(120, c.element(CLOCK).scalePct)
        assertEquals(85, c.element(DATE).scalePct)
        assertEquals(100, c.element(NOTES).scalePct)
        assertEquals(ElementAlignment.CENTER, c.element(CLOCK).alignment)
        assertEquals(listOf(DATE, CLOCK, BATTERY), c.ordered().map { it.type }.take(3))
        assertFalse(c.isVisible(BATTERY))
        assertEquals(0, c.element(CLOCK).style)
    }

    @Test
    fun newWidgetsAreHiddenAfterMigration() {
        val c = HomeLayoutCodec.decode(v1)!!
        HomeElementType.entries.filter { it.optionalWidget }.forEach { assertFalse(it.name, c.isVisible(it)) }
        assertEquals(HomeElementType.entries.size, c.elements.size)
        assertEquals(SPACER, c.ordered()[7].type)
    }

    @Test
    fun ordinalsOfExistingTypesAreUnchanged() {
        assertEquals(listOf(CLOCK, DATE, BATTERY, NOTES, FAVOURITES, APP_SEARCH, FOCUS_STATUS, SPACER),
                HomeElementType.entries.take(8))
    }

    @Test
    fun v2RoundTripsStyleAndScale() {
        val c = HomeLayoutConfig.Default
                .update(MONTH_CALENDAR) { it.copy(visible = true, scalePct = 140, style = 1) }
        val back = HomeLayoutCodec.decode(HomeLayoutCodec.encode(c))
        assertEquals(c, back)
        assertTrue(HomeLayoutCodec.encode(c).startsWith("v3|"))
    }

    @Test
    fun scaleIsClampedEverywhere() {
        assertEquals(50, ElementConfig.clampPercent(-10))
        assertEquals(200, ElementConfig.clampPercent(9999))
        assertEquals(0.5f, ElementConfig(CLOCK, scalePct = 1).scale)
        assertEquals(2f, ElementConfig(CLOCK, scalePct = 500).scale)
        val c = HomeLayoutConfig(elements = listOf(ElementConfig(CLOCK, scalePct = 900, style = 99),
                ElementConfig(DATE_CARD, scalePct = 3, style = 99))).normalized()
        assertEquals(200, c.element(CLOCK).scalePct)
        assertEquals(0, c.element(CLOCK).style)
        assertEquals(50, c.element(DATE_CARD).scalePct)
        assertEquals(2, c.element(DATE_CARD).style)
        val decoded = HomeLayoutCodec.decode("v2|CUSTOM|CLOCK:1:0:START:1000:NORMAL:0")!!
        assertEquals(200, decoded.element(CLOCK).scalePct)
    }

    @Test
    fun invalidV2IsRejected() {
        assertNull(HomeLayoutCodec.decode("v2|CUSTOM|CLOCK:1:0:START:100:NORMAL"))
        assertNull(HomeLayoutCodec.decode("v2|CUSTOM|NOPE:1:0:START:100:NORMAL:0"))
        assertNull(HomeLayoutCodec.decode("v4|CUSTOM|CLOCK:1:0:START:100:NORMAL:0"))
    }

    @Test
    fun presetsShowcaseFacesAndWidgets() {
        assertEquals("analog-minimal", HomeLayoutConfig.presetClockFace(LayoutPreset.AESTHETIC))
        assertEquals("split-accent", HomeLayoutConfig.presetClockFace(LayoutPreset.FOCUS))
        assertEquals("minimal-text", HomeLayoutConfig.presetClockFace(LayoutPreset.PURE_MINIMAL))
        assertEquals("classic-digital", HomeLayoutConfig.presetClockFace(LayoutPreset.INFORMATION))
        assertEquals("", HomeLayoutConfig.presetClockFace(LayoutPreset.DEFAULT))
    }

    @Test
    fun clockSettingsC1StillDecodesAndC2RoundTrips() {
        val old = "c1|STACKED|H24|0|0|SEMI_BOLD|0.0|1.0|0|START|1|SYSTEM"
        val s = ClockSettingsCodec.decode(old)!!
        assertEquals(ClockStyle.STACKED, s.style)
        assertEquals("", s.faceId)
        val n = s.copy(faceId = "word-clock", accentArgb = 0xFF8FC8FF.toInt())
        assertEquals(n, ClockSettingsCodec.decode(ClockSettingsCodec.encode(n)))
        assertNull(ClockSettingsCodec.decode("c2|STACKED|H24|0|0|SEMI_BOLD|0.0|1.0|0|START|1|SYSTEM|BAD ID|0"))
    }
}
