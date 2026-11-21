package com.lu4p.fokuslauncher.lockscreen

import com.lu4p.fokuslauncher.notes.NoteAlignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockLayoutMathTest {
    private val m: (String) -> Float = { it.length * 10f } // 10 px per char

    @Test
    fun wrapsOnWordsAndKeepsParagraphs() {
        val r = LockLayoutMath.wrap("hello brave new world", 110f, 10, m)
        assertEquals(listOf("hello brave", "new world"), r.lines)
        assertFalse(r.truncated)
        assertEquals(listOf("one", "two"), LockLayoutMath.wrap("one\ntwo", 110f, 10, m).lines)
    }

    @Test
    fun breaksOverlongWordsByCodePoint() {
        val r = LockLayoutMath.wrap("abcdefghij", 40f, 10, m)
        assertEquals(listOf("abcd", "efgh", "ij"), r.lines)
        val emoji = "😀😀😀" // each emoji is 2 chars -> 20px
        val e = LockLayoutMath.wrap(emoji, 40f, 10, m)
        e.lines.forEach { l -> assertTrue(l.codePoints().allMatch { Character.isSupplementaryCodePoint(it) }) }
        assertEquals(emoji, e.lines.joinToString(""))
    }

    @Test
    fun maxLinesEllipsisesLastLineWithinWidth() {
        val r = LockLayoutMath.wrap("one two three four five six", 60f, 2, m)
        assertEquals(2, r.lines.size)
        assertTrue(r.truncated)
        assertTrue(r.lines.last().endsWith("…"))
        assertTrue(m(r.lines.last()) <= 60f)
    }

    @Test
    fun blankAndEdgeInputs() {
        assertEquals(emptyList<String>(), LockLayoutMath.wrap("   \n ", 100f, 5, m).lines)
        assertEquals(listOf("a", "", "b"), LockLayoutMath.wrap("a\n\nb", 100f, 5, m).lines)
        assertEquals(1, LockLayoutMath.wrap("abc def", 100f, 0, m).lines.size)
    }

    @Test
    fun urduAndMixedTextWrapWithoutLoss() {
        val urdu = "یہ ایک اردو نوٹ ہے with English"
        val r = LockLayoutMath.wrap(urdu, 120f, 10, m)
        assertEquals(urdu.split(" ").toSet(), r.lines.flatMap { it.split(" ") }.toSet())
    }

    @Test
    fun lineBoostForUrduAndNastaliq() {
        assertEquals(1f, LockLayoutMath.lineBoost("", "hello"), 0f)
        assertEquals(LockLayoutMath.URDU_BOOST, LockLayoutMath.lineBoost("", "اردو"), 0f)
        assertEquals(1.45f, LockLayoutMath.lineBoost("bundled:noto_nastaliq_urdu", "hello"), 0f)
        assertEquals(1.45f, LockLayoutMath.lineBoost("bundled:noto_nastaliq_urdu", "اردو"), 0f)
    }

    @Test
    fun directionAndPhysicalAlignment() {
        assertEquals(TextDir.LTR, LockLayoutMath.direction("hello 123"))
        assertEquals(TextDir.RTL, LockLayoutMath.direction("اردو"))
        assertEquals(TextDir.MIXED_RTL_FIRST, LockLayoutMath.direction("اردو hello"))
        assertEquals(TextDir.MIXED_LTR_FIRST, LockLayoutMath.direction("hello اردو"))
        assertEquals(TextDir.NEUTRAL, LockLayoutMath.direction("123 !"))
        assertEquals(-1, LockLayoutMath.physicalAlign(NoteAlignment.START, false))
        assertEquals(1, LockLayoutMath.physicalAlign(NoteAlignment.START, true))
        assertEquals(-1, LockLayoutMath.physicalAlign(NoteAlignment.END, true))
        assertEquals(0, LockLayoutMath.physicalAlign(NoteAlignment.CENTER, true))
    }

    @Test
    fun placementStaysOnCanvas() {
        val h = 2000
        assertEquals(240f, LockLayoutMath.blockTop(LockPosition.TOP, h, 300f), 0.01f)
        assertEquals(850f, LockLayoutMath.blockTop(LockPosition.CENTER, h, 300f), 0.01f)
        assertEquals(1340f, LockLayoutMath.blockTop(LockPosition.BOTTOM, h, 300f), 0.01f)
        assertEquals(0f, LockLayoutMath.blockTop(LockPosition.CENTER, h, 5000f), 0f)
    }

    @Test
    fun sizesScaleWithCanvasAndLinesFit() {
        assertEquals(36f, LockLayoutMath.textSizePx(18f, 720, 1f), 0.01f)
        assertEquals(72f, LockLayoutMath.textSizePx(18f, 720, 2f), 0.01f)
        assertEquals(72f, LockLayoutMath.textSizePx(18f, 720, 9f), 0.01f)
        assertEquals(3, LockLayoutMath.maxLinesThatFit(100f, 30f, 10))
        assertEquals(1, LockLayoutMath.maxLinesThatFit(5f, 30f, 10))
        assertEquals(2, LockLayoutMath.maxLinesThatFit(500f, 30f, 2))
        assertEquals(1.4f * 1.45f / 1.2f, LockLayoutMath.spacingMultiplier(1.4f, 1.45f), 0.001f)
        assertEquals(0xFF111111.toInt(), LockLayoutMath.defaultTextColor(LockLayoutMath.luma(0xFFFFFFFF.toInt())))
        assertEquals(0xFFFFFFFF.toInt(), LockLayoutMath.defaultTextColor(LockLayoutMath.luma(0xFF000000.toInt())))
    }
}

class NotificationTextTest {
    @Test
    fun collapsesBlankLinesAndTrims() {
        assertEquals("a\nb", NotificationText.body("  a \n\n\n b  "))
    }

    @Test
    fun capsLinesAndChars() {
        val many = (1..20).joinToString("\n") { "line$it" }
        assertEquals("line1\nline2\nline3…", NotificationText.body(many, maxChars = 1000, maxLines = 3))
        val c = NotificationText.body("x".repeat(700))
        assertEquals(NotificationText.MAX_CHARS + 1, c.length)
        assertTrue(c.endsWith("…"))
    }

    @Test
    fun neverSplitsSurrogatePair() {
        val s = "a" + "😀".repeat(10)
        val r = NotificationText.short(s, 2) // cut would fall after a high surrogate
        assertFalse(Character.isHighSurrogate(r[r.length - 2]))
        assertTrue(r.endsWith("…"))
        val b = NotificationText.body(s, maxChars = 2)
        assertFalse(Character.isHighSurrogate(b[b.length - 2]))
    }

    @Test
    fun shortFlattensWhitespace() {
        assertEquals("a b c", NotificationText.short("a\n b\t c"))
        assertEquals("", NotificationText.short("   "))
    }
}

class LockNoteConfigTest {
    @Test
    fun roundTrip() {
        val c =
                LockNoteConfig(
                        noteId = 42,
                        styleId = "neon",
                        fontFamily = "bundled:caveat|odd",
                        alignment = NoteAlignment.END,
                        position = LockPosition.BOTTOM,
                        background = LockBackgroundKind.PHOTO,
                        photoFile = "wp_1.jpg",
                        showClockLine = true,
                        scale = 1.5f,
                        autoUpdate = true,
                        notificationOn = true,
                        notificationOnLockScreen = true,
                )
        assertEquals(c, LockNoteConfig.decode(c.encode()))
    }

    @Test
    fun badInputGivesDefaultsAndScaleIsClamped() {
        assertEquals(LockNoteConfig.DEFAULT, LockNoteConfig.decode(null))
        assertEquals(LockNoteConfig.DEFAULT, LockNoteConfig.decode("garbage"))
        assertEquals(LockNoteConfig.DEFAULT, LockNoteConfig.decode("l9|1"))
        assertEquals(2f, LockNoteConfig.decode("l1|1|a|b|0|0|0|1|2|d|p|0|99|0|0|0").scale, 0f)
        val short = LockNoteConfig.decode("l1|5|neon")
        assertEquals(5L, short.noteId)
        assertFalse(short.notificationOn)
        assertFalse(short.notificationOnLockScreen)
    }
}
