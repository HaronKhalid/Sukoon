package com.lu4p.fokuslauncher.ui.editor

import com.lu4p.fokuslauncher.home.layout.ClockSettings
import com.lu4p.fokuslauncher.home.layout.HomeLayoutConfig
import com.lu4p.fokuslauncher.home.layout.LayoutPreset
import com.lu4p.fokuslauncher.theme.BuiltInThemes
import com.lu4p.fokuslauncher.theme.ThemeAppearance
import com.lu4p.fokuslauncher.theme.ThemeCodec
import com.lu4p.fokuslauncher.theme.ThemeColorMath
import com.lu4p.fokuslauncher.typography.TextElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorSessionTest {
    private fun start() =
            EditorSnapshot(BuiltInThemes.Aurora, HomeLayoutConfig.forPreset(LayoutPreset.AESTHETIC), ClockSettings(faceId = "stacked"))

    @Test
    fun `edit undo and noop edits`() {
        val s = EditorSession(start())
        assertFalse(s.canUndo)
        assertFalse(s.edit { it }) // no change records nothing
        assertFalse(s.canUndo)
        assertTrue(s.edit { EditorOps.setAccent(it, 0xFF112233.toInt()) })
        assertTrue(s.canUndo && s.isModified)
        assertEquals(0xFF112233.toInt(), s.current.theme.colors.accent)
        assertTrue(s.undo())
        assertEquals(start(), s.current)
        assertFalse(s.isModified)
        assertFalse(s.undo())
    }

    @Test
    fun `slider drag collapses into one undo step`() {
        val s = EditorSession(start())
        for (pct in listOf(110, 120, 130, 140)) s.edit("clock-size") { EditorOps.setClockScale(it, pct) }
        s.endGesture()
        assertEquals(140, s.current.clockScalePct)
        s.undo()
        assertEquals(start(), s.current) // one step undone the whole drag
        assertFalse(s.canUndo)
    }

    @Test
    fun `two separate drags are two undo steps`() {
        val s = EditorSession(start())
        s.edit("k") { EditorOps.setClockScale(it, 120) }
        s.endGesture()
        s.edit("k") { EditorOps.setClockScale(it, 150) }
        s.endGesture()
        s.undo()
        assertEquals(120, s.current.clockScalePct)
    }

    @Test
    fun `reset returns to the opening state and is undoable`() {
        val s = EditorSession(start())
        s.edit { EditorOps.setBackdrop(it, "starfield") }
        s.edit { EditorOps.setLayoutPreset(it, LayoutPreset.FOCUS) }
        val edited = s.current
        assertTrue(s.reset())
        assertEquals(start(), s.current)
        assertFalse(s.reset())
        assertTrue(s.undo())
        assertEquals(edited, s.current)
    }

    @Test
    fun `undo history is bounded`() {
        val s = EditorSession(start(), maxUndo = 3)
        for (i in 1..10) s.edit { EditorOps.setClockScale(it, 100 + i) }
        var steps = 0
        while (s.undo()) steps++
        assertEquals(3, steps)
    }

    @Test
    fun `selecting a theme applies its layout preset and clock face`() {
        val n = EditorOps.selectTheme(start(), BuiltInThemes.CityLights)
        assertEquals(LayoutPreset.FOCUS, n.layout.preset)
        assertEquals("outline-bold", n.clock.faceId)
        assertEquals("city-glow", n.theme.style.backdrop)
    }

    @Test
    fun `backdrop of the other brightness adopts readable colours and stays valid`() {
        val light = EditorOps.selectTheme(start(), BuiltInThemes.Paper)
        val n = EditorOps.setBackdrop(light, "starfield")
        assertEquals(ThemeAppearance.DARK, n.theme.appearance)
        assertEquals("starfield", n.theme.style.backdrop)
        assertTrue(ThemeCodec.isValid(n.effectiveTheme))
        // accent is the user's, not the pair's
        assertEquals(BuiltInThemes.Paper.colors.accent, n.theme.colors.accent)
    }

    @Test
    fun `unreadable text colour is rejected`() {
        val s = start()
        assertNull(EditorOps.setTextColor(s, 0xFF0B1026.toInt())) // same as background
        val ok = EditorOps.setTextColor(s, 0xFFFFFFFF.toInt())
        assertNotNull(ok)
        assertTrue(ThemeCodec.isValid(ok!!.effectiveTheme))
    }

    @Test
    fun `tint is non destructive and bounded`() {
        val s = start()
        val tinted = EditorOps.setTint(s, 0xFF204080.toInt(), 30)!!
        assertTrue(tinted.effectiveTheme.backgroundStops != s.theme.backgroundStops)
        assertEquals(s.theme.backgroundStops, tinted.theme.backgroundStops) // base untouched
        val cleared = EditorOps.setTint(tinted, 0xFF204080.toInt(), 0)!!
        assertEquals(s.theme.backgroundStops, cleared.effectiveTheme.backgroundStops)
        assertEquals(EditorOps.MAX_TINT_PCT, EditorOps.setTint(s, 0xFF000000.toInt(), 99)!!.tintPct)
        // A very light tint on a dark theme with dark-ish text would fail contrast and be refused.
        val darkText = EditorOps.setTextColor(s, 0xFFCCCCCC.toInt())!!
        assertNull(EditorOps.setTint(darkText, 0xFFFFFFFF.toInt(), 60))
    }

    @Test
    fun `element fonts set and clear`() {
        var s = EditorOps.setElementFont(start(), TextElement.CLOCK, "bundled:inter")
        assertEquals("bundled:inter", s.elementFonts[TextElement.CLOCK])
        s = EditorOps.setElementFont(s, TextElement.CLOCK, "")
        assertTrue(s.elementFonts.isEmpty())
    }

    @Test
    fun `clock scale is clamped and flips the preset to custom`() {
        val n = EditorOps.setClockScale(start(), 999)
        assertEquals(200, n.clockScalePct)
        assertEquals(LayoutPreset.CUSTOM, n.layout.preset)
        assertTrue(ThemeColorMath.luminance(n.theme.colors.primaryText) > 0.5)
    }
}
