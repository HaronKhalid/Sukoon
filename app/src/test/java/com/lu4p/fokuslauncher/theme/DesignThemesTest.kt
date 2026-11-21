package com.lu4p.fokuslauncher.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignThemesTest {
    private val designed = BuiltInThemes.all.filter { it.id != BuiltInThemes.ID_CUSTOM }

    @Test
    fun `primary text has at least 4_5 contrast on every background stop of every built-in theme`() {
        designed.forEach { t ->
            val worst = ThemeColorMath.minContrast(t.colors.primaryText, t.backgroundStops)
            assertTrue("${t.id} primary text contrast $worst", worst >= 4.5)
        }
    }

    @Test
    fun `secondary text and accent stay legible on the new themes`() {
        BuiltInThemes.all.drop(8).forEach { t ->
            val sec = ThemeColorMath.minContrast(t.colors.secondaryText, t.backgroundStops)
            val acc = ThemeColorMath.minContrast(t.colors.accent, t.backgroundStops)
            assertTrue("${t.id} secondary $sec", sec >= 4.0)
            assertTrue("${t.id} accent $acc", acc >= 3.0)
        }
    }

    @Test
    fun `there are at least 14 new themes and several use gradients`() {
        assertTrue(BuiltInThemes.all.size - 8 >= 14)
        val gradients = BuiltInThemes.all.mapNotNull { it.gradient }
        assertTrue(gradients.size >= 8)
        gradients.forEach { assertTrue(it.stops.size in 2..3) }
    }

    @Test
    fun `every built-in has a known clock face and a real layout preset name`() {
        val presets = setOf("DEFAULT", "PURE_MINIMAL", "AESTHETIC", "FOCUS", "INFORMATION")
        BuiltInThemes.all.forEach { t ->
            assertTrue(t.id, t.style.clockFaceId in ClockFaces.KNOWN)
            assertTrue(t.id, t.style.layoutPreset in presets)
        }
    }

    @Test
    fun `gradient and style round-trip through the codec`() {
        val t =
                BuiltInThemes.Aurora.copy(
                        gradient = ThemeGradient(listOf(0xFF0B1026.toInt(), 0xFF123B4A.toInt(), 0xFF2A1B4D.toInt()), 45),
                        style = ThemeStyle("word-clock", "FOCUS", 600, 0.05f),
                )
        val back = (ThemeCodec.decode(ThemeCodec.encode(t)) as ThemeParseResult.Success).theme
        assertEquals(t, back)
        assertEquals(45, back.gradient!!.angleDegrees)
    }

    @Test
    fun `old v1 theme json without gradient or style still decodes with defaults`() {
        val old =
                """{"schemaVersion":1,"id":"old-one","name":"Old","appearance":"dark",
                "colors":{"background":"#101010","primaryText":"#FFFFFF","secondaryText":"#AAAAAA","accent":"#88AAFF","surface":"#202020"},
                "typography":{"defaultFont":"system","clockFont":"inter","noteFont":"system"},
                "motion":{"animationsEnabled":true,"intensity":"subtle"}}"""
        val t = (ThemeCodec.decode(old) as ThemeParseResult.Success).theme
        assertNull(t.gradient)
        assertEquals(ThemeStyle(), t.style)
        assertEquals(ThemeDefinition.CURRENT_SCHEMA_VERSION, t.schemaVersion)
        assertEquals("inter", t.typography.clockFont)
    }

    @Test
    fun `v1 to v2 migration step keeps data and bumps the version`() {
        val out = ThemeCodec.migrateToCurrent(mapOf("schemaVersion" to 1L, "id" to "x"))!!
        assertEquals(2L, out["schemaVersion"])
        assertEquals("x", out["id"])
    }

    @Test
    fun `unknown clock face id falls back to the default digital clock`() {
        assertEquals("classic-digital", ThemeStyle(clockFaceId = "hologram-9000").resolvedClockFaceId)
        assertEquals("classic-digital", ClockFaces.resolve(null))
        assertEquals("word-clock", ThemeStyle(clockFaceId = "word-clock").resolvedClockFaceId)
        // A well-formed but unknown id survives import and resolves at use.
        val t = BuiltInThemes.Graphite.copy(id = "imp", style = ThemeStyle(clockFaceId = "future-face"))
        val back = (ThemeCodec.decode(ThemeCodec.encode(t)) as ThemeParseResult.Success).theme
        assertEquals("classic-digital", back.style.resolvedClockFaceId)
    }

    @Test
    fun `malformed gradient and style are rejected`() {
        val noStyle = ThemeCodec.encode(BuiltInThemes.Graphite).substringBefore(",\n  \"style\"") + "\n}"
        fun decodeWith(extra: String) =
                ThemeCodec.decode(noStyle.removeSuffix("\n}") + ",\n$extra\n}")
        assertTrue(decodeWith("\"gradient\":{\"stops\":[\"#000000\"],\"angle\":90}") is ParseFail)
        assertTrue(decodeWith("\"gradient\":{\"stops\":[\"#000000\",\"nope\"]}") is ParseFail)
        assertTrue(decodeWith("\"gradient\":{\"stops\":[\"#000000\",\"#111111\"],\"angle\":999}") is ParseFail)
        assertTrue(decodeWith("\"style\":{\"fontWeight\":50}") is ParseFail)
        assertTrue(decodeWith("\"style\":{\"clockFaceId\":\"../../etc\"}") is ParseFail)
    }

    @Test
    fun `gradient that makes text unreadable is rejected`() {
        val bad = BuiltInThemes.Graphite.copy(gradient = ThemeGradient(listOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt())))
        assertTrue(ThemeCodec.decode(ThemeCodec.encode(bad)) is ParseFail)
    }

    @Test
    fun `classic selection yields no applied theme and nothing selected yields the default`() {
        assertNull(ThemeState(BuiltInThemes.ID_CLASSIC, emptyList()).applied)
        assertEquals(BuiltInThemes.SukoonDefault, ThemeState(null, emptyList()).applied)
        assertEquals(BuiltInThemes.Cream, ThemeState("cream", emptyList()).applied)
    }
}

private typealias ParseFail = ThemeParseResult.Failure
