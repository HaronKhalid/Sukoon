package com.lu4p.fokuslauncher.clock.faces

import com.lu4p.fokuslauncher.home.layout.ClockStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockFacesTest {
    @Test
    fun registryHasAllFixedIds() {
        assertEquals(
                listOf("classic-digital", "stacked", "split-accent", "minimal-text", "analog-minimal",
                        "analog-ticks", "word-clock", "outline-bold"),
                ClockFaceRegistry.ids,
        )
    }

    @Test
    fun unknownIdsFallBackToClassicDigital() {
        assertEquals("classic-digital", ClockFaceRegistry.get("nope").id)
        assertEquals("classic-digital", ClockFaceRegistry.get(null).id)
        assertEquals("classic-digital", ClockFaceRegistry.get("").id)
        assertEquals("word-clock", ClockFaceRegistry.get("word-clock").id)
        assertEquals("classic-digital", ClockFaceRegistry.resolveId("garbage", ClockStyle.SPLIT))
        assertEquals("split-accent", ClockFaceRegistry.resolveId("", ClockStyle.SPLIT))
        assertEquals("analog-ticks", ClockFaceRegistry.resolveId("analog-ticks", ClockStyle.SPLIT))
    }

    @Test
    fun scaleIsClamped() {
        assertEquals(0.5f, ClockScale.clamp(0.1f))
        assertEquals(2f, ClockScale.clamp(10f))
        assertEquals(1f, ClockScale.clamp(Float.NaN))
        assertEquals(1.3f, ClockScale.clamp(1.3f))
    }

    @Test
    fun wordClockText() {
        assertEquals("twelve o'clock", WordClock.text(12, 0))
        assertEquals("twelve o'clock", WordClock.text(0, 0))
        assertEquals("half past twelve", WordClock.text(0, 30))
        assertEquals("twenty five to one", WordClock.text(12, 35))
        assertEquals("one minute to twelve", WordClock.text(23, 59))
        assertEquals("half past ten", WordClock.text(10, 30))
        assertEquals("quarter past nine", WordClock.text(9, 15))
        assertEquals("quarter to seven", WordClock.text(6, 45))
        assertEquals("seven minutes past seven", WordClock.text(19, 7))
        assertEquals("one minute past one", WordClock.text(13, 1))
        assertEquals("twenty past three", WordClock.text(15, 20))
        assertEquals("ten to two", WordClock.text(1, 50))
        assertEquals(listOf("half past", "ten"), WordClock.lines(22, 30))
        assertTrue((0 until 24).all { h -> (0 until 60).all { m -> WordClock.text(h, m).isNotBlank() } })
    }

    @Test
    fun analogAngles() {
        assertEquals(0f, AnalogMath.hourDegrees(12, 0))
        assertEquals(105f, AnalogMath.hourDegrees(3, 30))
        assertEquals(180f, AnalogMath.minuteDegrees(30))
        assertEquals(354f, AnalogMath.secondDegrees(59))
    }
}
