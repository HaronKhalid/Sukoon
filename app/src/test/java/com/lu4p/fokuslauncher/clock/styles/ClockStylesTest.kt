package com.lu4p.fokuslauncher.clock.styles

import com.lu4p.fokuslauncher.clock.faces.ClockFaceRegistry
import com.lu4p.fokuslauncher.home.layout.ClockSettings
import com.lu4p.fokuslauncher.home.layout.ClockSettingsCodec
import com.lu4p.fokuslauncher.typography.FontRegistry
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockStylesTest {
    @Test
    fun registryHasFortyPlusUniqueValidIds() {
        val ids = ClockStyleRegistry.ids
        assertTrue("only ${ids.size}", ids.size >= 40)
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertTrue(it, Regex("[a-z0-9-]{1,32}").matches(it)) }
    }

    @Test
    fun presetsReferenceRealFontsAndFaces() {
        ClockStyleRegistry.presets.forEach {
            if (it.fontId.isNotEmpty()) assertNotNull("${it.id} font ${it.fontId}", FontRegistry.findBundled(it.fontId))
            assertTrue("${it.id} face ${it.faceId}", ClockFaceRegistry.isKnown(it.faceId))
            assertTrue(it.weight in 100..900)
        }
    }

    @Test
    fun presetsAreVisuallyDistinct() {
        val looks = ClockStyleRegistry.presets.map { it.copy(id = "", name = "") }
        assertEquals(looks.size, looks.toSet().size)
    }

    @Test
    fun idRoundTripsThroughSettingsCodec() {
        ClockStyleRegistry.ids.forEach { id ->
            val decoded = ClockSettingsCodec.decode(ClockSettingsCodec.encode(ClockSettings(styleId = id)))
            assertEquals(id, decoded?.styleId)
            assertEquals(id, ClockStyleRegistry.find(id)?.id)
        }
    }

    @Test
    fun oldCodecVersionsStillDecode() {
        val c2 = "c2|LARGE_DIGITAL|SYSTEM|0|0|SEMI_BOLD|0.0|1.0|0|START|1|SYSTEM|stacked|0"
        val d2 = ClockSettingsCodec.decode(c2)
        assertNotNull(d2)
        assertEquals("stacked", d2!!.faceId)
        assertEquals("", d2.styleId)
        val c1 = "c1|LARGE_DIGITAL|SYSTEM|0|0|SEMI_BOLD|0.0|1.0|0|START|1|SYSTEM"
        assertEquals("", ClockSettingsCodec.decode(c1)!!.styleId)
        assertTrue(ClockSettingsCodec.encode(ClockSettings()).startsWith("c3|"))
        assertEquals(ClockSettings(), ClockSettingsCodec.decode(ClockSettingsCodec.encode(ClockSettings())))
    }

    @Test
    fun badStyleIdOrArityIsRejected() {
        assertNull(ClockSettingsCodec.decode("c3|LARGE_DIGITAL|SYSTEM|0|0|SEMI_BOLD|0.0|1.0|0|START|1|SYSTEM||0|BAD ID"))
        assertNull(ClockSettingsCodec.decode("c3|LARGE_DIGITAL|SYSTEM|0|0|SEMI_BOLD|0.0|1.0|0|START|1|SYSTEM||0"))
    }

    @Test
    fun unknownIdsFallBack() {
        assertNull(ClockStyleRegistry.find("does-not-exist"))
        assertNull(ClockStyleRegistry.find(""))
        assertFalse(ClockStyleRegistry.isKnown(null))
        val bad = ClockStylePreset("x", "X", faceId = "nope")
        assertEquals(ClockFaceRegistry.DEFAULT_ID, ClockStyleRegistry.faceIdFor(bad))
    }

    @Test
    fun weekdayAndCaseText() {
        val t = ZonedDateTime.of(2026, 11, 19, 9, 5, 0, 0, ZoneId.of("UTC")) // Thursday
        assertEquals("Thu.", ClockStyleText.line(ClockLineVariant.WEEKDAY_DOT, t, Locale.ENGLISH, ClockTextCase.AS_IS))
        assertEquals("THURSDAY", ClockStyleText.line(ClockLineVariant.WEEKDAY, t, Locale.ENGLISH, ClockTextCase.UPPER))
        assertEquals("19 nov", ClockStyleText.line(ClockLineVariant.DATE, t, Locale.ENGLISH, ClockTextCase.LOWER))
        assertNull(ClockStyleText.line(ClockLineVariant.NONE, t, Locale.ENGLISH, ClockTextCase.AS_IS))
        assertEquals(listOf("0", "9", "0", "5"), ClockStyleText.verticalRows("09", "05"))
    }
}
