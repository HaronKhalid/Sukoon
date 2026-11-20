package com.lu4p.fokuslauncher.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BidiTextTest {
    private val urdu = "یہ ایک نوٹ ہے"
    private val mixed = "Meeting کل صبح 10:30 بجے، room B-12!"

    @Test
    fun pureUrduIsRtlAndNotMixed() {
        assertEquals(true, BidiText.firstStrongIsRtl(urdu))
        assertFalse(BidiText.isMixedDirection(urdu))
        assertTrue(BidiText.containsArabicScript(urdu))
    }

    @Test
    fun pureEnglishIsLtr() {
        assertEquals(false, BidiText.firstStrongIsRtl("Buy milk"))
        assertFalse(BidiText.containsArabicScript("Buy milk"))
    }

    @Test
    fun leadingNeutralsAreSkippedWhenFindingDirection() {
        assertEquals(true, BidiText.firstStrongIsRtl("123 - ... $urdu"))
        assertEquals(false, BidiText.firstStrongIsRtl("« 42 » hello"))
        assertNull(BidiText.firstStrongIsRtl("123 - !?"))
        assertNull(BidiText.firstStrongIsRtl(""))
    }

    @Test
    fun mixedUrduEnglishIsDetectedAndFollowsFirstStrong() {
        assertTrue(BidiText.isMixedDirection(mixed))
        assertEquals(false, BidiText.firstStrongIsRtl(mixed))
        val urduFirst = "نوٹ: remember to call"
        assertTrue(BidiText.isMixedDirection(urduFirst))
        assertEquals(true, BidiText.firstStrongIsRtl(urduFirst))
    }

    @Test
    fun urduPunctuationAndDigitsDoNotBreakDetection() {
        // Arabic question mark (U+061F), comma (U+060C), Urdu full stop (U+06D4), extended Arabic-Indic digits.
        val text = "کیا آپ ٹھیک ہیں؟ ہاں، شکریہ۔ ۱۲۳"
        assertEquals(true, BidiText.firstStrongIsRtl(text))
        assertFalse(BidiText.isMixedDirection(text))
    }

    @Test
    fun surrogatePairsAreHandled() {
        assertEquals(true, BidiText.firstStrongIsRtl("😀 $urdu"))
    }

    @Test
    fun zeroWidthNonJoinerInsideUrduWordKeepsText() {
        val withZwnj = "ہوائی‌جہاز"
        assertTrue(BidiText.containsArabicScript(withZwnj))
        assertEquals(true, BidiText.firstStrongIsRtl(withZwnj))
        assertEquals(withZwnj.length, withZwnj.codePointCount(0, withZwnj.length))
    }
}
